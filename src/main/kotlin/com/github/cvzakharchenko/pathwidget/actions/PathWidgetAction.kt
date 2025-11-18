package com.github.cvzakharchenko.pathwidget.actions

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.JBLabel
import com.intellij.util.messages.MessageBusConnection
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.SwingUtilities

class PathWidgetAction : AnAction(), CustomComponentAction, DumbAware {

    override fun actionPerformed(e: AnActionEvent) {
        // No action needed
    }

    override fun createCustomComponent(presentation: Presentation, place: String): JComponent {
        return PathLabel()
    }

    override fun update(e: AnActionEvent) {
        // Update presentation if needed, but component handles its own state
    }
}

class PathLabel : JBLabel(), FileEditorManagerListener {
    private var connection: MessageBusConnection? = null
    private var currentProject: Project? = null

    init {
        border = JBUI.Borders.empty(0, 10)
        text = ""
    }

    override fun addNotify() {
        super.addNotify()
        ensureProjectBinding()
    }

    override fun removeNotify() {
        super.removeNotify()
        connection?.disconnect()
        connection = null
        currentProject = null
    }

    override fun selectionChanged(event: FileEditorManagerEvent) {
        updatePath(event.newFile)
    }

    private fun ensureProjectBinding() {
        if (currentProject != null) {
            return
        }

        val dataManager = DataManager.getInstance()

        val directContext = runCatching { dataManager.getDataContext(this) }.getOrNull()
        val projectFromComponent = directContext?.let { CommonDataKeys.PROJECT.getData(it) }
        if (projectFromComponent != null) {
            attachToProject(projectFromComponent)
            return
        }

        val openProjects = ProjectManager.getInstance().openProjects.filterNot { it.isDisposed }
        if (openProjects.size == 1) {
            attachToProject(openProjects.first())
            return
        }

        dataManager.getDataContextFromFocusAsync().onSuccess { context ->
            val projectFromFocus = CommonDataKeys.PROJECT.getData(context)
            if (projectFromFocus != null) {
                SwingUtilities.invokeLater {
                    if (isDisplayable) {
                        attachToProject(projectFromFocus)
                    }
                }
            }
        }
    }

    private fun attachToProject(project: Project) {
        if (project.isDisposed || currentProject == project) {
            return
        }

        connection?.disconnect()
        currentProject = project
        connection = project.messageBus.connect()
        connection?.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this)

        val selectedFiles = FileEditorManager.getInstance(project).selectedFiles
        updatePath(selectedFiles.firstOrNull())
    }

    private fun updatePath(file: VirtualFile?) {
        if (currentProject == null || file == null) {
            text = ""
            toolTipText = null
            return
        }

        val baseDir = currentProject?.guessProjectDir()
        val relativePath = if (baseDir != null) {
            VfsUtilCore.getRelativePath(file, baseDir)
        } else {
            file.path
        }

        text = (relativePath ?: file.name).replace("/", " / ")
        toolTipText = file.path
    }
}

