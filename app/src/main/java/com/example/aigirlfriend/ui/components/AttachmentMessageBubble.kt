package com.example.aigirlfriend.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.aigirlfriend.data.model.ChatHistory
import com.example.aigirlfriend.ui.theme.MessageAi
import com.example.aigirlfriend.ui.theme.Pink500
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun AttachmentMessageBubble(message: ChatHistory) {
    val uriString = message.attachmentUri ?: return
    val mimeType = guessMimeType(uriString)
    val type = when {
        mimeType?.startsWith("image/") == true -> "image"
        mimeType?.startsWith("video/") == true -> "video"
        else -> "file"
    }
    val fileName = extractFileName(uriString)
    val fileSize = if (type == "file") getFileSizeString(uriString) else null

    if (type == "image") {
        ImageAttachmentBubble(uri = uriString)
    } else if (type == "video") {
        VideoAttachmentBubble(uri = uriString)
    } else {
        FileAttachmentBubble(
            fileName = fileName ?: "附件",
            fileSize = fileSize,
            mimeType = mimeType
        )
    }
}

@Composable
fun ImageAttachmentBubble(uri: String) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val loadError = remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        loadError.value = false
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                loadError.value = true
            }
        }
    }

    Surface(
        modifier = Modifier
            .widthIn(max = 200.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp))
            .clickable {},
        color = MessageAi,
        shape = RoundedCornerShape(12.dp)
    ) {
        if (!loadError.value && bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "图片",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3D3D5C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Photo,
                    contentDescription = null,
                    tint = Pink500.copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun VideoAttachmentBubble(uri: String) {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    val loadError = remember(uri) { mutableStateOf(false) }

    LaunchedEffect(uri) {
        loadError.value = false
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(Uri.parse(uri))
                bitmap = inputStream?.use { BitmapFactory.decodeStream(it) }
            } catch (_: Exception) {
                loadError.value = true
            }
        }
    }

    Box(
        modifier = Modifier
            .width(200.dp)
            .height(150.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!loadError.value && bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "视频",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .width(200.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF3D3D5C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF1E88E5).copy(alpha = 0.5f),
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "播放",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
fun FileAttachmentBubble(fileName: String, fileSize: String?, mimeType: String?) {
    val icon = when {
        mimeType?.contains("pdf") == true -> Icons.Default.Description
        mimeType?.contains("image") == true -> Icons.Default.Photo
        mimeType?.contains("video") == true -> Icons.Default.Movie
        mimeType?.contains("audio") == true -> Icons.Default.AudioFile
        else -> Icons.Default.InsertDriveFile
    }

    Surface(
        modifier = Modifier
            .widthIn(max = 260.dp)
            .shadow(2.dp, RoundedCornerShape(12.dp)),
        color = MessageAi,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Pink500,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = fileName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (fileSize != null) {
                    Text(
                        text = fileSize,
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

private fun guessMimeType(uriString: String): String? {
    val ext = uriString.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "jpg", "jpeg", "png", "gif", "webp", "bmp" -> "image/${ext.ifEmpty { "jpeg" }}"
        "mp4", "mov", "avi", "mkv", "webm" -> "video/${ext}"
        "pdf" -> "application/pdf"
        "doc", "docx" -> "application/msword"
        "xls", "xlsx" -> "application/vnd.ms-excel"
        "txt" -> "text/plain"
        "zip", "rar" -> "application/zip"
        else -> null
    }
}

private fun extractFileName(uriString: String): String? {
    return uriString.substringAfterLast('/')
        .substringBefore('?')
        .takeIf { it.isNotEmpty() }
}

private fun getFileSizeString(uriString: String): String? {
    return try {
        val file = File(uriString)
        if (file.exists()) {
            val bytes = file.length()
            when {
                bytes >= 1024 * 1024 -> "%.1f MB".format(bytes.toFloat() / (1024 * 1024))
                bytes >= 1024 -> "%.1f KB".format(bytes.toFloat() / 1024)
                else -> "$bytes B"
            }
        } else null
    } catch (_: Exception) {
        null
    }
}
