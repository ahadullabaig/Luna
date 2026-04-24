package com.luna.app.domain.model

enum class FlowLevel {
    LIGHT, MEDIUM, HEAVY
}

enum class Energy {
    TIRED, NEUTRAL, ENERGETIC
}

object PainFlag {
    const val CRAMPS    = 1 shl 0
    const val HEADACHE  = 1 shl 1
    const val BACKACHE  = 1 shl 2
    const val BLOATING  = 1 shl 3
}

object BodyFlag {
    const val FEVER     = 1 shl 0
    const val NAUSEA    = 1 shl 1
}
