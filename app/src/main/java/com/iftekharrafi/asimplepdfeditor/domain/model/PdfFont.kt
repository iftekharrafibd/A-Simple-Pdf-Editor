package com.iftekharrafi.asimplepdfeditor.domain.model

/**
 * Available font styles for PDF text overlays.
 * Lives in the domain layer so both presentation and data layers can reference it.
 */
enum class PdfFont(val displayName: String) {
    DEFAULT("Default"),
    SERIF("Serif"),
    CURSIVE("Cursive"),
    KALPANA("Kalpana"),
}
