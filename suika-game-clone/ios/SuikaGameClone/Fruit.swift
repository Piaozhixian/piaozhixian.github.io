import CoreGraphics

/// A single fruit tier definition shared by every fruit of that kind.
struct FruitKind {
    let tier: Int
    let emoji: String
    let radius: CGFloat
    let color: (r: CGFloat, g: CGFloat, b: CGFloat)
}

enum FruitTable {
    /// Tiers 1...11, smallest to largest (Cherry -> Watermelon).
    static let kinds: [FruitKind] = [
        FruitKind(tier: 1, emoji: "🍒", radius: 14, color: (0.80, 0.15, 0.20)),
        FruitKind(tier: 2, emoji: "🍓", radius: 18, color: (0.90, 0.25, 0.35)),
        FruitKind(tier: 3, emoji: "🍇", radius: 22, color: (0.45, 0.25, 0.55)),
        FruitKind(tier: 4, emoji: "🍋", radius: 27, color: (0.95, 0.85, 0.20)),
        FruitKind(tier: 5, emoji: "🍊", radius: 33, color: (0.95, 0.55, 0.15)),
        FruitKind(tier: 6, emoji: "🍎", radius: 40, color: (0.85, 0.15, 0.15)),
        FruitKind(tier: 7, emoji: "🍑", radius: 49, color: (0.98, 0.70, 0.55)),
        FruitKind(tier: 8, emoji: "🍐", radius: 59, color: (0.75, 0.85, 0.35)),
        FruitKind(tier: 9, emoji: "🍍", radius: 71, color: (0.90, 0.75, 0.20)),
        FruitKind(tier: 10, emoji: "🍈", radius: 86, color: (0.65, 0.85, 0.55)),
        FruitKind(tier: 11, emoji: "🍉", radius: 104, color: (0.20, 0.65, 0.35)),
    ]

    static var maxTier: Int { kinds.count }

    /// Pool of tiers that can appear as the next fruit to drop.
    static let spawnPool = [1, 2, 3, 4, 5]

    static func kind(forTier tier: Int) -> FruitKind? {
        guard tier >= 1 && tier <= kinds.count else { return nil }
        return kinds[tier - 1]
    }

    static func randomSpawnTier() -> Int {
        spawnPool.randomElement() ?? 1
    }
}
