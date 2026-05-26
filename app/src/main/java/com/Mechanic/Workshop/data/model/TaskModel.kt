data class TaskModel(
    val id: String,
    val createDate: String,
    val title: String,
    val description: String,
    val creator: String,
    val status: String,
    val assignedTo: String = "",
    val responsible: String = "",
    val pendingInvites: String = "",
    val unit: String = "",
    val priority: String = "",
    // فیلدهای جدید
    val sub_unit: String = "",
    val declaration_method: String = "",
    val requester: String = "",
    val request_date: String = "",
    val urgency: String = "",
    val initial_review: String = "",
    val system_request_number: String = "",
    val referredBy: String = ""
)