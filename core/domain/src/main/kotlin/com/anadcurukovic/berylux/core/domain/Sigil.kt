package com.anadcurukovic.berylux.core.domain

/**
 * The player's mark: a mirrored 5 by 5 pattern drawn from the profile's seed. The same seed always
 * gives the same sigil. The left half is random and the right half mirrors it, which makes it
 * read like a symbol instead of noise.
 */
data class Sigil(val cells: List<Boolean>) {
    init {
        require(cells.size == SIZE * SIZE) { "a sigil has ${SIZE * SIZE} cells" }
    }

    fun isOn(row: Int, column: Int): Boolean = cells[row * SIZE + column]

    companion object {
        const val SIZE = 5
        private const val HALF = (SIZE + 1) / 2
        private const val MIN_CELLS_ON = 6
        private const val MAX_CELLS_ON = 19

        fun from(seed: Long): Sigil {
            val random = SplitMix64(seed)
            while (true) {
                val half = List(SIZE * HALF) { random.nextInt(2) == 1 }
                val cells = List(SIZE * SIZE) { index ->
                    val row = index / SIZE
                    val column = index % SIZE
                    val mirrored = if (column < HALF) column else SIZE - 1 - column
                    half[row * HALF + mirrored]
                }
                // Nearly empty or nearly full patterns don't read as a mark; roll again.
                if (cells.count { it } in MIN_CELLS_ON..MAX_CELLS_ON) return Sigil(cells)
            }
        }
    }
}
