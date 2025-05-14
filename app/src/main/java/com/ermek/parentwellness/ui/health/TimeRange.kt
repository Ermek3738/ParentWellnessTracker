package com.ermek.parentwellness.ui.health

enum class TimeRange {
    DAY, WEEK, MONTH, YEAR;

    companion object {
        @Suppress("unused")
        fun fromString(value: String): TimeRange {
            return when(value.lowercase()) {
                "day" -> DAY
                "week" -> WEEK
                "month" -> MONTH
                "year" -> YEAR
                else -> WEEK
            }
        }
    }
}