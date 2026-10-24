## Context

当前应用已实现基础的文字聊天功能，使用 ModelScope 推理平台 API（`https://api-inference.modelscope.cn/v1`，模型 `Qwen/Qwen3.8-27B`）进行对话，UI 基于 Jetpack Compose Material 3 构建。聊天界面仅包含消息列表和文本输入框，无角色展示和语音能力。角色立绘已作为本地资源 `video.mp4` 存放于 `res/raw/` 目录。应用已集成 `androidx.datastore` 用于本地数据存储。

目标：在现有架构基础上，增加角色立绘动画展示层和语音交互层，同时保持与现有 ViewModel 和 API 层的兼容性。

## Goals / Non-Goals

**Goals:**
- 在聊天界面顶部区域嵌入角色立绘与动画状态展示
- 实现语音输入（STT）和语音输出（TTS）功能
- 将动画状态与对话生命周期联动（idle/thinking/listening/speaking）
- 保持现有文字聊天功能完全兼容
- 使用 ModelScope 推理平台 API（`https://api-inference.modelscope.cn/v1`，模型 `Qwen/Qwen3.8-27B`）
- API Key 持久化存储，避免重复输入
- AI 可调用天气查询工具增强实用性

**Non-Goals:**
- 不支持 3D 角色模型或复杂骨骼动画
- 不支持多角色切换或自定义角色
- 不支持离线 TTS/STT（依赖系统服务）
- 不改变 ModelScope API 集成逻辑（端点和模型已确定）

## Decisions

### 决策 1：角色动画实现方案 — 使用 Jetpack Compose AnimatedVisibility + FrameAnimationDrawable

**选择**：使用 Android `AnimationDrawable`（帧动画）或 Compose `AnimatedContent` 实现角色状态切换，结合静态立绘图片。

**理由**：
- 帧动画资源易于准备（PNG 序列），兼容性好
- Compose `AnimatedContent` 适合状态驱动的 UI 过渡
- 无需引入额外重型动画库

**替代方案**：
- Lottie 动画：功能强大但需要设计专门动画文件，资源准备成本高
- Compose Animation API：适合简单过渡，不适合复杂角色动画

### 决策 2：语音输入使用 Android 原生 SpeechRecognizer

**选择**：使用 `android.speech.SpeechRecognizer` 系统级 STT API。

**理由**：
- 系统内置，无需额外依赖
- 支持离线语音识别（依赖设备语言包）
- 与 Android 系统深度集成

**权限要求**：必须在调用前检查并请求 `RECORD_AUDIO` 运行时权限，使用 `ActivityResultContracts.RequestPermission` 获取用户授权。

**替代方案**：
- Whisper API：精度更高但需要网络，增加延迟和成本
- Google Speech SDK：额外依赖，包体积增加

### 决策 3：语音输出使用 Android TextToSpeech

**选择**：使用 `android.speech.tts.TextToSpeech` 系统级 TTS API。

**理由**：
- 系统内置，无需额外依赖
- 支持中文语音包（需设备预装或下载）
- API 简单易用，支持播放控制和中断

**替代方案**：
- OpenAI TTS API：音质更好但需要网络，增加延迟和 API 调用成本
- 第三方 TTS SDK：额外依赖和许可问题

### 决策 4：动画状态纳入现有 ChatViewModel

**选择**：在现有 `ChatState` 中扩展 `animationState` 和 `isSpeaking` 字段，不引入新的 ViewModel。

**理由**：
- 动画和语音状态与聊天对话生命周期紧密耦合
- 避免 ViewModel 过多导致状态管理复杂
- 便于在 Compose 中统一收集状态

**替代方案**：
- 新增独立 AnimationViewModel：职责分离但增加状态传递复杂度

### 决策 5：角色立绘使用本地 drawable 资源

**选择**：角色立绘使用本地 `res/drawable/img.png` 静态图片，通过 Compose `Image` 组件直接引用，不做远程加载。立绘图像 SHALL 通过底部内边距或顶部偏移向下调整，确保头像完整可见。

**理由**：
- 立绘资源已由用户提供并放置于 `drawable` 目录
- 本地资源无网络依赖，加载速度快，体验稳定
- 无需引入 Coil 等图片加载库，减少依赖复杂度
- 后续如需支持动态切换可在此基础扩展

**替代方案**：
- Coil 远程加载：灵活性高但需要网络，资源准备流程复杂
- 纯本地资源（已采用）

### 决策 6：API 端点与模型配置

**选择**：使用 ModelScope 推理平台 `https://api-inference.modelscope.cn/v1`，模型 `Qwen/Qwen3.8-27B`，API Key `ms-753bd3be-a78f-4fbf-b007-c4eb7dda9c82` 在 `NetworkClient.kt` 中配置默认值。

