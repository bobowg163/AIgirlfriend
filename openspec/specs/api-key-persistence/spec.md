# API Key Persistence Specification

## Purpose

为 AI 女友应用提供 API Key 本地持久化能力，使首次输入后自动保存，避免用户每次启动应用重复输入。

## Requirements

### Requirement: API Key 持久化存储
系统 SHALL 将用户输入的 API Key 持久化存储到本地 DataStore，应用重启后自动加载已保存的 API Key，无需用户重新输入。

#### Scenario: 首次输入 API Key 后自动保存
- **WHEN** 用户输入 API Key 并点击保存按钮
- **THEN** 系统 SHALL 将 API Key 保存到本地 DataStore，并在后续应用启动时自动加载

#### Scenario: 应用重启后自动加载 API Key
- **WHEN** 用户关闭并重新打开应用
- **THEN** 系统 SHALL 自动加载上次保存的 API Key，直接进入聊天界面，无需再次输入

#### Scenario: 用户清空 API Key
- **WHEN** 用户在设置中点击清除 API Key
- **THEN** 系统 SHALL 清除本地存储的 API Key，下次启动时回到输入界面

#### Scenario: API Key 为空时的降级处理
- **WHEN** 本地未保存任何 API Key
- **THEN** 系统 SHALL 使用内置默认 API Key，无需用户手动输入
