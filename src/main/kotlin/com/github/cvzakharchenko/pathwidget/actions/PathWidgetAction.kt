package com.github.cvzakharchenko.pathwidget.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class PathWidgetAction : AnAction(), CustomComponentAction, DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        // No action needed
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return PathLabel()
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        if (project == null) {
            e.presentation.text = ""
            e.presentation.description = null
            return
        }

        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()
        if (file == null) {
            e.presentation.text = ""
            e.presentation.description = null
            return
        }

        val baseDir = project.guessProjectDir()
        val relativePath = if (baseDir != null) {
            VfsUtilCore.getRelativePath(file, baseDir)
        } else {
            file.path
        }

        e.presentation.text = (relativePath ?: file.name).replace("/", " / ")
        e.presentation.description = file.path
    }

    override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
        if (component is PathLabel) {
            component.text = presentation.text
            component.toolTipText = presentation.description
        }
    }
}

class PathLabel : JBLabel() {
    init {
        border = JBUI.Borders.empty(0, 10)
        text = ""
    }
}
