package com.edc.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import java.awt.datatransfer.StringSelection

class EnvToYamlAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val selectedText = editor.selectionModel.selectedText ?: return
        
        val convertedText = convertEnvToYaml(selectedText)
        
        CopyPasteManager.getInstance().setContents(StringSelection(convertedText))
    }
    
    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val hasSelection = editor?.selectionModel?.hasSelection() == true
        e.presentation.isEnabledAndVisible = hasSelection
    }
    
    private fun convertEnvToYaml(env: String): String {
        val lines = env.lines().filter { it.trim().isNotEmpty() && !it.trim().startsWith("#") }
        val result = mutableListOf<String>()
        
        data class Node(val name: String, val children: MutableList<Node> = mutableListOf(), var value: String? = null)
        
        val root = Node("root")
        
        for (line in lines) {
            val trimmed = line.trim()
            if (!trimmed.contains("=")) continue
            
            val parts = trimmed.split("=", limit = 2)
            val key = parts[0].trim()
            val value = parts.getOrNull(1)?.trim() ?: ""
            
            val keys = key.split("_")
            var currentNode = root
            
            for (i in keys.indices) {
                val k = keys[i].lowercase()
                val existingNode = currentNode.children.find { it.name == k }
                
                if (existingNode != null) {
                    currentNode = existingNode
                } else {
                    val newNode = Node(k)
                    currentNode.children.add(newNode)
                    currentNode = newNode
                }
                
                if (i == keys.lastIndex) {
                    currentNode.value = value
                }
            }
        }
        
        fun buildYaml(node: Node, indent: Int): List<String> {
            val lines = mutableListOf<String>()
            val prefix = "  ".repeat(indent)
            
            val childGroups = node.children.groupBy { it.value == null }
            val nestedNodes = childGroups[true] ?: emptyList()
            val leafNodes = childGroups[false] ?: emptyList()
            
            for (child in nestedNodes) {
                lines.add("$prefix${child.name}:")
                lines.addAll(buildYaml(child, indent + 1))
            }
            
            for (child in leafNodes) {
                lines.add("$prefix${child.name}: ${child.value}")
            }
            
            return lines
        }
        
        result.addAll(buildYaml(root, 0))
        
        return result.joinToString("\n")
    }
}
