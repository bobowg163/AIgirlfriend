package com.example.aigirlfriend.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigirlfriend.R
import com.example.aigirlfriend.data.model.ChatHistory
import com.example.aigirlfriend.ui.components.AttachmentMessageBubble
import com.example.aigirlfriend.ui.components.AttachmentPickerDialog
import com.example.aigirlfriend.viewmodel.Attachment
import com.example.aigirlfriend.viewmodel.ChatViewModel
import com.example.aigirlfriend.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel) {
    val state = viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var showClearChatConfirm by remember { mutableStateOf(false) }
    var showPicker by remember { mutableStateOf(false) }
    var pendingAttachment by remember { mutableStateOf<Attachment?>(null) }
    var apiKeyInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var isLongPressInProgress by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    LaunchedEffect(state.value.animationState) {
        if (state.value.animationState !is AnimationState.Listening) {
            isLongPressInProgress = false
        }
    }

    LaunchedEffect(state.value.messages.size) {
        if (state.value.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.value.messages.size - 1)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.getLocationPermission()
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.getLocationPermission()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.setPermissionGranted(isGranted)
        if (isGranted) {
            viewModel.startListening()
        } else {
            viewModel.setAnimationState(AnimationState.Idle)
            viewModel.setSpeaking(false)
            viewModel.clearError()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            pendingAttachment = pendingAttachment?.copy(type = "image")
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingAttachment = Attachment(uri = it, type = "image", fileName = it.lastPathSegment) }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingAttachment = Attachment(uri = it, type = "video", fileName = it.lastPathSegment) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { pendingAttachment = Attachment(uri = it, type = "file", fileName = it.lastPathSegment) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val tempUri = android.provider.MediaStore.Images.Media.insertImage(
                ctx.contentResolver,
                android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888),
                "temp", null
            )
            if (tempUri != null) {
                val uri = android.net.Uri.parse(tempUri)
                pendingAttachment = Attachment(uri = uri, type = "image", fileName = "temp.jpg")
                cameraLauncher.launch(uri)
            }
        } else {
            android.widget.Toast.makeText(ctx, "无法使用相机，请检查权限设置", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    if (showApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showApiKeyDialog = false },
            title = {
                Text(
                    "⚙️ 设置 API Key",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
            },
            text = {
                Column {
                    Text(
                        "当前使用内置 API Key。如需自定义，请输入新的 Key：",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKeyInput,
                        onValueChange = { apiKeyInput = it },
                        placeholder = { Text("输入 ModelScope API Key", color = Color(0xFFCCCCCC)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Pink500,
                            unfocusedBorderColor = Pink300
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "🔒 API Key 仅保存在本地设备，不会上传到任何服务器。",
                        color = SecondaryText.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        TextButton(
                            onClick = { showClearChatConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
                        ) {
                            Text("清除聊天记录", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setApiKey(apiKeyInput.trim())
                        showApiKeyDialog = false
                        inputText = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showApiKeyDialog = false }) {
                    Text("取消", color = SecondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showClearChatConfirm) {
        AlertDialog(
            onDismissRequest = { showClearChatConfirm = false },
            title = {
                Text(
                    "⚠️ 清除聊天记录",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
            },
            text = {
                Text(
                    "确认要清除所有聊天记录吗？此操作不可恢复。",
                    color = SecondaryText,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearChat()
                        showClearChatConfirm = false
                        showApiKeyDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("确认清除", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearChatConfirm = false }) {
                    Text("取消", color = SecondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (showPicker) {
        AttachmentPickerDialog(
            onTakePhoto = {
                if (androidx.core.content.ContextCompat.checkSelfPermission(
                        ctx, android.Manifest.permission.CAMERA
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    val uriStr = android.provider.MediaStore.Images.Media.insertImage(
                        ctx.contentResolver,
                        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888),
                        "temp", null
                    )
                    if (uriStr != null) {
                        val uri = android.net.Uri.parse(uriStr)
                        pendingAttachment = Attachment(uri = uri, type = "image", fileName = "temp.jpg")
                        showPicker = false
                        cameraLauncher.launch(uri)
                    }
                } else {
                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                }
            },
            onSelectPhoto = { photoPickerLauncher.launch("image/*") },
            onSelectVideo = { videoPickerLauncher.launch("video/*") },
            onSelectFile = { filePickerLauncher.launch("*/*") },
            onDismiss = { showPicker = false }
        )
    }

    Scaffold(
        topBar = {
            // Removed the top bar row as requested
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.image),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .height(100.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
            ) {
                if (state.value.messages.isEmpty() && state.value.apiKey == null) {
                    EmptyChatView()
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.value.messages, key = { it.timestamp }) { message ->
                            if (message.attachmentUri != null) {
                                AttachmentMessageBubble(message = message)
                            } else {
                                MessageBubble(
                                    message = message,
                                    onPlayVoice = { viewModel.playResponseVoice(message) }
                                )
                            }
                        }
                        if (state.value.isLoading) {
                            item { LoadingBubble() }
                        }
                    }
                }

                state.value.error?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "⚠️ $error",
                            color = Color(0xFFFFEBEE),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.clearError() }
                        )
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = Color(0xFFFFEBEE),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearError() }
                        )
                    }
                }

                state.value.voiceError?.let { error ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "🎤 $error",
                            color = Color(0xFFFFD54F),
                            fontSize = 12.sp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.clearError() }
                        )
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "关闭",
                            tint = Color(0xFFFFD54F),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable { viewModel.clearError() }
                        )
                    }
                }

                if (state.value.apiKey == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { showApiKeyDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Pink500),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("设置 API Key 开始聊天", fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                } else {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.15f),
                        thickness = 1.dp
                    )
                    InputBar(
                        inputText = inputText,
                        pendingAttachment = pendingAttachment,
                        onInputChange = { inputText = it },
                        onSend = {
                            if (inputText.isNotBlank() || pendingAttachment != null) {
                                viewModel.sendMessage(inputText.trim(), pendingAttachment)
                                inputText = ""
                                pendingAttachment = null
                            }
                        },
                        onClearAttachment = { pendingAttachment = null },
                        onShowPicker = { showPicker = true },
                        onVoice = {
                            if (!isLongPressInProgress) {
                                isLongPressInProgress = true
                                if (!state.value.isPermissionGranted) {
                                    permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                } else {
                                    viewModel.startListening()
                                }
                            }
                        },
                        isListening = state.value.animationState is AnimationState.Listening
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyChatView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("💕", fontSize = 36.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "嗨，我是小薇~",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "你可以和我聊天，我会一直陪着你的 ❤️",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MessageBubble(message: ChatHistory, onPlayVoice: () -> Unit) {
    val isUser = message.isUser
    val isLastMessage = true

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Pink500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("🌸", fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            val context = LocalContext.current
            Surface(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .shadow(
                        if (isUser) 4.dp else 2.dp, RoundedCornerShape(
                            topStart = if (isUser) 16.dp else 4.dp,
                            topEnd = if (isUser) 4.dp else 16.dp,
                            bottomStart = 12.dp,
                            bottomEnd = 12.dp
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures(onLongPress = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = android.content.ClipData.newPlainText("message", message.content)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "已复制", android.widget.Toast.LENGTH_SHORT).show()
                        })
                    },
                color = if (isUser) Pink500 else MessageAi,
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
            ) {
                Text(
                    text = message.content,
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    lineHeight = 22.sp
                )
            }
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp, start = 36.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(Pink500.copy(alpha = 0.12f))
                            .clickable { onPlayVoice() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🔊", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "语音",
                        color = Pink400.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Pink500.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text("😊", fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Pink500.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Text("🌸", fontSize = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            modifier = Modifier
                .widthIn(max = 80.dp)
                .shadow(2.dp, RoundedCornerShape(12.dp)),
            color = MessageAi,
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                repeat(3) { i ->
                    val alpha = 0.4f + 0.3f * (i + 1) / 3f
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Pink400.copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

@Composable
fun InputBar(
    inputText: String,
    pendingAttachment: Attachment?,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onClearAttachment: () -> Unit,
    onShowPicker: () -> Unit,
    onVoice: () -> Unit,
    isListening: Boolean
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(top = 6.dp)) {
            if (pendingAttachment != null) {
                AttachmentPreviewRow(
                    attachment = pendingAttachment,
                    onClear = onClearAttachment
                )
                HorizontalDivider(color = Color.Black.copy(alpha = 0.08f), thickness = 0.5.dp)
            }
            Row(
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Pink500.copy(alpha = 0.12f))
                        .clickable { onShowPicker() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+", color = Pink500, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { onInputChange(it) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp),
                    placeholder = {
                        Text(
                            "给小薇发一条消息...",
                            color = SecondaryText.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    maxLines = 1,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = { if (inputText.isNotBlank() || pendingAttachment != null) onSend() }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Pink500,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = Pink500,
                        focusedContainerColor = SurfaceLight,
                        unfocusedContainerColor = SurfaceLight,
                        focusedPlaceholderColor = SecondaryText.copy(alpha = 0.5f),
                        focusedTextColor = Pink500,
                        unfocusedTextColor = Pink500
                    ),
                    shape = RoundedCornerShape(25.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isListening) Pink300 else Pink500.copy(alpha = 0.15f))
                        .clickable { onVoice() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "语音输入",
                        tint = if (isListening) Color.White else Pink500,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .shadow(4.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(Pink400, Pink600),
                                center = androidx.compose.ui.geometry.Offset(0.5f, 0.5f),
                                radius = 1f
                            )
                        )
                        .clickable(enabled = inputText.isNotBlank() || pendingAttachment != null) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "发送",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPreviewRow(
    attachment: Attachment,
    onClear: () -> Unit
) {
    val ctx = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (attachment.type == "image" || attachment.type == "video") {
            var bitmap by remember(attachment.uri) { mutableStateOf<android.graphics.Bitmap?>(null) }
            LaunchedEffect(attachment.uri) {
                withContext(Dispatchers.IO) {
                    try {
                        val inputStream = ctx.contentResolver.openInputStream(attachment.uri)
                        bitmap = inputStream?.use { android.graphics.BitmapFactory.decodeStream(inputStream) }
                    } catch (_: Exception) {}
                }
            }
            if (bitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("⏳", fontSize = 20.sp)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Pink500.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text("📄", fontSize = 28.sp)
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = attachment.fileName ?: "附件",
                color = Color(0xFF333333),
                fontSize = 13.sp,
                maxLines = 1
            )
            Text(
                text = when (attachment.type) {
                    "video" -> "视频"
                    "file" -> "文件"
                    else -> "图片"
                },
                color = SecondaryText,
                fontSize = 11.sp
            )
        }
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0))
                .clickable { onClear() },
            contentAlignment = Alignment.Center
        ) {
            Text("×", color = SecondaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Preview
@Composable
private fun InputBarPrivew() {
    AIGirlfriendTheme {
        InputBar("给小薇发一条消息", null, {}, {}, {}, {}, {}, false)
    }
}