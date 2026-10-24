## Purpose

为 AI 女友应用提供图片识别能力，用户发送图片后，AI 能将图片编码为 base64 并发送给视觉模型（如 Qwen2.5-VL），识别图片内容后以女朋友口吻回复。

## ADDED Requirements

### Requirement: 图片内容识别
系统 SHALL 在用户发送图片消息时，将图片读取并编码为 base64，以 OpenAI Vision 兼容格式发送给支持视觉的 AI 模型，接收图片描述后以自然语言回复用户。

#### Scenario: 用户发送图片
- **WHEN** 用户发送包含图片附件的消息（无文字或附文字）
- **THEN** 系统 SHALL 将图片读取为 base64 data URI，以 `content` 数组格式（`type: "image_url"` + `type: "text"`）发送给视觉 AI 模型

#### Scenario: AI 识别图片后回复
- **WHEN** AI 返回图片描述内容
- **THEN** 系统 SHALL 将 AI 描述以女朋友口吻回复用户，如"哇，这张照片好漂亮呀～"

#### Scenario: 图片识别失败
- **WHEN** 图片编码失败或视觉 API 返回错误
- **THEN** 系统 SHALL 回复友好提示（如"图片小薇暂时看不了呢，不过你说说看是什么呀？🥺"），不中断对话

#### Scenario: 无图片时正常对话
- **WHEN** 用户仅发送文字消息（无附件）
- **THEN** 系统 SHALL 走原有文本对话流程，不发送图片编码，与视觉功能互不影响

### Requirement: 视觉模型配置
系统 SHALL 使用支持视觉的 AI 模型（如 `Qwen/Qwen2.5-VL-7B-Instruct`）处理含图片消息的请求。

#### Scenario: 使用视觉模型
- **WHEN** 用户发送带图片的消息
- **THEN** 系统 SHALL 使用视觉模型（Qwen2.5-VL）而非纯文本模型处理该请求

#### Scenario: 降级使用文本模型
- **WHEN** 视觉模型请求失败（网络错误或模型不支持）
- **THEN** 系统 SHALL 降级到文本模型，回复图片不可用提示，不崩溃
