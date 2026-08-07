package com.ilaiyarasu.smartnote.data

enum class Category(val label: String) {
    STUDY("Study"),
    WORK("Work"),
    PERSONAL("Personal");

    companion object {
        fun fromLabel(label: String): Category =
            values().find { it.label == label } ?: PERSONAL

        fun labels(): List<String> = values().map { it.label }
    }
}