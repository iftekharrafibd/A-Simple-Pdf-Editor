package com.iftekharrafi.asimplepdfeditor.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.core.net.toUri
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.iftekharrafi.asimplepdfeditor.presentation.editor.screen.PdfEditorScreen
import com.iftekharrafi.asimplepdfeditor.presentation.editor.screen.PdfPreviewScreen
import com.iftekharrafi.asimplepdfeditor.presentation.home.HomeScreen

@Composable
fun RootGraph() {

    val backStack = rememberNavBackStack(Screen.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry(Screen.Home) {
                HomeScreen(
                    onPdfSelected = { pdfUri ->
                        if (pdfUri != null) {
                            val encodedUri = Uri.encode(pdfUri.toString())
                            backStack.add(Screen.PdfPreview(encodedUri))
                        }
                    }
                )
            }

            entry<Screen.PdfPreview> { key ->
                val decodedUri = Uri.decode(key.uri).toUri()
                PdfPreviewScreen(
                    pdfUri = decodedUri,
                    onBackClick = { backStack.removeLastOrNull() },
                    onEditPage = { pageIndex ->
                        backStack.add(Screen.PdfEditor(key.uri, pageIndex))
                    }
                )
            }

            entry<Screen.PdfEditor> { key ->
                val decodedUri = Uri.decode(key.uri).toUri()

                PdfEditorScreen(
                    pdfUri = decodedUri,
                    initialPageIndex = key.initialPageIndex,
                    onSaveComplete = { savedUri ->
                        backStack.removeLastOrNull() // Pops PdfEditor
                        backStack.removeLastOrNull() // Pops old PdfPreview
                        val encodedSavedUri = Uri.encode(savedUri.toString())
                        backStack.add(Screen.PdfPreview(encodedSavedUri)) // Adds updated PdfPreview
                    },
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }

        }
    )
}