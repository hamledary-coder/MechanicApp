package com.Mechanic.Workshop.data.model

data class TaskLogModel(
    val id: String,
    val taskId: String,
    val userId: String,
    val userName: String,
    val date: String,
    val startTime: String,
    val endTime: String,
    val actionDescription: String,
    val assignedUsers: String,
    val newStatus: String,
    val attachments: String,
    val notes: String,
    val duration: String = "",
    val comments: String = "[]"  // ← کاما حذف شد
)