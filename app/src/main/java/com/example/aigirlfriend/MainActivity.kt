package com.example.aigirlfriend

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.aigirlfriend.ui.screens.ChatScreen
import com.example.aigirlfriend.ui.screens.StartScreen
import com.example.aigirlfriend.ui.theme.AIGirlfriendTheme
import com.example.aigirlfriend.viewmodel.ChatViewModel
import com.example.aigirlfriend.viewmodel.ChatViewModelFactory

object ViewModelHolder {
    @Volatile var viewModel: ChatViewModel? = null
}

class MainActivity : ComponentActivity() {

    private var incomingText: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        incomingText = extractTextFromIntent(intent)
        Log.d("MainActivity", "onCreate incomingText: $incomingText")

        enableEdgeToEdge()
        setContent {
            AIGirlfriendTheme {
                val vm: ChatViewModel = viewModel(factory = ChatViewModelFactory(applicationContext))
                ViewModelHolder.viewModel = vm
                val state = vm.state.collectAsState()

                if (!incomingText.isNullOrBlank()) {
                    vm.sendMessage(incomingText!!)
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (state.value.apiKey == null) {
                        StartScreen(viewModel = vm)
                    } else {
                        ChatScreen(viewModel = vm)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val text = extractTextFromIntent(intent)
        Log.d("MainActivity", "onNewIntent text: $text")
        if (!text.isNullOrBlank()) {
            ViewModelHolder.viewModel?.sendMessage(text)
        }
    }

    private fun extractTextFromIntent(intent: Intent?): String? {
        return intent?.getStringExtra("text") ?: intent?.getStringExtra(android.content.Intent.EXTRA_TEXT)
    }
}
