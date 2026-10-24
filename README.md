# AIgirlfriend 🎀

**你的专属 AI 女友「小薇」** — 基于 Android Jetpack Compose 的本地 AI 聊天伴侣应用。

小薇拥有可爱的角色立绘动画、贴心的聊天陪伴、语音对话，还能帮你查天气、查快递、认图片。

## ✨ 功能特性

| 功能 | 说明 |
|------|------|
| 💬 AI 智能聊天 | 基于 OpenAI 兼容接口（ModelScope + Qwen 模型），女朋友口吻自然回复 |
| 👩 角色立绘与动画 | 聊天界面顶部展示角色立绘视频，随对话状态自动切换 idle / thinking / listening / speaking 动画 |
| 🎙️ 语音对话 | 语音输入（语音转文字）+ AI 回复文字转语音播放 |
| 📷 图片识别 | 发送图片后，通过视觉模型（如 Qwen2.5-VL）识别图片内容并回复 |
| 📎 附件发送 | 输入框 "+" 按钮支持拍照、相册选图/视频、选择任意文件，聊天中展示附件 |
| 🌤️ 天气查询 | 对话中询问天气时自动调用天气工具，结合定位获取本地城市信息 |
| 📦 快递查询 | 发送快递单号，自动查询并返回物流状态 |
| 🔒 API Key 本地持久化 | 首次输入后本地保存（DataStore），无需每次启动重新输入 |
| 📍 位置获取 | 获取当前城市用于天气等位置相关服务，并在 UI 显示 |

## 🏗️ 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM（`ChatViewModel` + `ChatState` StateFlow）
- **AI 接口**: OpenAI 兼容 REST API（Retrofit + OkHttp + Gson），默认使用 ModelScope 推理服务
- **本地存储**: DataStore Preferences（API Key、历史城市等）
- **语音**: Android `SpeechRecognizer`（ASR）+ `TextToSpeech`（TTS）
- **视频**: Media3 ExoPlayer（角色立绘循环播放）
- **定位**: Play Services Location
- **构建**: Gradle Kotlin DSL，`minSdk 36`，`targetSdk 37`

## 📱 快速开始

### 前置条件

- Android Studio（支持 Android 15+ SDK）
- 一个 ModelScope API Key（在 [modelscope.cn](https://modelscope.cn) 申请）

### 构建运行

```bash
# 克隆项目
git clone https://github.com/bobowg163/AIgirlfriend.git
cd AIgirlfriend

# 通过 Android Studio 打开，或命令行构建
./gradlew :app:assembleDebug
```

安装到手机后：

1. 首次启动在欢迎页输入 **ModelScope API Key**（仅保存在本地设备，不会上传）
2. 进入聊天界面即可开始与小薇对话
3. 点击 "🎙️ 语音" 按钮可以语音对话；点击输入框左侧 "+" 可以拍照、选图或发文件
4. 问她 "今天天气怎么样" 可以触发天气工具；发快递单号可以查物流

## 📁 项目结构

```
app/src/main/java/com/example/aigirlfriend/
├── MainActivity.kt                  # 应用入口
├── TextReceiver.kt                  # TTS 朗读接收器
├── data/
│   ├── api/OpenAIApiService.kt      # OpenAI 兼容接口定义
│   ├── ChatDatabase.kt              # 聊天记录本地持久化
│   ├── ChatRepository.kt            # 聊天数据仓库
│   ├── model/                       # 聊天消息、历史等数据模型
│   ├── network/NetworkClient.kt     # 网络请求（ModelScope BASE_URL / 默认模型）
│   ├── preferences/                 # DataStore 偏好管理
│   └── tools/                       # 内置工具：天气 / 快递 / 定位
├── ui/
│   ├── components/                  # 附件气泡、附件选择对话框
│   ├── screens/                     # StartScreen / ChatScreen / CharacterView
│   └── theme/                       # Material3 主题
└── viewmodel/ChatViewModel.kt       # 聊天核心逻辑（消息、语音、动画状态）
```

## 📋 功能规格

项目使用 [OpenSpec](https://github.com/) 管理行为规格，当前能力清单：

| 能力 | 规格文档 |
|------|---------|
| AI 视觉识别 | `openspec/specs/ai-vision/spec.md` |
| API Key 持久化 | `openspec/specs/api-key-persistence/spec.md` |
| 附件选择与展示 | `openspec/specs/attachment-picker/spec.md` |
| 角色立绘动画 | `openspec/specs/character-animation/spec.md` |
| 快递查询 | `openspec/specs/express-query/spec.md` |
| 位置获取 | `openspec/specs/location-tracking/spec.md` |
| 语音对话 | `openspec/specs/voice-conversation/spec.md` |
| 天气工具 | `openspec/specs/weather-tool/spec.md` |

已完成的变更记录在 `openspec/changes/archive/`。

## ⚠️ 隐私说明

- API Key 仅保存在本设备（DataStore），不会上传到任何服务器
- 位置信息仅在本地使用，用于天气等城市相关服务
- 聊天记录保存在本地设备

## 📄 License

MIT License
