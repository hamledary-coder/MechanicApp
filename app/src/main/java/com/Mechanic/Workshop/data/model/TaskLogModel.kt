package com.Mechanic.Workshop.data.model

data class TaskLogModel(
    val id: String,
    val taskId: String,
    val userId: String,              // ثبت‌کننده (rowId)
    val userName: String,            // اسم ثبت‌کننده
    val date: String,                // تاریخ گزارش (مثلاً ۱۴۰۴/۰۲/۲۵)
    val startTime: String,           // زمان شروع (اختیاری)
    val endTime: String,             // زمان پایان (اختیاری)
    val actionDescription: String,   // شرح اقدام انجام شده
    val consumedParts: String,       // قطعات مصرفی (متن ساده یا JSON)
    val assignedUsers: String,       // ids نیروی انسانی حاضر (با کاما جدا شده)
    val newStatus: String,           // وضعیت جدید کار بعد از این گزارش
    val attachments: String,         // لیست آدرس تصاویر (JSON یا با کاما جدا شده)
    val notes: String,               // توضیحات تکمیلی
    val canEditDelete: Boolean       // آیا کاربر فعلی مجاز به ویرایش/حذف این گزارش است؟
)