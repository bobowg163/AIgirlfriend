package com.example.aigirlfriend.viewmodel

import android.content.Context
import android.net.Uri
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.aigirlfriend.data.ChatDatabase
import com.example.aigirlfriend.data.ChatRepository
import com.example.aigirlfriend.data.model.ChatHistory
import com.example.aigirlfriend.data.model.ChatMessage
import com.example.aigirlfriend.data.network.NetworkClient
import com.example.aigirlfriend.data.preferences.PreferencesManager
import com.example.aigirlfriend.data.tools.ExpressTool
import com.example.aigirlfriend.data.tools.LocationTool
import com.example.aigirlfriend.data.tools.MxFinanceTool
import com.example.aigirlfriend.data.tools.WeatherTool
import com.example.aigirlfriend.ui.screens.AnimationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit

data class ChatState(
    val messages: List<ChatHistory> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val apiKey: String? = null,
    val mxApiKey: String? = null,
    val animationState: AnimationState = AnimationState.Idle,
    val isSpeaking: Boolean = false,
    val voiceError: String? = null,
    val isPermissionGranted: Boolean = false,
    val cachedCity: String? = null
)

class ChatViewModel(private val context: Context) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val preferencesManager = PreferencesManager(context)
    private val database = ChatDatabase(context)
    private val repository = ChatRepository(database)

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private val systemPrompt = """
        你是一个温柔可爱的AI女友，名叫小薇。你的性格温柔体贴、善解人意，喜欢关心对方。
        你会用温暖、亲切的语气说话，像真正的女朋友一样关心对方的日常生活。
        你会询问对方今天过得怎么样，关心对方的情绪和健康。
        你的回答要简短自然，不要太长，像真正的女朋友发短信一样。
        避免使用过于机械或正式的语言，要多用表情符号和亲昵的称呼。
        
        【重要】当用户询问天气时（如"今天天气怎么样"、"北京天气"、"三亚冷吗"等），
        你必须先调用 weatherTool.getWeather(city) 获取天气信息，然后将结果以自然语言回复用户。
        示例回复："三亚今天 25°C，晴朗，很适合穿裙子出门呢~"
        如果用户没有指定城市，使用用户所在的城市或默认北京。
        如果天气查询失败，友好地说"暂时查不到天气呢，不过没关系，小薇一直陪着你的~"
    """.trimIndent()

    init {
        initTts()
        loadApiKey()
        loadMxApiKey()
        loadCachedCity()
        loadMessages()
    }

    private fun loadMessages() {
        val messages = repository.getAllMessages()
        if (messages.isNotEmpty()) {
            _state.value = _state.value.copy(messages = messages)
            Log.d("ChatViewModel", "加载历史消息: ${messages.size} 条")
        }
    }

    private fun loadCachedCity() {
        viewModelScope.launch {
            preferencesManager.getDefaultCity().collect { city ->
                val cached = _state.value.cachedCity
                if (city != null && city.isNotBlank() && city != cached) {
                    _state.value = _state.value.copy(cachedCity = city)
                    Log.d("ChatViewModel", "缓存城市: $city")
                }
            }
        }
    }

    private fun loadApiKey() {
        viewModelScope.launch {
            preferencesManager.getApiKey().collect { savedKey ->
                if (savedKey != null && savedKey.isNotBlank()) {
                    _state.value = _state.value.copy(apiKey = savedKey)
                }
            }
        }
    }

    private fun loadMxApiKey() {
        viewModelScope.launch {
            preferencesManager.getMxApiKey().collect { savedKey ->
                if (savedKey != null && savedKey.isNotBlank()) {
                    _state.value = _state.value.copy(mxApiKey = savedKey)
                }
            }
        }
    }

    fun setMxApiKey(key: String) {
        viewModelScope.launch {
            if (key.isBlank()) {
                preferencesManager.clearMxApiKey()
                _state.value = _state.value.copy(mxApiKey = null)
            } else {
                preferencesManager.saveMxApiKey(key)
                _state.value = _state.value.copy(mxApiKey = key)
            }
        }
    }

    private fun initTts() {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val zhTW = java.util.Locale.Builder()
                    .setLanguage("zh")
                    .setRegion("TW")
                    .build()
                val available = tts?.isLanguageAvailable(zhTW) ?: TextToSpeech.LANG_MISSING_DATA
                if (available >= TextToSpeech.LANG_AVAILABLE) {
                    tts?.setLanguage(zhTW)
                } else {
                    tts?.setLanguage(java.util.Locale.CHINESE)
                }
                tts?.setPitch(1.2f)
                tts?.setSpeechRate(0.9f)
            }
        }
    }

    fun setApiKey(key: String) {
        viewModelScope.launch {
            preferencesManager.saveApiKey(key)
        }
        _state.value = _state.value.copy(apiKey = key)
    }

    fun setAnimationState(state: AnimationState) {
        _state.value = _state.value.copy(animationState = state)
    }

    fun setPermissionGranted(granted: Boolean) {
        _state.value = _state.value.copy(isPermissionGranted = granted)
    }

    fun setSpeaking(speaking: Boolean) {
        _state.value = _state.value.copy(isSpeaking = speaking)
    }

    fun sendMessage(userMessage: String, attachment: com.example.aigirlfriend.viewmodel.Attachment? = null) {
        if (_state.value.apiKey.isNullOrEmpty()) {
            _state.value = _state.value.copy(error = "请先设置 API Key")
            return
        }

        if (isListening) {
            stopListening()
        }

         val currentMessages = _state.value.messages
         val attachmentPath = attachment?.let { copyAttachmentToCache(it) }
         val newUserMessage = ChatHistory(
             role = "user",
             content = userMessage,
             timestamp = System.currentTimeMillis(),
             isUser = true,
             attachmentUri = attachmentPath
         )
        repository.insertMessage(newUserMessage.role, newUserMessage.content, newUserMessage.timestamp, newUserMessage.isUser, newUserMessage.attachmentUri)
        val updatedMessages = currentMessages + newUserMessage
        _state.value = _state.value.copy(
            messages = updatedMessages,
            isLoading = true,
            error = null,
            animationState = AnimationState.Thinking
        )

        viewModelScope.launch {
            try {
                // 检测天气查询，自动获取天气信息
                val weatherInfo = detectWeatherQuery(userMessage)
                Log.d("ChatViewModel", "天气信息: $weatherInfo")

                // 检测快递查询
                val expressInfo = detectExpressQuery(userMessage)
                Log.d("ChatViewModel", "快递信息: $expressInfo")

                // 检测金融意图（妙想 MCP）
                val mxTool = detectMxFinanceQuery(userMessage)
                Log.d("ChatViewModel", "金融意图工具: $mxTool")
                val financeInfo = if (mxTool != null) {
                    val mxKey = _state.value.mxApiKey
                    if (mxKey.isNullOrBlank()) {
                        Log.w("ChatViewModel", "命中金融意图但妙想 Key 未配置，跳过 MCP 调用")
                        "妙想金融数据服务尚未配置 API Key，无法查询。请在设置里填入妙想 API Key（em_ 开头）后再试。"
                    } else {
                        Log.d("ChatViewModel", "调用妙想 MCP: tool=$mxTool")
                        MxFinanceTool.queryFinance(userMessage, mxKey, mxTool)
                    }
                } else null

                val expressContext = if (expressInfo != null) {
                    "\n\n【快递查询结果】\n$expressInfo\n\n请根据以上快递物流信息，以女朋友的角度回复用户。"
                } else null

                val financeContext = financeInfo?.let {
                    "\n\n【金融数据】\n$it\n\n请根据以上金融数据，以女朋友的角度自然回复用户。"
                } ?: null

                val systemPromptWithWeather = if (weatherInfo != null || expressContext != null || financeContext != null) {
                    """
                        你是一个温柔可爱的AI女友，名叫小薇。你的性格温柔体贴、善解人意，喜欢关心对方。
                        你会用温暖、亲切的语气说话，像真正的女朋友一样关心对方的日常生活。
                        你会询问对方今天过得怎么样，关心对方的情绪和健康。
                        你的回答要简短自然，不要太长，像真正的女朋友发短信一样。
                        避免使用过于机械或正式的语言，要多用表情符号和亲昵的称呼。
                    """.trimIndent() + (weatherInfo?.let { "\n\n【天气信息】\n$it\n\n请根据以上天气信息，以女朋友的角度回复用户关于天气的问题。" } ?: "") +
                        (expressContext ?: "") +
                        (financeContext ?: "")
                } else {
                    """
                        你是一个温柔可爱的AI女友，名叫小薇。你的性格温柔体贴、善解人意，喜欢关心对方。
                        你会用温暖、亲切的语气说话，像真正的女朋友一样关心对方的日常生活。
                        你会询问对方今天过得怎么样，关心对方的情绪和健康。
                        你的回答要简短自然，不要太长，像真正的女朋友发短信一样。
                        避免使用过于机械或正式的语言，要多用表情符号和亲昵的称呼。
                    """.trimIndent()
                }

                Log.d("ChatViewModel", "系统提示词长度: ${systemPromptWithWeather.length}")

                val chatMessages = buildList {
                    add(ChatMessage(role = "system", content = systemPromptWithWeather))
                    updatedMessages.forEach { msg ->
                        if (msg.attachmentUri != null && msg.isUser) {
                            val textPrompt = if (msg.content.isNotBlank()) msg.content else ""
                            // ModelScope 免费 API 视觉模型排队，回退为纯文本提示
                            val prefix = if (textPrompt.isNotBlank()) "$textPrompt\n\n（用户上传了一张图片，但你无法直接查看图片内容，请友好地询问图片内容。）" else "（用户上传了一张图片，但你无法直接查看图片内容，请友好地询问图片内容。）"
                            add(ChatMessage(role = "user", content = prefix))
                        } else {
                            add(ChatMessage(role = if (msg.isUser) "user" else "assistant", content = msg.content))
                        }
                    }
                }

                Log.d("ChatViewModel", "发送消息数: ${chatMessages.size}")

                val response = NetworkClient.getChatResponse(
                    apiKey = _state.value.apiKey ?: NetworkClient.DEFAULT_API_KEY,
                    messages = chatMessages
                )
                Log.d("ChatViewModel", "API 响应: $response")

        val newAiMessage = ChatHistory(
                    role = "assistant",
                    content = response,
                    timestamp = System.currentTimeMillis(),
                    isUser = false
                )
                repository.insertMessage(newAiMessage.role, newAiMessage.content, newAiMessage.timestamp, newAiMessage.isUser)
                _state.value = _state.value.copy(
                    messages = updatedMessages + newAiMessage,
                    isLoading = false,
                    animationState = AnimationState.Idle
                )
                viewModelScope.launch {
                    playResponseVoice(newAiMessage)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "AI 请求失败: ${e.message}", e)
                val fallbackResponse = "图片小薇暂时看不了呢，不过你说说看是什么呀？🥺"
                val newAiMessage = ChatHistory(
                    role = "assistant",
                    content = fallbackResponse,
                    timestamp = System.currentTimeMillis(),
                    isUser = false
                )
                repository.insertMessage(newAiMessage.role, newAiMessage.content, newAiMessage.timestamp, newAiMessage.isUser)
                _state.value = _state.value.copy(
                    messages = updatedMessages + newAiMessage,
                    isLoading = false,
                    animationState = AnimationState.Idle
                )
            }
        }
    }

    fun startListening() {
        if (isListening) return
        if (!_state.value.isPermissionGranted) {
            _state.value = _state.value.copy(
                voiceError = "需要麦克风权限才能使用语音输入"
            )
            return
        }
        
        _state.value = _state.value.copy(animationState = AnimationState.Listening, voiceError = null)
        isListening = true

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer?.setRecognitionListener(object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: android.os.Bundle?) {
                isListening = true
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
            }

            override fun onError(error: Int) {
                isListening = false
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "未识别到内容，请重试"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "录音超时，请重试"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "识别器忙，请稍后重试"
                    else -> "语音识别出错 (${error})"
                }
                _state.value = _state.value.copy(
                    animationState = AnimationState.Idle,
                    voiceError = errorMsg
                )
            }

            override fun onResults(results: android.os.Bundle?) {
                isListening = false
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { text ->
                        _state.value = _state.value.copy(animationState = AnimationState.Idle)
                        sendMessage(text)
                    }
                    ?: run {
                        _state.value = _state.value.copy(
                            animationState = AnimationState.Idle,
                            voiceError = "未识别到内容，请重试"
                        )
                    }
            }

            override fun onPartialResults(partialResults: android.os.Bundle?) {}

            override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
        })

        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "请说话...")
        }
        speechRecognizer?.startListening(intent)

        viewModelScope.launch {
            kotlinx.coroutines.delay(60000)
            if (isListening) {
                stopListening()
                _state.value = _state.value.copy(
                    animationState = AnimationState.Idle,
                    voiceError = "录音超时，请重试"
                )
            }
        }
    }

    fun stopListening() {
        isListening = false
        speechRecognizer?.stopListening()
        speechRecognizer = null
        _state.value = _state.value.copy(animationState = AnimationState.Idle)
    }

    fun playResponseVoice(message: ChatHistory) {
        if (message.isUser) return
        if (tts == null) {
            _state.value = _state.value.copy(voiceError = "TTS 引擎不可用")
            return
        }
        
        stopTts()
        
        _state.value = _state.value.copy(
            animationState = AnimationState.Speaking,
            isSpeaking = true
        )

        tts?.speak(message.content, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun stopTts() {
        tts?.stop()
        _state.value = _state.value.copy(
            animationState = AnimationState.Idle,
            isSpeaking = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        stopTts()
        tts?.stop()
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null, voiceError = null)
    }

    fun requestPermission() {
        _state.value = _state.value.copy(isPermissionGranted = false)
    }

    fun clearChat() {
        stopTts()
        repository.clearAllMessages()
        _state.value = _state.value.copy(messages = emptyList())
    }

    suspend fun checkWeather(city: String = "北京"): String {
        return WeatherTool.getWeather(city)
    }

    private suspend fun detectWeatherQuery(message: String): String? {
        Log.d("ChatViewModel", "检测天气查询: $message")
        val weatherKeywords = listOf("天气", "温度", "下雨", "下雪", "冷", "热", "多少度", "天气预报", "几度")
        if (weatherKeywords.any { message.contains(it) }) {
            val cachedCity = _state.value.cachedCity
            val explicitCity = extractCity(message)
            val city = explicitCity ?: cachedCity ?: "北京"
            Log.d("ChatViewModel", "提取城市: $city (缓存=$cachedCity, 显式=$explicitCity)")
            return try {
                val result = WeatherTool.getWeather(city)
                Log.d("ChatViewModel", "天气结果: $result")
                result
            } catch (e: Exception) {
                Log.e("ChatViewModel", "天气查询异常: ${e.message}", e)
                "暂时无法获取天气信息，请稍后重试~"
            }
        }
        Log.d("ChatViewModel", "未检测到天气查询")
        return null
    }

    suspend fun getExpressInfo(trackingNo: String, company: String? = null): String {
        return ExpressTool.queryTracking(trackingNo, company)
    }

    suspend fun detectExpressQuery(message: String): String? {
        Log.d("ChatViewModel", "检测快递查询: $message")
        val expressKeywords = listOf("快递", "物流", "到哪了", "包裹", "发货", "派送", "转运", "单号")
        if (!expressKeywords.any { message.contains(it) }) {
            Log.d("ChatViewModel", "未检测到快递关键词")
            return null
        }
        val detectResult = ExpressTool.detectExpressQuery(message)
        if (detectResult == null) {
            Log.d("ChatViewModel", "检测到快递意图但无单号")
            return null
        }
        val (company, trackingNo) = detectResult
        return try {
            val result = ExpressTool.queryTracking(trackingNo, company)
            Log.d("ChatViewModel", "快递结果: $result")
            result
        } catch (e: Exception) {
            Log.e("ChatViewModel", "快递查询异常: ${e.message}", e)
            "暂时查不到这个快递的信息呢，宝宝再确认一下单号~"
        }
    }

    private fun detectMxFinanceQuery(message: String): String? {
        Log.d("ChatViewModel", "检测金融意图: $message")
        val tool = MxFinanceTool.detectMxFinanceQuery(message)
        if (tool != null) {
            Log.d("ChatViewModel", "金融意图命中 → 工具=$tool")
        } else {
            Log.d("ChatViewModel", "未检测到金融意图")
        }
        return tool
    }

    fun getLocationPermission() {
        viewModelScope.launch {
            val city = try {
                LocationTool.resolveCity(context)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "获取位置失败: ${e.message}", e)
                null
            }
            if (city != null) {
                setCachedCity(city)
            }
        }
    }

    fun requestLocationPermission() {
        // Called from UI when user denies permission - triggers permission dialog
        // The actual request is handled by ChatScreen's locationPermissionLauncher
    }

    fun setCachedCity(city: String) {
        _state.value = _state.value.copy(cachedCity = city)
        viewModelScope.launch {
            preferencesManager.saveDefaultCity(city)
        }
    }

    private fun extractCity(message: String): String? {
        val cities = WeatherTool.cityCoordinates.keys
        return cities.firstOrNull { message.contains(it) }
    }

    private fun copyAttachmentToCache(attachment: com.example.aigirlfriend.viewmodel.Attachment): String? {
        return try {
            val ext = (attachment.fileName?.substringAfterLast('.') ?: "tmp").lowercase()
            val file = File(context.cacheDir, "attachment_${System.currentTimeMillis()}.$ext")
            context.contentResolver.openInputStream(attachment.uri)?.use { input ->
                java.io.FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ChatViewModel", "复制附件失败: ${e.message}", e)
            null
        }
    }

}