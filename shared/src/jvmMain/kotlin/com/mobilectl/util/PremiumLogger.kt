package com.mobilectl.util

object PremiumLogger {
    private const val GREEN = "\u001B[32m"
    private const val CYAN = "\u001B[36m"
    private const val YELLOW = "\u001B[33m"
    private const val GRAY = "\u001B[90m"
    private const val WHITE = "\u001B[97m"
    private const val RED = "\u001B[31m"
    private const val RESET = "\u001B[0m"
    private const val BOLD = "\u001B[1m"
    private const val DIM = "\u001B[2m"

    fun header(title: String, icon: String = "●", success: Boolean = true) {
        val color = if (success) GREEN else YELLOW
        println()
        println("$GRAY┌─────────────────────────────────────────────────────────┐$RESET")
        println("$GRAY│$RESET  $color$icon$RESET  $BOLD$WHITE$title$RESET")
        println("$GRAY└─────────────────────────────────────────────────────────┘$RESET")
        println()
    }

    fun section(title: String) {
        println()
        println("$GRAY┌─────────────────────────────────────────────────────────┐$RESET")
        println("$GRAY│$RESET  $CYAN▶$RESET  $BOLD$WHITE$title$RESET")
        println("$GRAY├─────────────────────────────────────────────────────────┤$RESET")
    }

    fun sectionEnd() {
        println("$GRAY└─────────────────────────────────────────────────────────┘$RESET")
        println()
    }

    fun detail(label: String, value: String, dim: Boolean = false) {
        val valueColor = if (dim) DIM else ""
        val padding = maxOf(0, 16 - label.length)
        println("$GRAY│$RESET  ${DIM}$label$RESET${" ".repeat(padding)}$valueColor$value$RESET")
    }

    fun progress(message: String) {
        println("$GRAY│$RESET  $CYAN⋯$RESET  $DIM$message$RESET")
    }

    fun success(message: String) {
        println("$GRAY│$RESET  $GREEN✓$RESET  $message")
    }

    fun warning(message: String) {
        println("$GRAY│$RESET  $YELLOW⚠$RESET  $DIM$message$RESET")
    }

    fun error(message: String) {
        println("$GRAY│$RESET  $RED✗$RESET  $message")
    }

    fun info(message: String) {
        println("  $DIM$message$RESET")
    }

    fun divider() {
        println("  ${DIM}${GRAY}${"━".repeat(57)}$RESET")
    }

    fun box(title: String, items: Map<String, String>, success: Boolean = true) {
        val icon = if (success) "✓" else "⚠"
        val color = if (success) GREEN else YELLOW

        println()
        println("$GRAY┌─────────────────────────────────────────────────────────┐$RESET")
        println("$GRAY│$RESET  $color$icon$RESET  $BOLD$WHITE$title$RESET")
        println("$GRAY├─────────────────────────────────────────────────────────┤$RESET")

        items.forEach { (label, value) ->
            detail(label, value)
        }

        println("$GRAY└─────────────────────────────────────────────────────────┘$RESET")
        println()
    }

    fun simpleSuccess(message: String) {
        println("  $GREEN✓$RESET  $message")
    }

    fun simpleError(message: String) {
        println("  $RED✗$RESET  $message")
    }

    fun simpleWarning(message: String) {
        println("  $YELLOW⚠$RESET  $DIM$message$RESET")
    }

    fun simpleInfo(message: String) {
        println("  $CYAN▶$RESET  $message")
    }
}
