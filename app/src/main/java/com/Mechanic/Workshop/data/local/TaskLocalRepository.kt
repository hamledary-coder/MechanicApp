package com.Mechanic.Workshop.data.local

import TaskModel
import android.content.Context
import com.Mechanic.Workshop.data.remote.Config
import com.Mechanic.Workshop.data.sync.TaskMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * لایه‌ی بین WorkListFragment و Room
 * فقط خوندن — نوشتن رو TaskSyncer انجام می‌ده
 */
class TaskLocalRepository(private val context: Context) {

    private val db = AppDatabase.getInstance(context)

    /**
     * خروجی برای تب‌های مختلف کارتابل
     */
    fun getTasksForTab(
        tabName: String,
        userId: String,
        userRole: String
    ): Flow<List<TaskModel>> {

        val flow = when (tabName.trim()) {
            "اقدام نشده" -> db.taskDao().getUnassigned()

            "در حال انجام" -> db.taskDao().getInProgress()

            "کارتابل من" -> {
                when (userRole) {
                    Config.RoleCode.EMPLOYEE ->
                        db.taskDao().getMyCartableForEmployee(userId)
                    Config.RoleCode.SUPERVISOR,
                    Config.RoleCode.MANAGER ->
                        db.taskDao().getMyCartableForSupervisor(userId)
                    else ->
                        db.taskDao().getMyCartableForEmployee(userId)
                }
            }

            else -> db.taskDao().getUnassigned()
        }

        // تبدیل entity به model
        return flow.map { entities ->
            entities.map { TaskMapper.toModel(it) }
        }
    }

    /**
     * خروجی Flow برای یه کار خاص (برای TaskDetailActivity)
     */
    fun getTaskById(taskId: String): Flow<TaskModel?> {
        return db.taskDao().getByIdFlow(taskId).map { entity ->
            entity?.let { TaskMapper.toModel(it) }
        }
    }

    /**
     * برای HomeActivity — چک می‌کنه کاربر مسئول کاری هست یا نه
     */
    fun getResponsibleTasks(userId: String): Flow<List<TaskModel>> {
        return db.taskDao().getResponsibleTasks(userId).map { entities ->
            entities.map { TaskMapper.toModel(it) }
        }
    }

    suspend fun updateTaskStatus(taskId: String, newStatus: String) {
        val entity = db.taskDao().getById(taskId) ?: return
        db.taskDao().update(entity.copy(status = newStatus))
    }

    suspend fun deleteTaskLocally(taskId: String) {
        // ۱. task رو isDeleted = true
        val task = db.taskDao().getById(taskId) ?: return
        db.taskDao().update(task.copy(isDeleted = true))

        // ۲. گزارش‌های این کار رو پیدا کن
        val reports = db.reportDao().getByTaskOnce(taskId)
        val reportIds = reports.map { it.id }

        // ۳. گزارش‌ها رو پاک کن
        db.reportDao().deleteAllByTaskId(taskId)

        // ۴. Outbox ها رو پاک کن
        db.outboxDao().deleteByTargetId(taskId)   // addTaskLog, updateTaskLog, deleteTaskLog, updateTaskStatus
        if (reportIds.isNotEmpty()) {
            db.outboxDao().deleteByTargetIds(reportIds)   // updateEvaluation
        }
    }

    suspend fun updateTaskFromEdit(updatedTask: TaskModel) {
        val existing = db.taskDao().getById(updatedTask.id) ?: return
        db.taskDao().update(
            existing.copy(
                title = updatedTask.title,
                description = updatedTask.description,
                unit = updatedTask.unit,
                subUnit = updatedTask.sub_unit,
                declarationMethod = updatedTask.declaration_method,
                requester = updatedTask.requester,
                requestDate = updatedTask.request_date,
                urgency = updatedTask.urgency,
                initialReview = updatedTask.initial_review,
                systemRequestNumber = updatedTask.system_request_number
            )
        )
    }

    fun getArchivedTasks(): Flow<List<TaskModel>> {
        return db.taskDao().getArchived().map { entities ->
            entities.map { TaskMapper.toModel(it) }
        }
    }
}