## Purpose

为 AI 女友应用提供语音对话能力，支持语音输入（语音转文字发送消息）和语音输出（AI 回复文字转语音播放），增强语音交互体验。

## ADDED Requirements

### Requirement: 语音输入（STT）
系统 SHALL 提供语音输入功能，用户可通过按住语音按钮录制语音，系统将语音转换为文字后自动发送到聊天窗口。

#### Scenario: 按住录音松开发送
- **WHEN** 用户长按语音输入按钮，松开后停止录音
- **THEN** 系统 SHALL 将录音转换为文字并自动发送该条消息到聊天窗口

#### Scenario: 录音时长限制
- **WHEN** 用户持续按住语音按钮超过 60 秒
- **THEN** 系统 SHALL 自动停止录音并发送已录制的内容

#### Scenario: 录音权限未授予
- **WHEN** 用户未授予麦克风权限并尝试使用语音输入
- **THEN** 系统 SHALL 先检查权限状态，无权限时弹出系统权限申请对话框，用户拒绝后显示"需要麦克风权限才能使用语音输入"友好提示文字而非崩溃

#### Scenario: 录音权限被拒绝后仍可重试
- **WHEN** 用户之前拒绝了录音权限，再次点击语音按钮
- **THEN** 系统 SHALL 再次弹出权限申请，不直接禁用按钮，允许用户重新授权

#### Scenario: 语音识别失败降级
- **WHEN** 语音识别返回空结果或识别失败
- **THEN** 系统 SHALL 显示"未识别到内容，请重试"提示，不发送空消息

### Requirement: 语音输出（TTS）
系统 SHALL 在 AI 回复生成后提供语音播放功能，用户可选择播放 AI 回复的语音，TTS 引擎 SHALL 使用台湾国语（zh-TW）女声。AI 回复通过 ModelScope 推理平台（`https://api-inference.modelscope.cn/v1`，模型 `Qwen/Qwen3.8-27B`）获取。**AI 回复生成完成后 SHALL 自动播放语音，无需用户手动点击播放按钮。**

#### Scenario: 播放 AI 回复语音
- **WHEN** AI 回复生成完成后用户点击语音播放按钮
- **THEN** 系统 SHALL 使用中文女声 TTS 引擎播放该条回复文本，播放过程中角色切换至 speaking 动画

#### Scenario: AI 回复自动播放语音
- **WHEN** AI 回复生成完成
- **THEN** 系统 SHALL 自动使用 TTS 引擎播放 AI 回复文本，无需用户手动操作

#### Scenario: 语音播放中断
- **WHEN** 用户在语音播放过程中点击其他消息或发送新消息
- **THEN** 当前语音播放 SHALL 立即停止，角色动画恢复 idle

#### Scenario: 多条语音排队播放
- **WHEN** 用户连续点击多条 AI 回复的语音播放按钮
- **THEN** 系统 SHALL 停止当前播放并开始播放最新请求的语音，不排队

#### Scenario: TTS 引擎不可用降级
- **WHEN** 设备 TTS 引擎未安装或配置异常
- **THEN** 语音播放按钮 SHALL 置为不可点击状态，并显示"TTS 不可用"提示

### Requirement: 语音状态联动
语音输入和语音输出操作 SHALL 与角色动画状态联动，确保视觉反馈与音频状态一致。

#### Scenario: 语音输入触发 listening 动画
- **WHEN** 用户开始语音录音
- **THEN** 角色动画 SHALL 切换为 listening 状态，录音停止后恢复 idle

#### Scenario: 语音播放触发 speaking 动画
- **WHEN** TTS 开始播放 AI 回复
- **THEN** 角色动画 SHALL 切换为 speaking 状态，播放结束恢复 idle

### Requirement: 语音发送快捷方式
系统 SHALL 支持通过语音输入直接发送消息，无需用户在输入框中编辑文字。

#### Scenario: 语音消息标记
- **WHEN** 消息通过语音输入发送
- **THEN** 该消息气泡 SHALL 显示语音图标标识，与文字消息视觉上可区分