**理由**：
- 已明确指定使用 ModelScope 平台替代 OpenAI
- 模型 Qwen3.8-27B 支持中文对话，适合 AI 女友场景
- API Key 硬编码便于快速演示，生产环境建议移至配置或环境变量

**替代方案**：
- 保留 OpenAI API：与用户指定不符
- API Key 存入 SharedPreferences：更安全但增加实现复杂度，当前阶段优先快速验证

## Risks / Trade-offs

- **[STT 识别精度依赖设备]** → 不同设备 TTS/STT 质量差异较大，需提供明确的错误提示和重试引导
- **[TTS 中文女声需预装]** → 部分设备可能无中文女声，TTS 按钮置灰并提示用户；优先使用台湾国语（zh-TW），若不可用则降级为简体中文
- **[动画资源准备]** → 帧动画需要美术资源，初始版本可使用静态立绘 + 简单过渡动画替代
- **[麦克风权限]** → 需要运行时权限申请，首次使用需引导用户授权
- **[语音输入期间 API 调用]** → 录音时不应触发 API 请求，需确保状态互斥

## Migration Plan

1. 更新 `NetworkClient.kt`：将 `BASE_URL` 改为 `https://api-inference.modelscope.cn/v1`，默认模型改为 `Qwen/Qwen3.8-27B`，API Key 使用 `ms-753bd3be-a78f-4fbf-b007-c4eb7dda9c82`
2. 在 `ChatState` 中添加 `animationState: AnimationState` 和 `isSpeaking: Boolean` 字段
3. 创建 `CharacterView.kt` Composable 组件，使用本地 `R.drawable.img` 加载角色立绘，置于聊天界面顶部
4. 在 `ChatViewModel` 中新增 `startListening()`、`stopListening()`、`playResponseVoice()` 方法
5. 修改 `ChatScreen.kt`，将现有消息列表上移，顶部插入角色区域
6. 添加 `RECORD_AUDIO` 运行时权限请求逻辑
7. 集成 TTS 初始化和 STT 录音逻辑
8. 测试：语音输入/输出、动画状态切换、权限处理

**回滚策略**：新代码通过 feature flag 或版本隔离，旧版文字聊天功能不受影响。

### 决策 7：AI 回复自动语音播放

**选择**：AI 回复生成完成后自动调用 TTS 播放，无需用户点击播放按钮。

**理由**：
- 提升用户体验，减少操作步骤
- 符合"AI 女友"角色的陪伴感设计
- 用户仍可通过手动操作中断自动播放

**替代方案**：
- 仅手动播放：用户体验较差，需要额外操作
- 自动播放 + 用户可关闭：更灵活，但增加设置复杂度

### 决策 8：多视频随机轮播

**选择**：使用三个本地视频文件（video.mp4、video1.mp4、video2.mp4）随机轮播，当视频播放结束时随机切换到下一个不重复的视频。

**理由**：
- 增加角色视觉丰富度，避免单一视频重复播放
- 本地资源无网络依赖，加载速度快
- 实现简单，通过 `VideoView.setOnCompletionListener` 监听播放结束事件，使用随机数选择下一个视频
- 随机轮播比顺序轮播更具趣味性

**替代方案**：
- 顺序轮播：实现简单但视觉单调
- 帧动画序列：资源准备成本高，包体积大

### 决策 9：API Key 持久化存储方案

**选择**：使用 Android DataStore Preferences 进行 API Key 持久化，ViewModel 在初始化时自动加载已保存的 API Key。

**理由**：
- DataStore 是 Android 推荐的现代存储方案，类型安全，协程友好
- 已有 `PreferencesManager` 实现，可直接复用
- 应用启动时异步加载，不影响主线程性能

**替代方案**：
- SharedPreferences：老旧方案，API 已不推荐
- Room 数据库：过重，仅存储一个字符串无需完整数据库

### 决策 10：天气工具集成方案

**选择**：在系统提示词中注入天气工具使用说明，AI 根据用户意图自动判断是否调用天气查询。

**理由**：
- ModelScope Qwen 模型支持工具调用（Function Calling）
- 无需额外接入第三方天气 API，降低依赖复杂度
- 通过系统提示词引导 AI 行为，实现简单

**替代方案**：
- 手动输入天气 API：增加用户配置负担
- 内置天气数据：数据过时，无法提供实时信息

## Open Questions

- 语音输入是否需要在界面显示实时录音波形可视化？
- 自动语音播放时用户是否可以手动关闭？
- 天气查询是否需要获取用户当前位置？
