## Context

当前 `ChatScreen.kt` 的 `InputBar` 包含文本输入框、语音按钮和发送按钮，`ChatHistory` 模型仅包含文本消息。`ChatViewModel` 通过 `repository.insertMessage()` 存储消息。应用使用 Compose BOM 2026.02.01，已有 `ActivityResultContracts.RequestPermission` 等模式。

## Goals / Non-Goals

**Goals:**
- 在输入框左侧添加"+"按钮，弹出拍照/相册/文件选择菜单
- 选中媒体后显示预览，支持删除和发送
- 图片和文件在聊天中以气泡形式展示
- 正确处理 Android 13+ 媒体权限（`READ_MEDIA_IMAGES`/`READ_MEDIA_VIDEO`）

**Non-Goals:**
- 不支持语音附件（已有独立语音输入功能）
- 不支持多人文件传输或大文件上传（本地 SQLite 存储，无网络上传）
- 不支持图片压缩（存储原始 URI）
- 不支持视频在线播放（仅展示缩略图和文件名）

## Decisions

### D1: 附件预览位置 — 输入框上方 vs 独立区域

选择**输入框上方内联预览**：在 `InputBar` 的 `Surface` 内部、`Row` 上方添加预览区域。

原因：
- 符合微信等主流 IM 的交互模式
- 预览与输入框在同一视觉区块，用户操作直觉清晰
- 避免新增页面布局复杂度

备选方案：全屏预览层 — 体验更好但实现复杂，超出本次范围。

### D2: 媒体 URI 处理 — Content URI vs File URI

选择**Content URI + FileProvider**：通过 `Uri` 传递媒体路径，发送时拷贝到应用内部缓存目录并持久化存储。

原因：
- 安全且符合 Android 10+ 分区存储要求
- `ContentResolver` 可读取任意 URI 的内容
- 避免 `READ_EXTERNAL_STORAGE` 旧权限依赖

备选方案：直接存 `Uri.toString()` — 外部 URI 可能在 app 重启后失效，不可靠。

### D3: 附件状态管理 — State 内联 vs ViewModel

选择**ChatScreen 本地状态 + ViewModel 发送**：预览状态（选中附件、删除、清空）在 `ChatScreen` 中用 `remember` 管理，`sendMessage` 时将附件 URI 传给 `ChatViewModel`。

原因：
- 预览交互（删除、取消）是纯 UI 行为，不需要 ViewModel 介入
- ViewModel 只负责最终发送，职责清晰
- 与现有 `inputText` 管理模式一致

### D4: 文件类型检测 — MIME type vs 文件扩展名

选择 **MIME type + 文件扩展名双重检测**：用 `ContentResolver.getType(uri)` 获取 MIME type 判断是否为图片/视频，同时用文件扩展名作为兜底。

## Risks / Trade-offs

- [临时文件生命周期] → 使用应用内部缓存目录（`cacheDir`）存储，应用卸载时自动清理。发送后保留在 chat 消息中供展示。
- [大文件 OOM] → 图片缩略图使用 `BitmapFactory.Options.inSampleSize` 压缩显示；视频仅存缩略图，不缓存完整视频。
- [Android 13+ 权限碎片化] → 同时注册 `READ_MEDIA_IMAGES` 和 `READ_MEDIA_VIDEO` 权限请求，低版本降级到 `READ_EXTERNAL_STORAGE`。

## Migration Plan

1. 扩展 `ChatHistory` 模型新增 `attachmentUri` 字段，升级 SQLite schema（`onUpgrade` 迁移）
2. 在 `ChatScreen.kt` 中新增附件选择器逻辑（`registerForActivityResult` 注册）
3. 新增 `AttachmentPreview.kt`、`AttachmentPickerDialog.kt`、`AttachmentMessageBubble.kt`
4. 更新 `InputBar` 接受 `pendingAttachment` 参数
5. 更新 `ChatViewModel.sendMessage` 支持附件发送
6. 灰度测试：不同 Android 版本、相机/相册/文件三种来源

回滚：删除新增组件，回退 `ChatHistory` schema 和 `sendMessage` 逻辑，SQLite schema 升级不可逆但旧数据不受影响。

## Open Questions

无。

### D5: 图片识别 — Vision API 格式

选择 **OpenAI 兼容 Vision 格式**：将图片 base64 编码为 data URI（`data:image/jpeg;base64,...`），
通过 `content` 数组格式发送给模型（`type: "image_url"` + `type: "text"`）。

原因：
- ModelScope API 兼容 OpenAI 接口格式，支持 vision 模型
- 使用 `Qwen/Qwen2.5-VL-7B-Instruct` 模型，支持中文图片理解
- base64 编码无需额外上传图片服务，纯本地处理

备选方案：上传图片到图床返回 URL — 需额外后端服务，超出范围。
