package com.example.suikagameclone

import kotlin.math.abs
import kotlin.math.sqrt

/** A live fruit instance in the simulation. All units are pixels. */
class ActiveFruit(
    var tier: Int,
    var x: Float,
    var y: Float,
    val radius: Float,
    var vx: Float = 0f,
    var vy: Float = 0f
)

/**
 * A minimal hand-rolled 2D physics simulation for circle-shaped fruits: gravity,
 * wall/floor collision and pairwise circle-overlap resolution. No external physics
 * library is used so the Android build has no extra dependencies to resolve.
 */
class PhysicsEngine(
    private val leftWall: Float,
    private val rightWall: Float,
    private val floorY: Float,
    private val gravity: Float
) {
    val fruits = mutableListOf<ActiveFruit>()

    private val restitution = 0.15f
    private val wallFriction = 0.65f
    private val settleThreshold = 6f

    fun add(fruit: ActiveFruit) {
        fruits.add(fruit)
    }

    /** Advances the simulation by [dt] seconds and returns fruit pairs that should merge. */
    fun step(dt: Float): List<Pair<ActiveFruit, ActiveFruit>> {
        for (f in fruits) {
            f.vy += gravity * dt
            f.x += f.vx * dt
            f.y += f.vy * dt

            if (f.x - f.radius < leftWall) {
                f.x = leftWall + f.radius
                f.vx = -f.vx * restitution
            } else if (f.x + f.radius > rightWall) {
                f.x = rightWall - f.radius
                f.vx = -f.vx * restitution
            }
            if (f.y + f.radius > floorY) {
                f.y = floorY - f.radius
                f.vy = -f.vy * restitution
                f.vx *= wallFriction
            }
        }

        val merges = mutableListOf<Pair<ActiveFruit, ActiveFruit>>()
        val merged = HashSet<ActiveFruit>()

        for (i in fruits.indices) {
            val a = fruits[i]
            if (a in merged) continue
            for (j in i + 1 until fruits.size) {
                val b = fruits[j]
                if (b in merged) continue

                val dx = b.x - a.x
                val dy = b.y - a.y
                val dist = sqrt(dx * dx + dy * dy)
                val minDist = a.radius + b.radius
                if (dist >= minDist) continue

                if (a.tier == b.tier) {
                    merges.add(a to b)
                    merged.add(a)
                    merged.add(b)
                } else {
                    // Same-tier pairs are about to be removed; only push apart otherwise.
                    val overlap = minDist - dist
                    val nx = if (dist > 0.0001f) dx / dist else 1f
                    val ny = if (dist > 0.0001f) dy / dist else 0f
                    a.x -= nx * overlap / 2f
                    a.y -= ny * overlap / 2f
                    b.x += nx * overlap / 2f
                    b.y += ny * overlap / 2f

                    val avx = (a.vx + b.vx) / 2f
                    val avy = (a.vy + b.vy) / 2f
                    a.vx = avx * 0.9f
                    a.vy = avy * 0.9f
                    b.vx = avx * 0.9f
                    b.vy = avy * 0.9f
                }
            }
        }

        if (merges.isNotEmpty()) {
            fruits.removeAll(merged)
        }
        return merges
    }

    fun isSettled(fruit: ActiveFruit): Boolean = abs(fruit.vx) + abs(fruit.vy) < settleThreshold
}
