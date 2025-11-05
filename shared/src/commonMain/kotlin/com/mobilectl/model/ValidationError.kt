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
        println("❌ Configuration errors:")
        errors.forEach { println("   $it") }
    }

    if (warnings.isNotEmpty()) {
        if (errors.isNotEmpty()) println()
        println("⚠️  Configuration warnings:")
        warnings.forEach { println("   $it") }
    }
}
