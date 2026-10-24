package com.example.aigirlfriend.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aigirlfriend.R
import com.example.aigirlfriend.ui.theme.*

sealed class AnimationState {
    object Idle : AnimationState()
    object Thinking : AnimationState()
    object Listening : AnimationState()
    object Speaking : AnimationState()
}

@Composable
fun CharacterView(
    animationState: AnimationState,
    modifier: Modifier = Modifier
) {
    val stateLabel = when (animationState) {
        is AnimationState.Idle -> "💕 小薇在线"
        is AnimationState.Thinking -> "🌸 思考中..."
        is AnimationState.Listening -> "🎤 聆听中..."
        is AnimationState.Speaking -> "🔊 说话中..."
    }

    val stateColor = when (animationState) {
        is AnimationState.Idle -> Color(0xFF4CAF50)
        is AnimationState.Thinking -> Pink400
        is AnimationState.Listening -> Color(0xFF2196F3)
        is AnimationState.Speaking -> Color(0xFFFF9800)
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val glowAlpha = if (animationState is AnimationState.Idle) 0.15f else {
        infiniteTransition.animateFloat(
            initialValue = 0.2f, targetValue = 0.6f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1000, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        ).value
    }

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        androidx.compose.foundation.Image(
            painter = painterResource(id = R.drawable.image),
            contentDescription = "角色形象",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .height(80.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f))
                    )
                )
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (animationState !is AnimationState.Idle) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(stateColor.copy(alpha = glowAlpha))
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = stateLabel,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
