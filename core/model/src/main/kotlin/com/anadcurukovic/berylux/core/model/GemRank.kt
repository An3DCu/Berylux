package com.anadcurukovic.berylux.core.model

/** The ranks, from the clear beryl everyone starts with to the rarest one. In order. */
enum class GemRank(val minLux: Int) {
    GOSHENITE(minLux = 0),
    HELIODOR(minLux = 500),
    MORGANITE(minLux = 2_000),
    AQUAMARINE(minLux = 5_000),
    EMERALD(minLux = 11_000),
    RED_BERYL(minLux = 22_000),
}
