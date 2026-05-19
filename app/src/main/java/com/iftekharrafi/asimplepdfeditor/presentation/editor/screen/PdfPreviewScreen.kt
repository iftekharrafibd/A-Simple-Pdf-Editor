package com.iftekharrafi.asimplepdfeditor.presentation.editor.screen

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface
import com.iftekharrafi.asimplepdfeditor.utils.getFileName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPreviewScreen(
    pdfUri: Uri,
    onBackClick: () -> Unit,
    onEditPage: (Int) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pageCount by remember { mutableIntStateOf(0) }
    var selectedPageIndex by remember { mutableStateOf<Int?>(null) }
    val fileName = remember(pdfUri) { pdfUri.getFileName(context) }
    val isEdited = remember(fileName) { fileName.startsWith("Edited_") }

    val shareCurrentPdf = {
        try {
            val file = if (pdfUri.scheme == "file") {
                java.io.File(pdfUri.path!!)
            } else {
                java.io.File(context.filesDir, fileName)
            }
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "File does not exist!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val authority = "${context.packageName}.fileprovider"
                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)

                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error sharing: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    val exportCurrentPdf = {
        try {
            val file = if (pdfUri.scheme == "file") {
                java.io.File(pdfUri.path!!)
            } else {
                java.io.File(context.filesDir, fileName)
            }
            if (!file.exists()) {
                android.widget.Toast.makeText(context, "File does not exist!", android.widget.Toast.LENGTH_SHORT).show()
            } else {
                val resolver = context.contentResolver
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS + "/AmarPDF")
                }

                val targetUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (targetUri != null) {
                    resolver.openOutputStream(targetUri)?.use { outputStream ->
                        file.inputStream().use { inputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                    android.widget.Toast.makeText(context, "Exported successfully to Downloads/AmarPDF", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to export PDF", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "Error exporting: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // On-demand bitmap cache for preview pages
    val renderedBitmaps = remember { mutableStateMapOf<Int, Bitmap>() }
    var rendererRef by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptorRef by remember { mutableStateOf<ParcelFileDescriptor?>(null) }

    // Lifecycle observer to increment refreshTrigger whenever this screen resumes (e.g., when returning from the editor)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Initialize/Reload native PdfRenderer keyed on both pdfUri and refreshTrigger
    LaunchedEffect(pdfUri, refreshTrigger) {
        if (refreshTrigger == 0) return@LaunchedEffect
        
        withContext(Dispatchers.IO) {
            try {
                // Safely close any previously opened descriptors
                try {
                    rendererRef?.close()
                    fileDescriptorRef?.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val fileDescriptor = context.contentResolver.openFileDescriptor(pdfUri, "r")
                if (fileDescriptor != null) {
                    fileDescriptorRef = fileDescriptor
                    val renderer = PdfRenderer(fileDescriptor)
                    rendererRef = renderer
                    pageCount = renderer.pageCount

                    // Clear preview bitmaps cache to force re-render with new edits
                    withContext(Dispatchers.Main) {
                        renderedBitmaps.clear()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Clean up native resources on dispose
    DisposableEffect(Unit) {
        onDispose {
            try {
                rendererRef?.close()
                fileDescriptorRef?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            renderedBitmaps.values.forEach { it.recycle() }
            renderedBitmaps.clear()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = fileName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    if (isEdited) {
                        IconButton(onClick = { shareCurrentPdf() }) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Share",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = { exportCurrentPdf() }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Export",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = EditorSurface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedPageIndex != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
            ) {
                FloatingActionButton(
                    onClick = { selectedPageIndex?.let { onEditPage(it) } },
                    containerColor = AccentBlue,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Edit Page ${(selectedPageIndex ?: 0) + 1}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
        containerColor = EditorBackground
    ) { innerPadding ->
        if (pageCount > 0) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
            ) {
                items(pageCount) { pageIndex ->
                    val isSelected = selectedPageIndex == pageIndex
                    val pageBitmap = renderedBitmaps[pageIndex]

                    // Render page on-demand when visible
                    LaunchedEffect(pageIndex, rendererRef) {
                        val renderer = rendererRef ?: return@LaunchedEffect
                        if (renderedBitmaps.containsKey(pageIndex)) return@LaunchedEffect

                        withContext(Dispatchers.IO) {
                            try {
                                synchronized(renderer) {
                                    val page = renderer.openPage(pageIndex)
                                    // Scale down for preview display to conserve memory
                                    val previewWidth = (page.width * 1.5f).toInt()
                                    val previewHeight = (page.height * 1.5f).toInt()

                                    val bitmap = createBitmap(previewWidth, previewHeight)
                                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                    page.close()

                                    renderedBitmaps[pageIndex] = bitmap
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "PAGE ${pageIndex + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) AccentBlue else Color.White.copy(alpha = 0.4f),
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        val activeBorder = if (isSelected) {
                            Modifier.border(3.dp, AccentBlue, RoundedCornerShape(12.dp))
                        } else Modifier

                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.92f)
                                .then(activeBorder)
                                .shadow(8.dp, RoundedCornerShape(12.dp))
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .clickable {
                                    selectedPageIndex = if (isSelected) null else pageIndex
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (pageBitmap != null) {
                                Image(
                                    bitmap = pageBitmap.asImageBitmap(),
                                    contentDescription = "Page ${pageIndex + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(pageBitmap.width.toFloat() / pageBitmap.height.toFloat())
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .aspectRatio(0.707f) // Standard vertical page
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AccentBlue,
                                        modifier = Modifier.size(36.dp),
                                        strokeWidth = 3.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = AccentBlue)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Loading PDF Preview...",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
