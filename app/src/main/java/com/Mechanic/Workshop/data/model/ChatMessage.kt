package com.Mechanic.Workshop.data.model

data class ChatMessage(
    val id: Int,
    val sender_id: Int,
    val sender_name: String,
    val message: String,
    val created_at: String
)