package com.mobilectl.model

enum class ValidationSeverity {
    ERROR,      // Must fix
    WARNING     // Should fix
}

data class ValidationError(
    val field: String,           // "changelog.output_file"
    val message: String,         // "Cannot be empty"
    val severity: ValidationSeverity = ValidationSeverity.ERROR,
    val suggestion: String? = null
) {
    override fun toString(): String {
        return buildString {
            val prefix = when (severity) {
                ValidationSeverity.ERROR -> "❌"
                ValidationSeverity.WARNING -> "⚠️"
            }
            append("$prefix $field: $message")
            if (suggestion != null) {
                append("\n   💡 $suggestion")
            }
        }
    }
}

// Helper extensions
fun List<ValidationError>.hasErrors(): Boolean = any { it.severity == ValidationSeverity.ERROR }
fun List<ValidationError>.errors(): List<ValidationError> = filter { it.severity == ValidationSeverity.ERROR }
fun List<ValidationError>.warnings(): List<ValidationError> = filter { it.severity == ValidationSeverity.WARNING }

fun List<ValidationError>.printReport() {
    if (isEmpty()) return

    val errors = errors()
    val warnings = warnings()

    if (errors.isNotEmpty()) {
        val gray = "\u001B[90m"
        val white = "\u001B[97m"
        val red = "\u001B[31m"
        val yellow = "\u001B[33m"
        val reset = "\u001B[0m"
        val bold = "\u001B[1m"
        val dim = "\u001B[2m"

        println()
        println("$gray┌─────────────────────────────────────────────────────────┐$reset")
        println("$gray│$reset  $red✗$reset  ${bold}${white}Configuration Errors$reset")
        println("$gray├─────────────────────────────────────────────────────────┤$reset")

        errors.forEach { error ->
            println("$gray│$reset")
            println("$gray│$reset  ${dim}${error.field}$reset")
            println("$gray│$reset  $red✗$reset  ${error.message}")
            if (error.suggestion != null) {
                println("$gray│$reset  ${yellow}💡$reset  ${dim}${error.suggestion}$reset")
            }
        }

        println("$gray└─────────────────────────────────────────────────────────┘$reset")
        println()
    }

    if (warnings.isNotEmpty()) {
        val gray = "\u001B[90m"
        val white = "\u001B[97m"
        val yellow = "\u001B[33m"
        val reset = "\u001B[0m"
        val bold = "\u001B[1m"
        val dim = "\u001B[2m"

        println()
        println("$gray┌─────────────────────────────────────────────────────────┐$reset")
        println("$gray│$reset  $yellow⚠$reset  ${bold}${white}Configuration Warnings$reset")
        println("$gray├─────────────────────────────────────────────────────────┤$reset")

        warnings.forEach { warning ->
            println("$gray│$reset")
            println("$gray│$reset  ${dim}${warning.field}$reset")
            println("$gray│$reset  $yellow⚠$reset  ${warning.message}")
            if (warning.suggestion != null) {
                println("$gray│$reset  ${yellow}💡$reset  ${dim}${warning.suggestion}$reset")
            }
        }

        println("$gray└─────────────────────────────────────────────────────────┘$reset")
        println()
    }
}
