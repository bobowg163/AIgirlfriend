package com.example.aigirlfriend.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigirlfriend.ui.theme.Pink500
import com.example.aigirlfriend.ui.theme.SecondaryText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentPickerDialog(
    onTakePhoto: () -> Unit,
    onSelectPhoto: () -> Unit,
    onSelectVideo: () -> Unit,
    onSelectFile: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "添加附件",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PickerMenuItem(
                    icon = "📷",
                    label = "拍照",
                    onClick = { onTakePhoto(); onDismiss() }
                )
                PickerMenuItem(
                    icon = "🖼️",
                    label = "从相册选择",
                    onClick = { onSelectPhoto(); onDismiss() }
                )
                PickerMenuItem(
                    icon = "🎬",
                    label = "选择视频",
                    onClick = { onSelectVideo(); onDismiss() }
                )
                PickerMenuItem(
                    icon = "📄",
                    label = "选择文件",
                    onClick = { onSelectFile(); onDismiss() }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = SecondaryText)
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
private fun PickerMenuItem(icon: String, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Text(
            label,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
