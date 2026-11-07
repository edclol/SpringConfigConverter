package com.edc.converter

object ConfigFormatDetector {

    private val kvLine = Regex("^[A-Za-z0-9_.\\-\\[\\]]+=")

    fun fromFileName(fileName: String?): ConfigFormat? {
        val name = fileName?.lowercase() ?: return null
        return when {
            name.endsWith(".yml") || name.endsWith(".yaml") -> ConfigFormat.YAML
            name.endsWith(".properties") -> ConfigFormat.PROPERTIES
            name.endsWith(".env") -> ConfigFormat.ENV
            else -> null
        }
    }

    fun fromContent(text: String): ConfigFormat {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
        val kvLines = lines.filter { kvLine.matches(it) }
        if (kvLines.isNotEmpty()) {
            val hasDotInKey = kvLines.any { it.substringBefore('=').contains('.') }
            return if (hasDotInKey) ConfigFormat.PROPERTIES else ConfigFormat.ENV
        }
        return ConfigFormat.YAML
    }

    fun detect(fileName: String?, content: String): ConfigFormat =
        fromFileName(fileName) ?: fromContent(content)
}
