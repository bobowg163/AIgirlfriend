package com.example.aigirlfriend.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.aigirlfriend.R
import com.example.aigirlfriend.ui.theme.*
import com.example.aigirlfriend.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(viewModel: ChatViewModel) {
    var showSettings by remember { mutableStateOf(false) }
    var apiKeyInput by remember { mutableStateOf("") }
    var mxApiKeyInput by remember { mutableStateOf("") }

    val infiniteTransition = rememberInfiniteTransition(label = "float")

    val heart1Y by infiniteTransition.animateFloat(
        initialValue = -30f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart1"
    )
    val heart2Y by infiniteTransition.animateFloat(
        initialValue = -20f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart2"
    )
    val heart3Y by infiniteTransition.animateFloat(
        initialValue = -25f,
        targetValue = 25f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heart3"
    )

    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = {
                Text(
                    "⚙️ API Key 设置",
                    style = MaterialTheme.typography.titleMedium,
                    color = PrimaryText
                )
            },
            text = {
                Column {
                    Text(
                        "当前使用内置 API Key，无需手动配置。",
                        color = SecondaryText,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "如需自定义，请输入新的 Key：",
                        color = SecondaryText.copy(alpha = 0.8f),
                        fontSize = 13.sp
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
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "🔒 API Key 仅保存在本地设备，不会上传到任何服务器。",
                        color = SecondaryText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "💹 妙想 API Key（可选，金融数据服务）：",
                        color = SecondaryText.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = mxApiKeyInput,
                        onValueChange = { mxApiKeyInput = it },
                        placeholder = { Text("输入妙想 API Key（em_ 开头）", color = Color(0xFFCCCCCC)) },
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "填入后可查询股票、基金、财经新闻等金融数据；同样仅保存在本地。",
                        color = SecondaryText.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setApiKey(apiKeyInput.trim())
                        viewModel.setMxApiKey(mxApiKeyInput.trim())
                        showSettings = false
                        apiKeyInput = ""
                        mxApiKeyInput = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Pink500),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("保存", fontSize = 14.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSettings = false }) {
                    Text("取消", color = SecondaryText)
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(200.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.75f)
                        )
                    )
                )
        )

        Text(
            "💕",
            fontSize = 28.sp,
            modifier = Modifier
                .offset(x = (-40).dp, y = heart1Y.dp)
                .alpha(0.35f)
        )
        Text(
            "✨",
            fontSize = 20.sp,
            modifier = Modifier
                .offset(x = 50.dp, y = heart2Y.dp)
                .alpha(0.3f)
        )
        Text(
            "🌸",
            fontSize = 24.sp,
            modifier = Modifier
                .offset(x = (-30).dp, y = heart3Y.dp)
                .alpha(0.3f)
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            CharacterView(
                animationState = AnimationState.Idle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .shadow(16.dp, RoundedCornerShape(20.dp))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "💕",
                fontSize = 44.sp,
                modifier = Modifier
                    .shadow(8.dp, CircleShape)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "小薇",
                color = Pink600,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "你的 AI 女友",
                color = Pink400,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "温柔的陪伴 · 贴心的对话",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = { showSettings = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(8.dp, RoundedCornerShape(27.dp)),
                shape = RoundedCornerShape(27.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Pink500)
            ) {
                Text("开始聊天", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(14.dp))

            TextButton(
                onClick = { showSettings = true },
                modifier = Modifier
                    .height(40.dp)
                    .padding(horizontal = 32.dp)
            ) {
                Text("设置 API Key", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                infoChip(modifier = Modifier.weight(1f), "🔒 本地存储")
                infoChip(modifier = Modifier.weight(1f), "🎤 语音互动")
                infoChip(modifier = Modifier.weight(1f), "🌤️ 天气查询")
            }
        }
    }
}

@Composable
fun infoChip(modifier: Modifier = Modifier, text: String) {
    Surface(
        modifier = modifier.shadow(2.dp, RoundedCornerShape(12.dp)),
        color = Color.White.copy(alpha = 0.85f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF666666),
            fontSize = 11.sp,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}
