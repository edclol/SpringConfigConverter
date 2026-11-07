package com.edc.converter

enum class ConfigFormat(val label: String, val fileExtension: String) {
    YAML("YAML", "yml"),
    PROPERTIES("Properties", "properties"),
    ENV("ENV", "env"),
}
