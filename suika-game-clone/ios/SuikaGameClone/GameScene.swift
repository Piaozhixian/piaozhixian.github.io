import SpriteKit

final class GameScene: SKScene, SKPhysicsContactDelegate {

    // MARK: - Categories

    private let fruitCategory: UInt32 = 0x1 << 0
    private let wallCategory: UInt32 = 0x1 << 1

    // MARK: - Layout

    private var containerRect: CGRect = .zero
    private var dangerLineY: CGFloat = 0
    private var previewY: CGFloat = 0

    // MARK: - State

    private var scoreLabel: SKLabelNode!
    private var nextFruitPreview: SKShapeNode?
    private var currentAimTier = FruitTable.randomSpawnTier()
    private var canDrop = true
    private var isGameOver = false
    private var dangerTimer: TimeInterval = 0
    private var score = 0 {
        didSet { scoreLabel?.text = "Score: \(score)" }
    }
    private var lastUpdateTime: TimeInterval = 0
    private var pendingMerges: Set<ObjectIdentifier> = []

    // MARK: - Setup

    override func didMove(to view: SKView) {
        backgroundColor = SKColor(red: 0.96, green: 0.93, blue: 0.85, alpha: 1)
        physicsWorld.gravity = CGVector(dx: 0, dy: -9.8)
        physicsWorld.contactDelegate = self

        let margin: CGFloat = 20
        containerRect = CGRect(
            x: margin,
            y: 60,
            width: size.width - margin * 2,
            height: size.height - 60 - 110
        )
        dangerLineY = containerRect.maxY - 50
        previewY = containerRect.maxY + 30

        buildContainerWalls()
        buildDangerLine()
        buildHUD()
        spawnPreviewFruit()
    }

    private func buildContainerWalls() {
        let path = CGMutablePath()
        path.move(to: CGPoint(x: containerRect.minX, y: containerRect.maxY))
        path.addLine(to: CGPoint(x: containerRect.minX, y: containerRect.minY))
        path.addLine(to: CGPoint(x: containerRect.maxX, y: containerRect.minY))
        path.addLine(to: CGPoint(x: containerRect.maxX, y: containerRect.maxY))

        let walls = SKNode()
        walls.physicsBody = SKPhysicsBody(edgeChainFrom: path)
        walls.physicsBody?.categoryBitMask = wallCategory
        walls.physicsBody?.friction = 0.4
        addChild(walls)

        let outline = SKShapeNode(path: path)
        outline.strokeColor = SKColor(white: 0.3, alpha: 0.6)
        outline.lineWidth = 3
        addChild(outline)
    }

    private func buildDangerLine() {
        let line = SKShapeNode(rectOf: CGSize(width: containerRect.width, height: 2))
        line.position = CGPoint(x: containerRect.midX, y: dangerLineY)
        line.strokeColor = .red
        line.fillColor = .red
        line.alpha = 0.5
        line.zPosition = 5
        addChild(line)
    }

    private func buildHUD() {
        scoreLabel = SKLabelNode(fontNamed: "AvenirNext-Bold")
        scoreLabel.text = "Score: 0"
        scoreLabel.fontSize = 22
        scoreLabel.fontColor = .black
        scoreLabel.horizontalAlignmentMode = .left
        scoreLabel.position = CGPoint(x: containerRect.minX, y: size.height - 40)
        addChild(scoreLabel)
    }

    // MARK: - Fruit spawning

    private func spawnPreviewFruit() {
        currentAimTier = FruitTable.randomSpawnTier()
        guard let kind = FruitTable.kind(forTier: currentAimTier) else { return }

        let node = makeFruitNode(kind: kind)
        node.position = CGPoint(x: containerRect.midX, y: previewY)
        node.name = "preview"
        addChild(node)
        nextFruitPreview = node
    }

    private func makeFruitNode(kind: FruitKind) -> SKShapeNode {
        let node = SKShapeNode(circleOfRadius: kind.radius)
        node.fillColor = SKColor(red: kind.color.r, green: kind.color.g, blue: kind.color.b, alpha: 1)
        node.strokeColor = SKColor(white: 0, alpha: 0.25)
        node.lineWidth = 1.5
        node.userData = ["tier": kind.tier]

        let label = SKLabelNode(text: kind.emoji)
        label.fontSize = kind.radius * 1.3
        label.verticalAlignmentMode = .center
        label.horizontalAlignmentMode = .center
        node.addChild(label)
        return node
    }

    private func dropCurrentFruit(atX x: CGFloat) {
        guard canDrop, !isGameOver, let preview = nextFruitPreview, let kind = FruitTable.kind(forTier: currentAimTier) else { return }

        let clampedX = min(max(x, containerRect.minX + kind.radius), containerRect.maxX - kind.radius)
        preview.position = CGPoint(x: clampedX, y: previewY)
        preview.name = "fruit"

        let body = SKPhysicsBody(circleOfRadius: kind.radius)
        body.categoryBitMask = fruitCategory
        body.contactTestBitMask = fruitCategory
        body.collisionBitMask = fruitCategory | wallCategory
        body.restitution = 0.05
        body.friction = 0.5
        body.linearDamping = 0.4
        body.angularDamping = 0.6
        body.allowsRotation = true
        preview.physicsBody = body

        nextFruitPreview = nil
        canDrop = false

        run(SKAction.wait(forDuration: 0.5)) { [weak self] in
            self?.canDrop = true
            self?.spawnPreviewFruit()
        }
    }

