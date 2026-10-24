package com.example.aigirlfriend.viewmodel

import android.net.Uri

public data class Attachment(
    val uri: Uri,
    val type: String,
    val fileName: String?
)
