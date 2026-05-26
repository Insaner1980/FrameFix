package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HomeScreen(
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1C1B1F)) // Clean Minimalism Dark Charcoal Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Creative Tool-Like Geometric Icon drawing via Canvas to look beautiful without heavy resources
            GeometricArt(
                modifier = Modifier
                    .size(160.dp)
                    .testTag("geometric_graphic_logo")
            )

            Spacer(modifier = Modifier.height(32.dp))

            // App Name FrameFix in crisp luxury minimalism display typography
            Text(
                text = "FrameFix",
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE6E1E5),
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("app_title")
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle explanation
            Text(
                text = "State-of-the-art aspect ratio framing tool. Reshape, zoom, and pan images without distortion.",
                fontSize = 14.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF938F99), // Clean Minimalism Muted Grey
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .padding(horizontal = 16.dp)
                    .testTag("app_subtitle")
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Choose Image custom CTA Button
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        androidx.activity.result.PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )
                    )
                },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4D96FF), // Clean Minimalism Sky Blue Accent
                    contentColor = Color(0xFF1C1B1F)  // Contrast Dark Charcoal
                ),
                modifier = Modifier
                    .widthIn(min = 220.dp)
                    .height(56.dp)
                    .testTag("choose_image_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.AddPhotoAlternate,
                    contentDescription = "Image Picker Icon",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(1.dp))
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(
                    text = "Choose Image",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun GeometricArt(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        // Outer ratio box drawing (representing 16:9 canvas frame)
        drawRoundRect(
            color = Color(0xFF2B2930),
            topLeft = Offset(width * 0.05f, height * 0.15f),
            size = Size(width * 0.9f, height * 0.7f),
            cornerRadius = CornerRadius(16f, 16f)
        )

        // Draw outer lavender frame contour
        drawRoundRect(
            color = Color(0xFF4D96FF),
            topLeft = Offset(width * 0.05f, height * 0.15f),
            size = Size(width * 0.9f, height * 0.7f),
            cornerRadius = CornerRadius(16f, 16f),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw inner cropped boundary representing 1:1 format overlay
        drawRoundRect(
            color = Color(0xFF938F99),
            topLeft = Offset(width * 0.25f, height * 0.15f),
            size = Size(width * 0.5f, height * 0.7f),
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(15f, 10f), 0f
                )
            )
        )

        // Draw decorative abstract center composition
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF4D96FF).copy(alpha = 0.4f), Color.Transparent),
                center = Offset(width * 0.5f, height * 0.5f),
                radius = width * 0.2f
            ),
            center = Offset(width * 0.5f, height * 0.5f),
            radius = width * 0.2f
        )
    }
}
