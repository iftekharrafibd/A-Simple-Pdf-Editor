package com.iftekharrafi.asimplepdfeditor.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Screen: NavKey {
    @Serializable
    data object Home: Screen
    @Serializable
    data class PdfPreview(val uri: String): Screen

    @Serializable
    data class PdfEditor(val uri: String, val initialPageIndex: Int = 0): Screen
}