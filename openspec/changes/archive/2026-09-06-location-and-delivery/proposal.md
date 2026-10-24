## Why

用户希望通过 AI 女友应用获得更实用的生活服务能力——实时定位自己和查询快递物流信息，让"小薇"不仅能聊天，还能提供贴心的生活助手服务。

## What Changes

- **定位功能**：获取用户当前地理位置（城市/地址），并在 AI 对话中自动使用，使天气等需要位置的服务更精准
- **快递查询功能**：支持用户输入快递单号，查询物流信息（快递公司、中转站、派送状态等）

## Capabilities

### New Capabilities
- `location-tracking`：获取并缓存用户位置，在对话中自动注入位置上下文
- `express-query`：支持快递单号查询，返回物流轨迹和状态信息

### Modified Capabilities
（无）

## Impact

- **新增依赖**：`androidx.lifecycle:lifecycle-location`（获取 GPS/网络定位）
- **新增权限**：`ACCESS_FINE_LOCATION`、`ACCESS_COARSE_LOCATION`（运行时请求）
- **新增数据层**：`LocationTool.kt`、`ExpressTool.kt`（类似现有 `WeatherTool.kt` 的工具类模式）
- **变更 ViewModel**：`ChatViewModel` 新增位置检测逻辑，快递关键词自动触发工具调用
- **变更 UI**：`ChatScreen` 顶部状态栏显示当前位置（仅城市名），权限状态指示
