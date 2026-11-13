package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.StudioFileType
import com.example.domain.CodeSyntaxVisualTransformation
import com.example.ui.theme.EditorGutterBg
import com.example.ui.theme.EditorGutterText
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.StudioBgObsidian
import com.example.ui.theme.StudioBluePrimary
import com.example.ui.theme.StudioBorderSubtle
import com.example.ui.theme.StudioSurfaceCard
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.StudioTextPrimary
import com.example.ui.theme.StudioTextSecondary

@Composable
fun StudioSectionHeader(
    title: String,
    subtitle: String? = null,
    badge: String? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = StudioTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary
                )
            }
        }
        if (!badge.isNullOrBlank()) {
            StatusPill(text = badge, color = StudioBluePrimary)
        }
    }
}

@Composable
fun StatusPill(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun GamingActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = StudioSurfaceElevated),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = 0.14f),
                            StudioSurfaceCard.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentColor.copy(alpha = 0.2f))
                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = StudioTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SyntaxCodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    fileType: StudioFileType,
    fontSizeSp: Int,
    highlightLine: Int? = null,
    readOnly: Boolean = false,
    testTag: String = "code_editor_input",
    modifier: Modifier = Modifier
) {
    val lines = remember(code) { code.split('\n') }
    val lineCount = maxOf(lines.size, 1)
    val gutterWidthDp = remember(lineCount) {
        val digits = lineCount.toString().length.coerceAtLeast(2)
        (digits * 10 + 18).dp
    }
    val verticalScroll = rememberScrollState()
    val horizontalScroll = rememberScrollState()
    val visualTransformation = remember(fileType) { CodeSyntaxVisualTransformation(fileType) }

    val lineHeight = (fontSizeSp * 1.45f).sp
    val codeTextStyle = TextStyle(
        fontFamily = JetBrainsMonoFamily,
        fontSize = fontSizeSp.sp,
        lineHeight = lineHeight,
        color = StudioTextPrimary
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(StudioBgObsidian)
            .border(1.dp, StudioBorderSubtle, RoundedCornerShape(12.dp))
            .verticalScroll(verticalScroll)
    ) {
        // Line numbers gutter
        Column(
            modifier = Modifier
                .width(gutterWidthDp)
                .background(EditorGutterBg)
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.End
        ) {
            for (lineNum in 1..lineCount) {
                val isErrorLine = highlightLine == lineNum
                Text(
                    text = lineNum.toString(),
                    style = TextStyle(
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = fontSizeSp.sp,
                        lineHeight = lineHeight,
                        fontWeight = if (isErrorLine) FontWeight.Bold else FontWeight.Normal,
                        color = if (isErrorLine) Color(0xFFFF5252) else EditorGutterText
                    )
                )
            }
        }

        // Editable syntax-highlighted code pane
        Box(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(horizontalScroll)
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            BasicTextField(
                value = code,
                onValueChange = onCodeChange,
                readOnly = readOnly,
                textStyle = codeTextStyle,
                cursorBrush = SolidColor(StudioBluePrimary),
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 320.dp)
                    .testTag(testTag)
            )
            if (code.isEmpty()) {
                Text(
                    text = "// Escribe o pega tu código aquí...",
                    style = codeTextStyle.copy(color = EditorGutterText)
                )
            }
        }
    }
}
