package com.mobilectl.util

object TimeFormatter {
    fun formatDuration(seconds: Long): String {
        return when {
            seconds < 60 -> "${seconds}s"
            seconds < 3600 -> {
                val minutes = seconds / 60
                val secs = seconds % 60
                if (secs == 0L) "${minutes}m" else "${minutes}m ${secs}s"
            }
            seconds < 86400 -> {
                val hours = seconds / 3600
                val minutes = (seconds % 3600) / 60
                if (minutes == 0L) "${hours}h" else "${hours}h ${minutes}m"
            }
            else -> {
                val days = seconds / 86400
                val hours = (seconds % 86400) / 3600
                if (hours == 0L) "${days}d" else "${days}d ${hours}h"
            }
        }
    }

    fun formatExpiry(seconds: Int): String {
        return when {
            seconds < 60 -> "$seconds seconds"
            seconds < 120 -> "~1 minute"
            seconds < 3600 -> "~${seconds / 60} minutes"
            seconds < 7200 -> "~1 hour"
            else -> "~${seconds / 3600} hours"
        }
    }
}
