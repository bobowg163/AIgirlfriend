## Why

当前 AI 女友应用仅有纯文字聊天功能，用户缺乏与虚拟角色的视觉和情感连接。加入角色动画和语音对话功能，可以大幅提升用户体验和陪伴感，让小薇更有"生命力"。

## What Changes

- **新增角色动画系统**：在聊天界面顶部展示小薇的角色立绘视频（本地资源 `video.mp4`、`video1.mp4`、`video2.mp4`），视频自动轮播切换，并支持基础动画状态（idle、thinking、happy、listening 等）
- **新增语音对话功能**：支持语音输入（语音转文字）和语音输出（文字转语音播放）；AI 回复生成后自动播放语音
- **新增动画状态管理**：根据对话状态切换角色动画
- **UI 重构**：聊天界面重新布局，上方展示角色区域，下方为聊天消息
- **API Key 自动保存**：用户输入的 API Key 将持久化保存到本地 DataStore，下次启动时自动加载，无需重新输入
- **新增天气预报工具**：AI 可调用天气查询 API 获取实时天气信息，在对话中为用户提供天气相关回复

## Capabilities

### New Capabilities
- `character-animation`: 角色立绘展示与动画状态切换
- `voice-conversation`: 语音输入与语音播放对话能力
- `api-key-persistence`: API Key 本地持久化存储
- `weather-tool`: 天气预报查询工具

### Modified Capabilities

## Impact

- 新增依赖：`androidx.media3`（语音播放）、Android Speech-to-Text API、TTS API
- 新增资源：角色立绘视频 `video.mp4`、`video1.mp4`、`video2.mp4`（本地 raw 资源）、动画帧序列或动画资源文件
- 修改文件：`ChatScreen.kt`（UI 重构）、`ChatViewModel.kt`（新增动画状态、语音状态和 API Key 持久化）、`NetworkClient.kt`（API 端点和模型变更）、`PreferencesManager.kt`（扩展存储逻辑）
- 权限需求：`RECORD_AUDIO`（语音输入）
- API 配置：ModelScope 推理平台 `https://api-inference.modelscope.cn/v1`，模型 `Qwen/Qwen3.8-27B`，API Key `ms-753bd3be-a78f-4fbf-b007-c4eb7dda9c82`
