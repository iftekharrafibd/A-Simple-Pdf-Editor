package com.iftekharrafi.asimplepdfeditor.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.iftekharrafi.asimplepdfeditor.presentation.editor.screen.PdfEditorScreen
import com.iftekharrafi.asimplepdfeditor.presentation.home.HomeScreen

@Composable
fun RootGraph(modifier: Modifier = Modifier) {

    val backStack = rememberNavBackStack(Screen.Home)

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {

            entry(Screen.Home) {
                HomeScreen(
                    onPdfSelected = { pdfUri ->
                        // ১. ইউজার আসলেও কোনো ফাইল সিলেক্ট করেছে কি না তা চেক করা
                        if (pdfUri != null) {
                            // ২. প্রোডাকশন সেফটি: URI-কে String এ কনভার্ট করে Encode করে পাঠানো
                            val encodedUri = Uri.encode(pdfUri.toString())
                            backStack.add(Screen.PdfEditor(encodedUri))
                        }
                    }
                )
            }

            entry<Screen.PdfEditor> { key ->
                val passedUriString = key.uri // তোমার ডেটা ক্লাসের প্রোপার্টি নাম 'uri' ধরে নিলাম

                // ৩. রিসিভ করার পর ডিকোড করে আবার আসল URI তে কনভার্ট করা
                val decodedUri = passedUriString?.let { Uri.decode(it) }?.toUri()

                PdfEditorScreen(
                    pdfUri = decodedUri,
                    onBackClick = { backStack.removeLastOrNull() }
                )
            }

        }
    )
}