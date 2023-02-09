package com.bitpunchlab.android.analyzer



enum class AppState {
    NORMAL,
    SCANNING
}

enum class BatteryInfo {
    LEVEL,
    VOLTAGE,
    TEMPERATURE,
    HEALTH,
    IS_CHARGING
}

enum class SignalStrength(val level: Int) {
    NO(0),
    POOR(1),
    FAIR(2),
    GOOD(3),
    EXCELLENT(4);

    companion object {
        fun fromInt(value: Int) = SignalStrength.values().first { it.level == value }
    }
}