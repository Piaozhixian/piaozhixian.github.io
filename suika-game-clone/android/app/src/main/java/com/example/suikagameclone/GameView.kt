package com.example.suikagameclone

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.Choreographer
import android.view.MotionEvent
import android.view.View

/** Custom-view based game surface. Draws the container, fruits and HUD every frame
 *  and drives its own physics loop via [Choreographer] instead of a game engine. */
class GameView(context: Context) : View(context) {

    private val density = resources.displayMetrics.density
    private fun dp(value: Float) = value * density

    private lateinit var engine: PhysicsEngine
    private var containerRect = RectF()
    private var dangerLineY = 0f
    private var previewY = 0f
    private var initialized = false
    private var running = false

    private var nextTier = FruitTable.randomSpawnTier()
    private var aimX = 0f
    private var canDrop = true
    private var isGameOver = false
    private var score = 0
    private var dangerTimer = 0f
    private var dropCooldownRemaining = 0f
    private var lastFrameTimeNanos = 0L

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.argb(90, 0, 0, 0)
        strokeWidth = dp(1.5f)
    }
    private val fruitTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = dp(20f)
        isFakeBoldText = true
    }
    private val dangerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(140, 220, 30, 30)
        strokeWidth = dp(2f)
    }
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 0, 0, 0)
    }
    private val overlayTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = dp(30f)
        isFakeBoldText = true
    }
    private val overlaySubtitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        textSize = dp(16f)
    }

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            onFrame(frameTimeNanos)
            if (running) Choreographer.getInstance().postFrameCallback(this)
        }
    }

    init {
        setBackgroundColor(Color.parseColor("#F5EDD8"))
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        running = true
        lastFrameTimeNanos = 0L
        Choreographer.getInstance().postFrameCallback(frameCallback)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        running = false
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val margin = dp(20f)
        val topReserved = dp(130f)
        val bottomReserved = dp(50f)
        containerRect = RectF(margin, topReserved, w - margin, h - bottomReserved)
        dangerLineY = containerRect.top + dp(45f)
        previewY = containerRect.top - dp(30f)
        engine = PhysicsEngine(containerRect.left, containerRect.right, containerRect.bottom, dp(1600f))
        aimX = containerRect.centerX()
        initialized = true
    }

    private fun onFrame(frameTimeNanos: Long) {
        if (!initialized) return
        val dt = if (lastFrameTimeNanos == 0L) {
            0f
        } else {
            ((frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f).coerceAtMost(0.05f)
        }
        lastFrameTimeNanos = frameTimeNanos

        if (!isGameOver) {
            if (dropCooldownRemaining > 0f) {
                dropCooldownRemaining -= dt
                if (dropCooldownRemaining <= 0f) canDrop = true
            }

            val merges = engine.step(dt)
            for ((a, b) in merges) {
                if (a.tier >= FruitTable.maxTier) {
                    score += 1000
                    continue
                }
                val newTier = a.tier + 1
                val kind = FruitTable.kind(newTier) ?: continue
                val mx = (a.x + b.x) / 2f
                val my = (a.y + b.y) / 2f
                engine.add(ActiveFruit(newTier, mx, my, dp(kind.radius)))
                score += newTier * 10
            }

            var overLine = false
            for (f in engine.fruits) {
                if (engine.isSettled(f) && f.y - f.radius < dangerLineY) {
                    overLine = true
                    break
                }
            }
            dangerTimer = if (overLine) dangerTimer + dt else 0f
            if (dangerTimer > 1.5f) isGameOver = true
        }

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!initialized) return

        canvas.drawRect(containerRect, strokePaint)
        canvas.drawLine(containerRect.left, dangerLineY, containerRect.right, dangerLineY, dangerPaint)

        for (f in engine.fruits) {
            drawFruit(canvas, f.tier, f.x, f.y, f.radius)
        }

        FruitTable.kind(nextTier)?.let { kind ->
            drawFruit(canvas, nextTier, aimX, previewY, dp(kind.radius))
        }

        canvas.drawText("Score: $score", containerRect.left, dp(30f), hudPaint)

        if (isGameOver) {
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)
            canvas.drawText("Game Over", width / 2f, height / 2f - dp(10f), overlayTitlePaint)
            canvas.drawText("Score: $score — Tap to restart", width / 2f, height / 2f + dp(24f), overlaySubtitlePaint)
        }
    }

    private fun drawFruit(canvas: Canvas, tier: Int, x: Float, y: Float, radiusPx: Float) {
        val kind = FruitTable.kind(tier) ?: return
        fillPaint.color = Color.rgb(kind.r, kind.g, kind.b)
        canvas.drawCircle(x, y, radiusPx, fillPaint)
        canvas.drawCircle(x, y, radiusPx, strokePaint)
        fruitTextPaint.textSize = radiusPx * 1.3f
        val metrics = fruitTextPaint.fontMetrics
        canvas.drawText(kind.emoji, x, y - (metrics.ascent + metrics.descent) / 2f, fruitTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isGameOver) {
            if (event.action == MotionEvent.ACTION_DOWN) restart()
            return true
        }
        if (!initialized) return true

        val kind = FruitTable.kind(nextTier) ?: return true
        val radiusPx = dp(kind.radius)
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                aimX = event.x.coerceIn(containerRect.left + radiusPx, containerRect.right - radiusPx)
                invalidate()
            }
            MotionEvent.ACTION_UP -> {
                if (canDrop) {
                    engine.add(ActiveFruit(nextTier, aimX, previewY, radiusPx))
                    nextTier = FruitTable.randomSpawnTier()
                    canDrop = false
                    dropCooldownRemaining = 0.5f
                }
            }
        }
        return true
    }

    private fun restart() {
        engine.fruits.clear()
        score = 0
        dangerTimer = 0f
        isGameOver = false
        canDrop = true
        dropCooldownRemaining = 0f
        nextTier = FruitTable.randomSpawnTier()
    }
}
