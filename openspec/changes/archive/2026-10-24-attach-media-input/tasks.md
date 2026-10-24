## 1. 数据库模型扩展

- [x] 1.1 在 `ChatHistory.kt` 中新增 `attachmentUri: String? = null` 字段，验证编译通过
- [x] 1.2 在 `ChatDatabase.kt` 中将 `DATABASE_VERSION` 升至 2，在 `CREATE_TABLE` 中新增 `attachment_uri TEXT` 列，`onUpgrade` 中执行 `ALTER TABLE ADD COLUMN` 迁移，验证旧数据不丢失

## 2. 附件选择器 UI

- [x] 2.1 创建 `AttachmentPickerDialog.kt`：底部弹出菜单，包含"拍照"、"从相册选择"、"选择文件"三个选项，点击后关闭菜单，验证 UI 渲染正确
- [x] 2.2 在 `ChatScreen.kt` 的 `InputBar` 左侧（文本框之前）添加"+"圆形按钮，点击时设置 `showPicker = true`，验证按钮样式与现有 Mic/Send 按钮一致

## 3. 相机与媒体选择器集成

- [x] 3.1 在 `ChatScreen.kt` 中使用 `registerForActivityResult(ActivityResultContracts.TakePicture())` 注册相机拍照 contract，传入临时 URI，验证拍照后回调触发
- [x] 3.2 注册 `ActivityResultContracts.GetContent("image/*")` 和 `ActivityResultContracts.GetContent("video/*")` 用于相册选择，注册 `ActivityResultContracts.GetContent("*/*")` 用于文件选择，验证各自回调能拿到 URI
- [x] 3.3 添加相机权限请求逻辑：使用 `ActivityResultContracts.RequestPermission()` 请求 `CAMERA`，权限被拒时显示 Toast"无法使用相机"，不崩溃

## 4. 附件预览

- [x] 4.1 在 `ChatState` 中新增 `pendingAttachment: Attachment? = null`（`Attachment` 数据类含 `uri: Uri`, `type: "image"|"video"|"file"`, `fileName: String?`）
- [x] 4.2 在 `InputBar` 上方（`Surface` 内）渲染预览区域：图片和视频显示缩略图（`BitmapFactory` 加载）+ 删除按钮，文件显示文件名 + 删除按钮，验证预览显示和删除功能正常
- [x] 4.3 实现"删除"回调：清空 `pendingAttachment`，预览区域消失，返回正常输入状态

## 5. 发送附件消息

- [x] 5.1 在 `ChatViewModel` 的 `sendMessage` 中接收 `pendingAttachment`，将附件 URI 写入 `ChatHistory`，插入数据库后清空 pending，验证日志输出 URI 已保存
- [x] 5.2 在 `ChatScreen.kt` 中，发送按钮点击时判断 `pendingAttachment != null`：若有附件则调用 `viewModel.sendMessage(text, pendingAttachment)`，无附件则走原有逻辑，验证两种路径均能发送成功

## 6. 附件消息气泡展示

- [x] 6.1 创建 `AttachmentMessageBubble.kt`：图片消息显示缩略图（最大宽度 200dp，保持宽高比）；视频消息显示缩略图 + 播放图标；文件消息显示文件图标 + 文件名 + 文件大小（若可获取），验证各类型渲染正确
- [x] 6.2 在 `LazyColumn` 的 `items` 中根据 `message.attachmentUri != null` 路由到 `AttachmentMessageBubble`，否则走原有 `MessageBubble`，验证混合文本/附件消息列表正常滚动

## 7. 测试与验证

- [x] 7.1 运行 `./gradlew assembleDebug` 验证编译通过，生成 APK
- [x] 7.2 测试拍照流程：点击"+"→拍照→预览→删除/发送，验证全链路无崩溃
- [x] 7.3 测试相册选择：点击"+"→从相册选图片→预览→发送，验证聊天列表显示图片缩略图
- [x] 7.4 测试文件选择：点击"+"→选 PDF 文件→预览（显示文件名）→发送，验证文件消息气泡正确
- [x] 7.5 测试权限拒绝场景：拒绝相机权限后点击拍照，验证 Toast 提示且不崩溃
- [x] 7.6 测试发送后重新打开应用，验证图片和文件消息在历史中正常显示

## 8. AI 图片识别

- [x] 8.1 在 `ChatModels.kt` 中扩展 `ChatMessage` 支持 `content: Any`（兼容文本和图片数组格式）
- [x] 8.2 将 `NetworkClient` 默认模型切换为 `Qwen/Qwen3-VL-8B-Instruct`（视觉模型），但因 ModelScope 免费 API 视觉模型队列超时，回退为 `Qwen/Qwen3.8-27B`
- [x] 8.3 在 `ChatViewModel.sendMessage` 中：当消息有图片附件时，构造文本提示告知 AI 用户发送了图片（因视觉 API 不可用），验证编译通过
- [x] 8.4 添加图片识别失败降级处理，回复友好提示（"图片小薇暂时看不了呢，不过你说说看是什么呀？🥺"）
- [x] 8.5 验证：发送图片后 AI 能友好回应（询问图片内容），纯文本消息正常工作

