package com.edc.intentions

import com.edc.ConverterNotifications
import com.edc.converter.ConfigConverter
import com.edc.converter.ConfigFormat
import com.edc.converter.ConfigFormatDetector
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

abstract class BaseConvertIntention(private val target: ConfigFormat) : IntentionAction {

    override fun startInWriteAction(): Boolean = false

    override fun getFamilyName(): String = "Spring 配置转换"

    override fun getText(): String = "转换为 ${target.label}"

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile?): Boolean {
        if (editor == null || file == null || !editor.selectionModel.hasSelection()) return false
        val selection = editor.selectionModel.selectedText ?: return false
        val source = ConfigFormatDetector.detect(file.name, selection)
        return source != target
    }

    override fun invoke(project: Project, editor: Editor?, file: PsiFile?) {
        val ed = editor ?: return
        val selection = ed.selectionModel.selectedText ?: return
        val source = file?.let { ConfigFormatDetector.detect(it.name, selection) } ?: return

        try {
            val converted = ConfigConverter.convert(selection, source, target)
            WriteCommandAction.runWriteCommandAction(project) {
                ed.document.replaceString(
                    ed.selectionModel.selectionStart,
                    ed.selectionModel.selectionEnd,
                    converted,
                )
            }
            ConverterNotifications.info(project, "已转换为 ${target.label}")
        } catch (ex: Exception) {
            ConverterNotifications.error(project, "转换失败: ${ex.message}")
        }
    }
}
