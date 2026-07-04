package com.example.suikagameclone

/** A single fruit tier definition shared by every fruit of that kind. Radius is in dp. */
data class FruitKind(
    val tier: Int,
    val emoji: String,
    val radius: Float,
    val r: Int,
    val g: Int,
    val b: Int
)

object FruitTable {
    /** Tiers 1...11, smallest to largest (Cherry -> Watermelon). */
    val kinds: List<FruitKind> = listOf(
        FruitKind(1, "🍒", 14f, 204, 38, 51),   // cherry 🍒
        FruitKind(2, "🍓", 18f, 230, 64, 89),   // strawberry 🍓
        FruitKind(3, "🍇", 22f, 115, 64, 140),  // grape 🍇
        FruitKind(4, "🍋", 27f, 242, 217, 51),  // lemon 🍋
        FruitKind(5, "🍊", 33f, 242, 140, 38),  // orange 🍊
        FruitKind(6, "🍎", 40f, 217, 38, 38),   // apple 🍎
        FruitKind(7, "🍑", 49f, 250, 179, 140), // peach 🍑
        FruitKind(8, "🍐", 59f, 191, 217, 89),  // pear 🍐
        FruitKind(9, "🍍", 71f, 230, 191, 51),  // pineapple 🍍
        FruitKind(10, "🍈", 86f, 166, 217, 140), // melon 🍈
        FruitKind(11, "🍉", 104f, 51, 166, 89)   // watermelon 🍉
    )

    val maxTier: Int get() = kinds.size

    /** Pool of tiers that can appear as the next fruit to drop. */
    val spawnPool = listOf(1, 2, 3, 4, 5)

    fun kind(tier: Int): FruitKind? = kinds.getOrNull(tier - 1)

    fun randomSpawnTier(): Int = spawnPool.random()
}