    // MARK: - Touch handling

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        if isGameOver {
            restart()
            return
        }
        if let touch = touches.first {
            moveAim(to: touch.location(in: self).x)
        }
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard !isGameOver, let touch = touches.first else { return }
        moveAim(to: touch.location(in: self).x)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        guard !isGameOver, let touch = touches.first else { return }
        dropCurrentFruit(atX: touch.location(in: self).x)
    }

    private func moveAim(to x: CGFloat) {
        guard let preview = nextFruitPreview, let tier = preview.userData?["tier"] as? Int,
              let kind = FruitTable.kind(forTier: tier) else { return }
        let clampedX = min(max(x, containerRect.minX + kind.radius), containerRect.maxX - kind.radius)
        preview.position = CGPoint(x: clampedX, y: previewY)
    }

    // MARK: - Contact / merging

    func didBegin(_ contact: SKPhysicsContact) {
        guard let nodeA = contact.bodyA.node as? SKShapeNode,
              let nodeB = contact.bodyB.node as? SKShapeNode,
              nodeA.name == "fruit", nodeB.name == "fruit",
              let tierA = nodeA.userData?["tier"] as? Int,
              let tierB = nodeB.userData?["tier"] as? Int,
              tierA == tierB else { return }

        let idA = ObjectIdentifier(nodeA)
        let idB = ObjectIdentifier(nodeB)
        guard !pendingMerges.contains(idA), !pendingMerges.contains(idB) else { return }
        pendingMerges.insert(idA)
        pendingMerges.insert(idB)

        let midpoint = CGPoint(x: (nodeA.position.x + nodeB.position.x) / 2,
                                y: (nodeA.position.y + nodeB.position.y) / 2)

        nodeA.removeFromParent()
        nodeB.removeFromParent()
        pendingMerges.remove(idA)
        pendingMerges.remove(idB)

        if tierA >= FruitTable.maxTier {
            score += 1000
            return
        }

        let newTier = tierA + 1
        guard let kind = FruitTable.kind(forTier: newTier) else { return }
        let merged = makeFruitNode(kind: kind)
        merged.name = "fruit"
        merged.position = midpoint

        let body = SKPhysicsBody(circleOfRadius: kind.radius)
        body.categoryBitMask = fruitCategory
        body.contactTestBitMask = fruitCategory
        body.collisionBitMask = fruitCategory | wallCategory
        body.restitution = 0.05
        body.friction = 0.5
        body.linearDamping = 0.4
        body.angularDamping = 0.6
        merged.physicsBody = body

        addChild(merged)
        score += newTier * 10
    }

    // MARK: - Game loop

    override func update(_ currentTime: TimeInterval) {
        let dt = lastUpdateTime == 0 ? 0 : currentTime - lastUpdateTime
        lastUpdateTime = currentTime
        guard !isGameOver else { return }

        var overLine = false
        for child in children where child.name == "fruit" {
            guard let body = child.physicsBody else { continue }
            let speed = abs(body.velocity.dx) + abs(body.velocity.dy)
            if speed < 4, child.position.y + (child.frame.height / 2) > dangerLineY {
                overLine = true
                break
            }
        }

        if overLine {
            dangerTimer += dt
            if dangerTimer > 1.5 {
                gameOver()
            }
        } else {
            dangerTimer = 0
        }
    }

    private func gameOver() {
        isGameOver = true
        physicsWorld.speed = 0

        let overlay = SKShapeNode(rectOf: size)
        overlay.fillColor = SKColor(white: 0, alpha: 0.55)
        overlay.strokeColor = .clear
        overlay.position = CGPoint(x: size.width / 2, y: size.height / 2)
        overlay.zPosition = 100
        overlay.name = "overlay"

        let title = SKLabelNode(fontNamed: "AvenirNext-Bold")
        title.text = "Game Over"
        title.fontSize = 36
        title.fontColor = .white
        title.position = CGPoint(x: 0, y: 20)
        overlay.addChild(title)

        let subtitle = SKLabelNode(fontNamed: "AvenirNext-Medium")
        subtitle.text = "Score: \(score) — Tap to restart"
        subtitle.fontSize = 18
        subtitle.fontColor = .white
        subtitle.position = CGPoint(x: 0, y: -20)
        overlay.addChild(subtitle)

        addChild(overlay)
    }

    private func restart() {
        removeAllChildren()
        removeAllActions()
        score = 0
        dangerTimer = 0
        isGameOver = false
        canDrop = true
        physicsWorld.speed = 1
        pendingMerges.removeAll()
        didMove(to: self.view!)
    }
}
