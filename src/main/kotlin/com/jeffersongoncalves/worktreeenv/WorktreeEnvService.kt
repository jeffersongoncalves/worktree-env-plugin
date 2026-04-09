package com.jeffersongoncalves.worktreeenv

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import java.io.File

enum class WorktreeStatus {
    NOT_WORKTREE,
    UNCONFIGURED,
    CONFIGURED,
}

@Service(Service.Level.PROJECT)
class WorktreeEnvService(private val project: Project) {

    var info: WorktreeInfo? = null
        private set

    var status: WorktreeStatus = WorktreeStatus.NOT_WORKTREE
        private set

    var currentAppUrl: String? = null
        private set

    init {
        refresh()
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val basePath = project.basePath ?: return
                val relevant = events.any { event ->
                    val path = when (event) {
                        is VFileCreateEvent -> event.path
                        is VFileDeleteEvent -> event.path
                        is VFileContentChangeEvent -> event.path
                        else -> null
                    }
                    path != null && path.replace("\\", "/").let { p ->
                        p == "$basePath/.env".replace("\\", "/") ||
                            p == "$basePath/.env.testing".replace("\\", "/")
                    }
                }
                if (relevant) {
                    refresh()
                }
            }
        })
    }

    fun refresh() {
        val detected = WorktreeDetector.detect(project)
        info = detected

        if (detected == null) {
            status = WorktreeStatus.NOT_WORKTREE
            currentAppUrl = null
        } else {
            val envFile = File(detected.worktreeRoot, ".env")
            if (!envFile.exists()) {
                status = WorktreeStatus.UNCONFIGURED
                currentAppUrl = null
            } else {
                val appUrl = EnvConfigurator.readEnvValue(envFile, "APP_URL")
                if (appUrl != null && appUrl.contains(detected.worktreeFolderName)) {
                    status = WorktreeStatus.CONFIGURED
                    currentAppUrl = appUrl
                } else {
                    status = WorktreeStatus.UNCONFIGURED
                    currentAppUrl = appUrl
                }
            }
        }

        project.messageBus.syncPublisher(STATUS_CHANGED).onStatusChanged(status)
    }

    companion object {
        val STATUS_CHANGED: Topic<StatusChangeListener> = Topic.create(
            "WorktreeEnv.StatusChanged",
            StatusChangeListener::class.java,
        )

        fun getInstance(project: Project): WorktreeEnvService {
            return project.getService(WorktreeEnvService::class.java)
        }
    }

    fun interface StatusChangeListener {
        fun onStatusChanged(status: WorktreeStatus)
    }
}
