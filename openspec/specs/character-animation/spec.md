# Character Animation Specification

## Purpose

为 AI 女友应用提供角色立绘展示与动画状态切换能力，让用户在聊天时能看到小薇的视觉形象并感知其情绪状态变化。

## Requirements

### Requirement: 角色立绘展示
应用 SHALL 在聊天界面顶部区域展示小薇的角色立绘视频（本地资源 `raw/video.mp4`），视频 SHALL 占据聊天界面约 40% 的屏幕高度，并支持基础的响应式缩放以适应不同屏幕尺寸。**立绘视频 SHALL 通过底部内边距或顶部偏移将视频向下调整，确保角色头像完整可见，不被顶部裁切。**视频 SHALL 自动循环播放。

#### Scenario: 首次进入聊天界面显示立绘
- **WHEN** 用户已完成 API Key 设置并进入聊天界面
- **THEN** 界面顶部 SHALL 显示小薇角色立绘，立绘下方显示聊天消息区域

#### Scenario: 立绘随屏幕尺寸适配
- **WHEN** 应用在横向或不同尺寸屏幕上运行
- **THEN** 角色立绘 SHALL 自动缩放并保持视觉比例，不出现变形或裁切异常

#### Scenario: 立绘视频位置调整露出头像
- **WHEN** 角色立绘视频在界面中渲染
- **THEN** 视频 SHALL 向下偏移或以底部对齐方式展示，确保角色头像完整可见，不被顶部裁切

#### Scenario: 视频自动循环播放
- **WHEN** 角色区域渲染完成
- **THEN** 视频 SHALL 自动开始播放并循环，用户无需任何操作

### Requirement: 动画状态切换
系统 SHALL 根据对话状态自动切换角色动画，定义以下四种动画状态：idle（待机）、thinking（思考中）、listening（聆听/语音输入中）、speaking（说话/语音播放中）。

#### Scenario: 等待用户输入时显示 idle 动画
- **WHEN** 无消息请求进行中且未处于语音输入或播放状态
- **THEN** 角色 SHALL 播放 idle 待机动画

#### Scenario: AI 正在生成回复时显示 thinking 动画
- **WHEN** 用户发送消息后系统正在请求 AI 响应
- **THEN** 角色 SHALL 切换为 thinking 思考动画，AI 返回回复后恢复 idle

#### Scenario: 语音输入中显示 listening 动画
- **WHEN** 用户点击语音按钮并开始录音
- **THEN** 角色 SHALL 切换为 listening 聆听动画，录音停止后恢复 idle

#### Scenario: 语音播放中显示 speaking 动画
- **WHEN** AI 回复文字转语音并开始播放
- **THEN** 角色 SHALL 切换为 speaking 说话动画，播放结束后恢复 idle

### Requirement: 动画状态可见性
用户 SHALL 能够通过界面清晰感知当前角色所处的动画状态，动画状态 SHALL 伴随视觉指示（如状态标签或图标）展示在角色立绘区域。

#### Scenario: 显示当前动画状态标签
- **WHEN** 角色处于非 idle 动画状态
- **THEN** 角色立绘区域 SHALL 显示对应的状态文字标签（如"思考中"、"聆听中"、"说话中"）

### Requirement: 动画资源管理
系统 SHALL 支持加载本地资源动画（帧动画或动画资源文件），并在内存中缓存动画资源以避免重复加载。

#### Scenario: 动画资源加载失败降级处理
- **WHEN** 动画资源文件缺失或加载失败
- **THEN** 系统 SHALL 降级为静态立绘展示，不抛出崩溃异常，仅记录错误日志
