package com.iftekharrafi.asimplepdfeditor.presentation.home


import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Draw
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.iftekharrafi.asimplepdfeditor.data.local.entity.PdfEntity
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentBlue
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentCyan
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPink
import com.iftekharrafi.asimplepdfeditor.ui.theme.AccentPurple
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorBackground
import com.iftekharrafi.asimplepdfeditor.ui.theme.EditorSurface
import com.iftekharrafi.asimplepdfeditor.ui.theme.GradientEnd
import com.iftekharrafi.asimplepdfeditor.ui.theme.GradientStart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.platform.LocalLocale
import androidx.core.net.toUri


@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onPdfSelected: (Uri?) -> Unit
) {
    val context = LocalContext.current
    val recentFiles by viewModel.recentPdfs.collectAsState()

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // ১. ডেটাবেসে সেভ করা হচ্ছে
            viewModel.addRecentFile(it, context)
            // ২. এডিটর স্ক্রিনে পাঠানো হচ্ছে
            onPdfSelected(it)
        }
    }

    // Animated gradient angle
    val infiniteTransition = rememberInfiniteTransition(label = "gradient")
    val animatedAngle = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EditorBackground)
    ) {
        // Animated background orbs
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val centerX = size.width / 2
            val centerY = size.height / 3
            val angle = Math.toRadians(animatedAngle.value.toDouble())

            // Blue orb
            drawCircle(
                color = AccentBlue.copy(alpha = 0.08f),
                radius = size.width * 0.4f,
                center = Offset(
                    centerX + (kotlin.math.cos(angle) * 80).toFloat(),
                    centerY + (kotlin.math.sin(angle) * 60).toFloat()
                )
            )

            // Purple orb
            drawCircle(
                color = AccentPurple.copy(alpha = 0.06f),
                radius = size.width * 0.35f,
                center = Offset(
                    centerX - (kotlin.math.cos(angle + 2) * 100).toFloat(),
                    centerY + (kotlin.math.sin(angle + 2) * 80).toFloat()
                )
            )

            // Cyan orb
            drawCircle(
                color = AccentCyan.copy(alpha = 0.05f),
                radius = size.width * 0.3f,
                center = Offset(
                    centerX + (kotlin.math.sin(angle) * 120).toFloat(),
                    centerY * 2 + (kotlin.math.cos(angle) * 40).toFloat()
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App icon
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(GradientStart, GradientEnd)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Description,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App title
            Text(
                text = "Amar PDF",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "PDF Editor",
                style = MaterialTheme.typography.titleMedium,
                color = AccentBlue,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Add text, draw, annotate, and save\nyour PDFs with professional tools",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Import PDF Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                GradientStart.copy(alpha = 0.15f),
                                GradientEnd.copy(alpha = 0.15f)
                            )
                        )
                    )
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GradientStart.copy(alpha = 0.4f),
                                GradientEnd.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        pdfLauncher.launch(arrayOf("application/pdf"))
                    }
                    .padding(vertical = 32.dp, horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(GradientStart, GradientEnd)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.UploadFile,
                            contentDescription = "Import PDF",
                            modifier = Modifier.size(30.dp),
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Import PDF",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Tap to select a PDF file from your device",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
// --- নতুন: Recent Files Section ---
            if (recentFiles.isNotEmpty()) {
                Text(
                    text = "RECENT FILES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 3.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // রিসেন্ট ফাইলের লিস্ট
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth().weight(1f) // স্ক্রল করার জন্য
                ) {
                    items(recentFiles) { pdf ->
                        RecentFileItem(
                            pdf = pdf,
                            onClick = {
                                // ডেটাবেস থেকে URI নিয়ে আবার ডেটাবেস আপডেট করে ওপেন করা
                                val uri = pdf.fileUri.toUri()
                                viewModel.addRecentFile(uri, context) // টাইম আপডেট করার জন্য
                                onPdfSelected(uri)
                            }
                        )
                    }
                }
            } else {
                // Feature highlights
                Text(
                    text = "FEATURES",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.3f),
                    letterSpacing = 3.sp,
                    modifier = Modifier.align(Alignment.Start)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FeatureRow(
                    icon = Icons.Default.TextFields,
                    title = "Add Text",
                    description = "Custom fonts, sizes & colors",
                    color = AccentBlue
                )
                Spacer(modifier = Modifier.height(12.dp))

                FeatureRow(
                    icon = Icons.Default.Draw,
                    title = "Draw & Annotate",
                    description = "Freehand drawing with color options",
                    color = AccentPurple
                )
                Spacer(modifier = Modifier.height(12.dp))

                FeatureRow(
                    icon = Icons.Default.Edit,
                    title = "Professional Editing",
                    description = "Zoom, pan & precise controls",
                    color = AccentCyan
                )
                Spacer(modifier = Modifier.height(12.dp))

                FeatureRow(
                    icon = Icons.Default.SaveAlt,
                    title = "Export as PDF",
                    description = "Save edits as a new PDF file",
                    color = AccentPink
                )
            }
        }
    }
}

@Composable
private fun RecentFileItem(
    pdf: PdfEntity,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorSurface.copy(alpha = 0.6f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(AccentPink.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.PictureAsPdf,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = AccentPink
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pdf.fileName,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                maxLines = 1
            )

            // টাইমস্ট্যাম্প থেকে সুন্দর ডেট ফরম্যাট
            val date = SimpleDateFormat("dd MMM yyyy, hh:mm a", LocalLocale.current.platformLocale).format(Date(pdf.lastOpened))
            Text(
                text = "Opened: $date",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    description: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(EditorSurface.copy(alpha = 0.6f))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = color
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.4f)
            )
        }
    }
}
