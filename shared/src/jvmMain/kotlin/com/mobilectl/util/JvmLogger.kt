package com.mobilectl.util

actual interface Logger {
    actual fun info(message: String)
    actual fun debug(message: String)
    actual fun warn(message: String)
    actual fun error(message: String)
    actual fun error(message: String, throwable: Throwable)
}

actual fun createLogger(tag: String): Logger = JvmLoggerImpl(tag)

class JvmLoggerImpl(private val tag: String) : Logger {
    private val isDebug = System.getenv("DEBUG") != null

    override fun info(message: String) {
        println("ℹ️  [$tag] $message")
    }

    override fun debug(message: String) {
        if (isDebug) {
            println("🐛 [$tag] $message")
        }
    }

    override fun warn(message: String) {
        println("⚠️  [$tag] $message")
    }

    override fun error(message: String) {
        System.err.println("❌ [$tag] $message")
    }

    override fun error(message: String, throwable: Throwable) {
        System.err.println("❌ [$tag] $message")
        throwable.printStackTrace(System.err)
    }
}
