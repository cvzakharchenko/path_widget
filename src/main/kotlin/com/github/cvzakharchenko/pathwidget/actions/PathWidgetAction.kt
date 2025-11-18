package com.github.cvzakharchenko.pathwidget.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.ui.components.JBLabel
import com.intellij.util.messages.MessageBusConnection
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
        e.presentation.putClientProperty(PROJECT_KEY, project)
    }

    override fun updateCustomComponent(component: JComponent, presentation: Presentation) {
        if (component is PathLabel) {
            val project = presentation.getClientProperty(PROJECT_KEY)
            component.bindToProject(project)
        }
    }

    companion object {
        private val PROJECT_KEY = Key.create<Project>("PathWidgetAction.Project")
    }
}

class PathLabel : JBLabel(), FileEditorManagerListener {
    private var connection: MessageBusConnection? = null
    private var currentProject: Project? = null

    init {
        border = JBUI.Borders.empty(0, 10)
        text = ""
    }

    fun bindToProject(project: Project?) {
        if (currentProject === project) {
            return
        }

        connection?.disconnect()
        connection = null
        currentProject = null

        if (project != null && !project.isDisposed) {
            currentProject = project
            connection = project.messageBus.connect()
            connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)
            updatePath()
        } else {
            text = ""
            toolTipText = null
        }
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        updatePath()
    }

    private fun updatePath() {
        val project = currentProject ?: return
        if (project.isDisposed) return

        val file = FileEditorManager.getInstance(project).selectedFiles.firstOrNull()

        if (file == null) {
            text = ""
            toolTipText = null
            return
        }

        val baseDir = project.guessProjectDir()
        val relativePath = if (baseDir != null) {
            VfsUtilCore.getRelativePath(file, baseDir)
        } else {
            file.path
        }

        text = (relativePath ?: file.name).replace("/", " / ")
        toolTipText = file.path
    }

    override fun removeNotify() {
        super.removeNotify()
        connection?.disconnect()
        connection = null
        currentProject = null
    }
}
