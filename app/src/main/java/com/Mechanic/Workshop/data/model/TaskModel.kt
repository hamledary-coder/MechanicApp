data class TaskModel(
    val id: String,
    val createDate: String,
    val title: String,
    val description: String,
    val creator: String,
    val status: String,
    val assignedTo: String = "", // 🔴 اضافه کن
    val responsible: String = "",
    val pendingInvites: String = "",
    val unit: String = "",        // واحد مربوطه (کد)
    val priority: String = ""     // درجه اهمیت (کد)
)