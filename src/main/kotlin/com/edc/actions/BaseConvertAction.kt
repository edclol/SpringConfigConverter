package com.edc.actions

import com.edc.ConverterNotifications
import com.edc.converter.ConfigConverter
import com.edc.converter.ConfigFormat
import com.edc.converter.ConfigFormatDetector
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import java.awt.datatransfer.StringSelection
import java.io.IOException

abstract class BaseConvertAction(private val target: ConfigFormat) : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val selection = editor?.selectionModel?.takeIf { it.hasSelection() }?.selectedText

        if (selection != null) {
            convertSelectionToClipboard(project, virtualFile.name, selection)
        } else {
            convertWholeFileToNewFile(project, virtualFile)
        }
    }

    private fun convertSelectionToClipboard(project: Project, fileName: String?, selection: String) {
        val source = ConfigFormatDetector.detect(fileName, selection)
        try {
            val converted = ConfigConverter.convert(selection, source, target)
            CopyPasteManager.getInstance().setContents(StringSelection(converted))
            ConverterNotifications.info(project, "已转换为 ${target.label}，结果已复制到剪贴板")
        } catch (ex: Exception) {
            ConverterNotifications.error(project, "转换失败: ${ex.message}")
        }
    }

    private fun convertWholeFileToNewFile(project: Project, sourceFile: VirtualFile) {
        val parent = sourceFile.parent ?: return
        val text = FileDocumentManager.getInstance().getDocument(sourceFile)?.text
        if (text.isNullOrEmpty()) {
            ConverterNotifications.error(project, "无法读取文件内容: ${sourceFile.name}")
            return
        }
        val targetName = "${sourceFile.nameWithoutExtension}.${target.fileExtension}"
        try {
            val converted = ConfigConverter.convert(text, ConfigFormatDetector.detect(sourceFile.name, text), target)
            ApplicationManager.getApplication().runWriteAction {
                if (parent.findChild(targetName) != null) {
                    throw IOException("目标文件已存在: $targetName")
                }
                VfsUtil.saveText(parent.createChildData(this@BaseConvertAction, targetName), converted)
            }
            ConverterNotifications.info(project, "已生成 $targetName")
        } catch (ex: Exception) {
            ConverterNotifications.error(project, "转换失败: ${ex.message}")
        }
    }

    override fun update(e: AnActionEvent) {
        val editor = e.getData(CommonDataKeys.EDITOR)
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
        val selection = editor?.selectionModel?.takeIf { it.hasSelection() }?.selectedText

        val source: ConfigFormat? = when {
            selection != null -> ConfigFormatDetector.detect(virtualFile?.name, selection)
            virtualFile != null && !virtualFile.isDirectory -> ConfigFormatDetector.fromFileName(virtualFile.name)
            else -> null
        }
        e.presentation.isEnabledAndVisible = source != null && source != target
    }
}
