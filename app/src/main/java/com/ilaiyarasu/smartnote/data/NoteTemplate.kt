package com.ilaiyarasu.smartnote.data

data class NoteTemplate(
    val id: String,
    val label: String,
    val suggestedCategory: String,
    val titleSuggestion: String,
    val contentTemplate: String
)

val noteTemplates = listOf(
    NoteTemplate(
        id = "meeting",
        label = "Meeting Notes",
        suggestedCategory = Category.WORK.label,
        titleSuggestion = "Meeting - ",
        contentTemplate = """
            Date:
            Attendees:

            Agenda:
            -

            Discussion:
            -

            Action Items:
            -

            Next Steps:
            -
        """.trimIndent()
    ),
    NoteTemplate(
        id = "class_notes",
        label = "Class Notes",
        suggestedCategory = Category.STUDY.label,
        titleSuggestion = "Class - ",
        contentTemplate = """
            Subject:
            Topic:
            Date:

            Key Points:
            -

            Important Terms:
            -

            Questions to Review:
            -
        """.trimIndent()
    ),
    NoteTemplate(
        id = "todo",
        label = "To-Do List",
        suggestedCategory = Category.PERSONAL.label,
        titleSuggestion = "To-Do - ",
        contentTemplate = """
            Today's Tasks:
            ☐ 
            ☐ 
            ☐ 

            Priority:
            -
        """.trimIndent()
    ),
    NoteTemplate(
        id = "journal",
        label = "Journal Entry",
        suggestedCategory = Category.PERSONAL.label,
        titleSuggestion = "Journal - ",
        contentTemplate = """
            Date:
            Mood:

            Today I:
            -

            Grateful for:
            -
        """.trimIndent()
    ),
    NoteTemplate(
        id = "shopping",
        label = "Shopping List",
        suggestedCategory = Category.PERSONAL.label,
        titleSuggestion = "Shopping List",
        contentTemplate = """
            Groceries:
            ☐ 
            ☐ 
            ☐ 

            Other:
            ☐ 
        """.trimIndent()
    ),
    NoteTemplate(
        id = "exam_prep",
        label = "Exam Prep",
        suggestedCategory = Category.STUDY.label,
        titleSuggestion = "Exam Prep - ",
        contentTemplate = """
            Subject:
            Exam Date:

            Topics to Cover:
            -

            Formulas / Key Facts:
            -

            Practice Questions Done:
            -
        """.trimIndent()
    )
)
