package com.xayah.core.service.packages.restore

import com.xayah.core.data.repository.PackageRepository
import com.xayah.core.data.repository.TaskRepository
import com.xayah.core.database.dao.PackageDao
import com.xayah.core.database.dao.TaskDao
import com.xayah.core.model.DataType
import com.xayah.core.model.OpType
import com.xayah.core.model.OperationState
import com.xayah.core.model.TaskType
import com.xayah.core.model.database.TaskDetailPackageEntity
import com.xayah.core.model.database.TaskEntity
import com.xayah.core.rootservice.service.RemoteRootService
import com.xayah.core.service.util.PackagesRestoreUtil
import com.xayah.core.util.PathUtil
import com.xayah.core.util.localBackupSaveDir
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class RestoreServiceLocalImpl @Inject constructor() : RestoreService() {
    @Inject
    override lateinit var rootService: RemoteRootService

    @Inject
    override lateinit var pathUtil: PathUtil

    @Inject
    override lateinit var taskDao: TaskDao

    @Inject
    override lateinit var packageDao: PackageDao

    @Inject
    override lateinit var packagesRestoreUtil: PackagesRestoreUtil

    @Inject
    override lateinit var taskRepository: TaskRepository

    @Inject
    override lateinit var packageRepository: PackageRepository

    override val taskEntity by lazy {
        TaskEntity(
            id = 0,
            opType = OpType.RESTORE,
            taskType = TaskType.PACKAGE,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            backupDir = context.localBackupSaveDir(),
            isProcessing = true,
        )
    }

    override suspend fun createTargetDirs() {}

    private val appsDir by lazy { pathUtil.getLocalBackupAppsDir() }

    override suspend fun restorePackage(t: TaskDetailPackageEntity) {
        t.apply {
            state = OperationState.PROCESSING
            taskDao.upsert(this)
        }

        val p = t.packageEntity
        val srcDir = "${appsDir}/${p.archivesRelativeDir}"
        val userId = if (restoreUser == -1) p.userId else restoreUser

        packagesRestoreUtil.restoreApk(userId = userId, p = p, t = t, srcDir = srcDir)
        packagesRestoreUtil.restoreData(userId = userId, p = p, t = t, dataType = DataType.PACKAGE_USER, srcDir = srcDir)
        packagesRestoreUtil.restoreData(userId = userId, p = p, t = t, dataType = DataType.PACKAGE_USER_DE, srcDir = srcDir)
        packagesRestoreUtil.restoreData(userId = userId, p = p, t = t, dataType = DataType.PACKAGE_DATA, srcDir = srcDir)
        packagesRestoreUtil.restoreData(userId = userId, p = p, t = t, dataType = DataType.PACKAGE_OBB, srcDir = srcDir)
        packagesRestoreUtil.restoreData(userId = userId, p = p, t = t, dataType = DataType.PACKAGE_MEDIA, srcDir = srcDir)
        packagesRestoreUtil.restorePermissions(userId = userId, p = p)
        packagesRestoreUtil.restoreSsaid(userId = userId, p = p)

        t.apply {
            t.apply {
                state = if (isSuccess) OperationState.DONE else OperationState.ERROR
                taskDao.upsert(this)
            }
            packageEntity = p
            taskDao.upsert(this)
        }
        taskEntity.also {
            if (t.isSuccess) it.successCount++ else it.failureCount++
            taskDao.upsert(it)
        }
    }

    override suspend fun clear() {}
}
