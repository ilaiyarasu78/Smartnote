package com.ilaiyarasu.smartnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val category: String = Category.PERSONAL.label,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false,
    val reminderTime: Long? = null,
    val isPinned: Boolean = false
)