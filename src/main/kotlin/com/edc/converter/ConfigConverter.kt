package com.edc.converter

import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml

/**
 * Spring Boot Relaxed Binding 互转:
 * - ENV 键:小写点分键名 -> 大写、"." 变 "_"、"-" 原样保留,列表用 "_0_" 索引
 * - Properties 键:保持小写点分,列表用 "key[0]" 索引
 * - 扁平键按下划线/点号切分还原为嵌套结构,连续数字键还原为列表
 */
object ConfigConverter {

    private val indexSegment = Regex("^(.*)\\[(\\d+)]$")
    private val numericKey = Regex("\\d+")

    fun convert(text: String, from: ConfigFormat, to: ConfigFormat): String {
        val root = parse(text, from)
        if (root.isEmpty()) {
            throw IllegalArgumentException("未找到可转换的键值配置")
        }
        return render(normalize(root), to)
    }

    private fun parse(text: String, format: ConfigFormat): LinkedHashMap<String, Any> = when (format) {
        ConfigFormat.YAML -> parseYaml(text)
        ConfigFormat.PROPERTIES -> parseFlat(text, separator = '.', lowercaseKeys = false)
        ConfigFormat.ENV -> parseFlat(text, separator = '_', lowercaseKeys = true)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseYaml(text: String): LinkedHashMap<String, Any> {
        val loaded: Any? = Yaml().load<Any>(text) ?: return LinkedHashMap()
        if (loaded !is Map<*, *>) {
            throw IllegalArgumentException("YAML 顶层必须是键值映射")
        }
        val result = LinkedHashMap<String, Any>()
        for ((key, value) in loaded as Map<Any, Any>) {
            result[key.toString()] = value
        }
        return result
    }

    private fun parseFlat(
        text: String,
        separator: Char,
        lowercaseKeys: Boolean,
    ): LinkedHashMap<String, Any> {
        val root = LinkedHashMap<String, Any>()
        var hasEntry = false
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val index = line.indexOf('=')
            if (index <= 0) {
                throw IllegalArgumentException("无效的键值行: $line")
            }
            hasEntry = true
            val rawKey = line.substring(0, index).trim()
            val key = if (lowercaseKeys) rawKey.lowercase() else rawKey
            val value = line.substring(index + 1).trim().removeSurrounding("\"")
            insert(root, splitKey(key, separator), value)
        }
        if (!hasEntry) {
            throw IllegalArgumentException("未找到有效的 KEY=VALUE 行")
        }
        return root
    }

    private fun splitKey(key: String, separator: Char): List<String> =
        key.split(separator)
            .filter { it.isNotEmpty() }
            .flatMap { segment ->
                indexSegment.find(segment)?.destructured?.let { (name, index) ->
                    listOf(name, index)
                } ?: listOf(segment)
            }

    private fun insert(node: LinkedHashMap<String, Any>, path: List<String>, value: String) {
        val key = path.first()
        if (path.size == 1) {
            node[key] = value
            return
        }
        val child = node[key]
        val map = if (child is LinkedHashMap<*, *>) {
            @Suppress("UNCHECKED_CAST")
            child as LinkedHashMap<String, Any>
        } else {
            LinkedHashMap<String, Any>().also { node[key] = it }
        }
        insert(map, path.drop(1), value)
    }

    private fun normalize(node: LinkedHashMap<String, Any>): Any {
        val normalized = LinkedHashMap<String, Any>()
        node.forEach { (key, value) ->
            normalized[key] = if (value is LinkedHashMap<*, *>) {
                @Suppress("UNCHECKED_CAST")
                normalize(value as LinkedHashMap<String, Any>)
            } else {
                value
            }
        }
        val keys = normalized.keys.toList()
        if (keys.isNotEmpty() && keys.all { it.matches(numericKey) }) {
            val indices = keys.map { it.toInt() }
            if (indices == (0 until keys.size).toList()) {
                return keys.map { normalized[it]!! }
            }
        }
        return normalized
    }

    private fun render(root: Any, format: ConfigFormat): String = when (format) {
        ConfigFormat.YAML -> {
            val options = DumperOptions().apply {
                defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                indent = 2
                indicatorIndent = 2
                indentWithIndicator = true
            }
            Yaml(options).dump(coerceScalars(root))
        }
        ConfigFormat.PROPERTIES -> flatten(root, envStyle = false)
            .joinToString("\n") { (key, value) -> "$key=$value" }
        ConfigFormat.ENV -> flatten(root, envStyle = true)
            .joinToString("\n") { (key, value) -> "$key=$value" }
    }

    private fun flatten(root: Any, envStyle: Boolean): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        walk(emptyList(), root, out, envStyle)
        if (out.isEmpty()) {
            throw IllegalArgumentException("未找到可转换的键值配置")
        }
        return out
    }

    private fun walk(path: List<String>, value: Any?, out: MutableList<Pair<String, String>>, envStyle: Boolean) {
        when (value) {
            is Map<*, *> -> value.forEach { (k, v) -> walk(path + k.toString(), v, out, envStyle) }
            is List<*> -> value.forEachIndexed { index, item ->
                if (envStyle) {
                    walk(path + index.toString(), item, out, envStyle)
                } else {
                    walk(appendIndex(path, index), item, out, envStyle)
                }
            }
            null -> {}
            else -> out.add(joinKey(path, envStyle) to value.toString())
        }
    }

    private fun appendIndex(path: List<String>, index: Int): List<String> =
        if (path.isEmpty()) {
            listOf(index.toString())
        } else {
            path.dropLast(1) + (path.last() + "[$index]")
        }

    private fun joinKey(path: List<String>, envStyle: Boolean): String =
        path.joinToString(if (envStyle) "_" else ".") { segment ->
            if (envStyle) segment.replace(".", "_").uppercase() else segment
        }

    /**
     * 扁平键值过来的标量都是字符串,转 YAML 时还原为数字/布尔,
     * 避免 SnakeYAML 为保类型输出 '8080' 这类带引号的标量
     */
    @Suppress("UNCHECKED_CAST")
    private fun coerceScalars(value: Any): Any = when (value) {
        is Map<*, *> -> {
            val out = LinkedHashMap<String, Any>()
            value.forEach { (k, v) -> if (v != null) out[k.toString()] = coerceScalars(v) }
            out
        }
        is List<*> -> value.map { coerceScalars(it ?: "") }
        is String -> coerceScalar(value)
        else -> value
    }

    private fun coerceScalar(text: String): Any = when {
        text.equals("true", ignoreCase = true) || text.equals("false", ignoreCase = true) -> text.toBoolean()
        text.length > 1 && text.startsWith("0") -> text
        else -> text.toLongOrNull() ?: text
    }
}
