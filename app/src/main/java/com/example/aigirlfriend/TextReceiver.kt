package com.example.aigirlfriend

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class TextReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val text = intent.getStringExtra("text") ?: intent.getStringExtra(Intent.EXTRA_TEXT)
        if (!text.isNullOrBlank()) {
            Log.d("TextReceiver", "收到广播: $text")
            ViewModelHolder.viewModel?.sendMessage(text)
        }
    }
}
