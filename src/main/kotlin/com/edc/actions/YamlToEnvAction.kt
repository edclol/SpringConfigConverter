package com.edc.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class YamlToEnvAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        val convertedText = convertYamlToEnv(selectedText)
        
        CopyPasteManager.getInstance().setContents(StringSelection(convertedText))
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }
    
    private fun convertYamlToEnv(yaml: String): String {
        val lines = yaml.lines()
        val result = mutableListOf<String>()
        val prefixStack = mutableListOf<String>()
        
        for (line in lines) {
            val trimmed = line.trim()
            
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue
            }
            
            val indent = line.indexOfFirst { !it.isWhitespace() }
            val level = indent / 2
            
            if (trimmed.contains(":")) {
                val parts = trimmed.split(":", limit = 2)
                val key = parts[0].trim()
                val value = parts.getOrNull(1)?.trim()
                
                if (value.isNullOrEmpty()) {
                    if (level < prefixStack.size) {
                        repeat(prefixStack.size - level) {
                            prefixStack.removeAt(prefixStack.lastIndex)
                        }
                    }
                    prefixStack.add(key.uppercase())
                } else {
                    val fullKey = if (prefixStack.isNotEmpty()) {
                        "${prefixStack.joinToString("_")}_${key.uppercase()}"
                    } else {
                        key.uppercase()
                    }
                    
                    val cleanValue = value.removeSurrounding("\"").removeSurrounding("'")
                    result.add("$fullKey=$cleanValue")
                }
            }
        }
        
        return result.joinToString("\n")
    }
}
