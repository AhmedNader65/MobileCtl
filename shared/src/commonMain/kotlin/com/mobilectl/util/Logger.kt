package com.mobilectl.util

/**
 * Platform-agnostic logger interface
 */
expect interface Logger {
    fun info(message: String)
    fun debug(message: String)
    fun warn(message: String)
    fun error(message: String)
    fun error(message: String, throwable: Throwable)
}

expect fun createLogger(tag: String): Logger