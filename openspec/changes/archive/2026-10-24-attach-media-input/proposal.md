## Why

用户希望在聊天中不仅能发送文字，还能发送图片、视频和文件，使"小薇"的陪伴体验更丰富、更贴近真实对话场景。

## What Changes

- **输入框"+"按钮**：在输入框左侧新增"+"号按钮，点击后弹出菜单（拍照/从相册选择/选择文件）
- **媒体预览**：选择图片或视频后，在输入框上方显示预览，可删除或发送
- **附件消息气泡**：发送的图片/视频在聊天中以缩略图形式展示，支持点击放大；文件以文件名+图标形式展示
- **权限处理**：拍照和相册访问按需请求相机和读取权限，拒绝后给出友好提示
- **AI 看图识别**：发送图片时，将图片以 base64 格式发送给视觉 AI 模型，AI 能描述和分析图片内容并与用户互动

## Capabilities

### New Capabilities
- `attachment-picker`：输入框附件选择器（拍照、相册、文件）、媒体预览与发送
- `ai-vision`：将图片 base64 编码发送给支持视觉的 AI 模型（如 Qwen2.5-VL），AI 能识别图片内容并回复

### Modified Capabilities
（无）

## Impact

- **新增依赖**：`androidx.activity:activity-ktx`（已存在）、`androidx.documentfile`（AndroidX）
- **新增权限**：`CAMERA`（拍照）、`READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO`（Android 13+ 相册读取，旧版本降级为 `READ_EXTERNAL_STORAGE`）
- **新增组件**：`AttachmentPreview.kt`（预览气泡）、`AttachmentPickerDialog.kt`（选择菜单）、`AttachmentMessageBubble.kt`（附件消息气泡）
- **变更 ViewModel**：`ChatState` 新增 `pendingAttachment: Attachment?` 字段，`sendMessage` 支持携带附件发送
- **变更数据库**：`ChatHistory` 模型新增 `attachmentUri: String?` 字段
- **变更 UI**：`ChatScreen.kt` 新增附件预览区域及"+"按钮交互逻辑
- **变更 API 模型**：默认模型切换为 `Qwen/Qwen2.5-VL-7B-Instruct`（支持视觉识别），`ChatMessage` 支持 `content` 数组格式（文本+图片）
