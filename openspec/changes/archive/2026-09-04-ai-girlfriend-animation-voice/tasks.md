## 1. API 配置更新

- [x] 1.1 更新 `NetworkClient.kt`：将 `BASE_URL` 改为 `https://api-inference.modelscope.cn/v1`，默认模型改为 `Qwen/Qwen3.8-27B`，API Key 使用 `ms-753bd3be-a78f-4fbf-b007-c4eb7dda9c82`，验证 API 调用正常返回响应
- [x] 1.2 确认 `OpenAIApiService.kt` 的 endpoint 路径 `/v1/chat/completions` 与 ModelScope 推理平台兼容（如不兼容则调整注解路径）

## 2. 依赖与资源准备

- [x] 2.1 在 `libs.versions.toml` 中添加 `androidx.media3` 依赖版本，并在 `app/build.gradle.kts` 中添加 implementation 依赖，验证 Gradle sync 成功
- [x] 2.2 在 AndroidManifest.xml 中添加 `RECORD_AUDIO` 权限声明，验证编译通过

## 3. 数据模型扩展

- [x] 3.1 在 `ChatViewModel.kt` 的 `ChatState` data class 中添加 `animationState: AnimationState = AnimationState.Idle` 和 `isSpeaking: Boolean = false` 字段，其中 `AnimationState` 为 sealed class（Idle, Thinking, Listening, Speaking）
- [x] 3.2 在 `ChatViewModel.kt` 中添加 `setAnimationState()` 和 `setSpeaking()` 方法，验证 StateFlow 状态变更正确触发

## 4. 角色动画组件

- [x] 4.1 创建 `CharacterView.kt` Composable 组件，接收 `animationState` 参数，使用本地资源 `R.drawable.img` 渲染角色立绘（约占屏幕 40% 高度），显示当前状态文字标签
- [x] 4.2 实现 idle/thinking/listening/speaking 四种状态的视觉区分（通过不同图片资源或 Compose 动画过渡），验证状态切换时 UI 正确响应
- [x] 4.3 实现立绘图片加载（直接使用 `R.drawable.img` 本地资源，无需网络），添加加载失败时的降级静态显示，验证无网络时不崩溃
- [x] 4.4 调整立绘图像位置：通过底部内边距或顶部偏移将图像向下调整，确保角色头像完整可见不被顶部裁切，验证头像露出正常
- [x] 4.5 将静态图片替换为视频播放：使用 VideoView 播放本地 `raw/video.mp4`，自动循环播放，验证视频正常渲染
- [x] 4.6 实现视频随机轮播功能：按 video.mp4、video1.mp4、video2.mp4 随机切换播放，视频播放结束时随机切换到下一个不重复的视频

## 5. 语音输入（STT）

- [x] 5.1 在 `ChatViewModel.kt` 中添加 `startListening()` 和 `stopListening()` 方法，使用 `SpeechRecognizer` 实现语音识别，识别结果通过回调或 StateFlow 返回
- [x] 5.2 实现录音权限检查逻辑，未授权时弹出系统权限申请，拒绝时显示友好提示文字
- [x] 5.3 添加运行时权限请求逻辑：在调用 SpeechRecognizer 前检查 RECORD_AUDIO 权限，无权限时通过 ActivityResultContracts.RequestPermission 请求，用户拒绝后显示"需要麦克风权限才能使用语音输入"提示
- [x] 5.4 在 `ChatScreen.kt` 中新增长按语音按钮，录音期间触发 `listening` 动画状态，松开发送识别结果作为文本消息，验证语音消息在聊天列表中正确显示并带语音图标标识

## 6. 语音输出（TTS）

- [x] 6.1 在 `ChatViewModel.kt` 中添加 `playResponseVoice(message: ChatHistory)` 方法，使用 `TextToSpeech` 播放指定消息文本，支持播放中断（新消息发送时停止当前播放）
- [x] 6.2 在 `ChatScreen.kt` 的消息气泡中添加语音播放按钮，点击后触发 TTS 播放并切换角色至 `speaking` 动画状态，播放结束后恢复 `idle`
- [x] 6.3 处理 TTS 引擎不可用情况（检测 `TextToSpeech` 初始化返回值），按钮置灰并显示提示
- [x] 6.4 在 AI 回复生成完成后自动调用 TTS 播放，无需用户手动点击播放按钮

## 7. 聊天界面重构

- [x] 7.1 重构 `ChatScreen.kt` 布局：将 `CharacterView` 置于顶部，消息列表（LazyColumn）居中，底部输入框保持原位，验证整体布局在不同屏幕尺寸下正常显示
- [x] 7.2 确保语音输入按钮与文本输入框共存于底部输入区域（或语音按钮替换发送按钮），验证两种输入方式均可正常发送消息
- [x] 7.3 验证动画状态与对话生命周期的联动：发送消息后进入 `thinking` → AI 回复后恢复 `idle`，语音输入进入 `listening` → 发送后恢复 `idle`，TTS 播放进入 `speaking` → 播放结束恢复 `idle`

## 8. 测试与验证

- [x] 8.1 运行 `./gradlew assembleDebug` 验证编译通过，生成 APK
- [x] 8.2 在设备上测试完整流程：文字聊天、语音输入发送消息、TTS 播放 AI 回复、角色动画状态切换，验证无崩溃且功能符合规格要求

## 9. API Key 持久化

- [x] 9.1 在 `ChatViewModel` 中添加 DataStore 依赖，初始化时自动加载已保存的 API Key
- [x] 9.2 修改 `setApiKey()` 方法，保存 API Key 到 DataStore 并更新 StateFlow
- [x] 9.3 修改 `ChatScreen.kt` 和 `StartScreen.kt`，移除手动输入界面，改为设置页面
- [x] 9.4 验证：应用重启后 API Key 自动加载，直接进入聊天界面

## 10. 天气工具

- [x] 10.1 在 `PreferencesManager` 中添加天气相关偏好存储（如默认城市）
- [x] 10.2 创建 `WeatherTool.kt`，封装天气查询 API 调用逻辑
- [x] 10.3 在 `ChatViewModel` 的系统提示词中注入天气工具使用说明
- [x] 10.4 验证：用户询问天气时 AI 能正确返回天气信息

## 11. 天气工具修复

- [x] 11.1 修复 WeatherTool.kt：改用 Open-Meteo API（免费无需 API Key），添加 26 个中国城市坐标
- [x] 11.2 更新系统提示词，添加"未指定城市时使用北京"的说明
- [x] 11.3 验证编译通过，APK 生成成功
