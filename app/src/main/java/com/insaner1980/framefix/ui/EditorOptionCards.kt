package com.insaner1980.framefix.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.insaner1980.framefix.model.BackgroundType
import com.insaner1980.framefix.model.LayoutMode
import com.insaner1980.framefix.ui.theme.FrameFixColors

@Composable
fun SelectableOptionCard(
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FrameFixColors.Accent else FrameFixColors.Outline,
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) FrameFixColors.Accent else FrameFixColors.Outline.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.clickable { onClick() },
    ) {
        content()
    }
}

@Composable
fun LayoutModeCard(
    label: String,
    mode: LayoutMode,
    selectedMode: LayoutMode,
    imageTopLeftFraction: Offset,
    imageSizeFraction: Size,
    onSelect: (LayoutMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSelected = selectedMode == mode

    SelectableOptionCard(
        isSelected = isSelected,
        modifier = modifier,
        onClick = { onSelect(mode) },
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LayoutModePreview(
                isSelected = isSelected,
                imageTopLeftFraction = imageTopLeftFraction,
                imageSizeFraction = imageSizeFraction,
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label,
                color = if (isSelected) FrameFixColors.Background else FrameFixColors.OnDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun LayoutModePreview(isSelected: Boolean, imageTopLeftFraction: Offset, imageSizeFraction: Size) {
    Canvas(modifier = Modifier.size(24.dp)) {
        drawRoundRect(
            color = if (isSelected) FrameFixColors.Background.copy(alpha = 0.5f) else FrameFixColors.Muted,
            size = size,
            cornerRadius = CornerRadius(4f, 4f),
            style = Stroke(width = 1.5.dp.toPx()),
        )
        drawRoundRect(
            color = if (isSelected) FrameFixColors.Background else FrameFixColors.OnDark.copy(alpha = 0.6f),
            topLeft = Offset(
                x = size.width * imageTopLeftFraction.x,
                y = size.height * imageTopLeftFraction.y,
            ),
            size = Size(
                width = size.width * imageSizeFraction.width,
                height = size.height * imageSizeFraction.height,
            ),
            cornerRadius = CornerRadius(2f, 2f),
        )
    }
}

@Composable
fun BackgroundTypeCard(
    label: String,
    type: BackgroundType,
    selectedType: BackgroundType,
    onTypeSelect: (BackgroundType) -> Unit,
    modifier: Modifier = Modifier,
    leadingContent: (@Composable (Boolean) -> Unit)? = null,
) {
    val isSelected = selectedType == type

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) FrameFixColors.Accent else FrameFixColors.Background,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) FrameFixColors.Accent else FrameFixColors.Outline.copy(alpha = 0.3f),
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier.clickable { onTypeSelect(type) },
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leadingContent?.invoke(isSelected)
            if (leadingContent != null) {
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                color = if (isSelected) FrameFixColors.Background else FrameFixColors.OnDark,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SolidColorDot(color: Color, borderColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(color, CircleShape)
            .border(1.dp, borderColor, CircleShape),
    )
}

@Composable
fun PresetColorDot(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(10.dp)
            .background(
                brush = Brush.sweepGradient(listOf(Color.Red, Color.Green, Color.Blue, Color.Red)),
                shape = CircleShape,
            ),
    )
}
