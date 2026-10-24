## Context

应用已有 `WeatherTool.kt` 的工具类模式（OkHttpClient + suspend fun + Dispatchers.IO），`ChatViewModel` 通过 `detectWeatherQuery` 检测关键词并调用工具。新需求遵循相同架构模式。

## Goals / Non-Goals

**Goals:**
- 获取并缓存用户城市位置，注入天气等位置相关的对话上下文
- 支持快递单号识别与物流查询，以自然语言回复用户
- 权限请求失败时优雅降级，不影响核心聊天功能

**Non-Goals:**
- 不实现实时 GPS 轨迹追踪（仅需城市级别位置）
- 不实现快递下单/购买功能
- 不支持国际快递查询

## Decisions

### D1: 定位方式 — FusedLocationProviderClient vs LocationManager

选择 `FusedLocationProviderClient`（Google Play Services），原因：
- 自动融合 GPS/网络/WiFi，功耗更低
- Android 13+ 兼容性好，API 简洁
- 与 `WeatherTool` 已有 OkHttpClient 架构一致

备选方案：`LocationManager` — 更底层但代码冗余，Google 推荐 FusedLocation。

### D2: 逆地理编码 — 离线城市缓存 vs 在线 Geocoding API

选择**离线城市缓存**：将中国主要城市坐标预置在 `LocationTool.kt`（类似 `WeatherTool.kt` 的 `cityCoordinates`），通过最近距离匹配城市名。

原因：
- 无需额外 API Key，不增加依赖
- 离线可用，响应速度快（<100ms）
- 覆盖 50+ 主要城市，满足日常需求

备选方案：高德/百度 Geocoding API — 需注册 API Key，有调用限额。

### D3: 快递查询 API — 免费聚合 API vs 自建中转

选择免费聚合 API：`https://www.kuaidi100.com` 或 `https://api.oick.cn/express/query`（免费额度），通过 `ExpressTool.kt` 封装。

原因：
- 无需注册，开箱即用
- 支持中通、圆通、顺丰、韵达等主流快递
- 响应格式统一 JSON

备选方案：自建中转服务 — 需后端部署，超出本项目范围。

### D4: 位置缓存持久化

使用已有 `PreferencesManager`（DataStore）缓存城市名，与 API Key 存储方式一致。

## Risks / Trade-offs

- [离线城市缓存覆盖有限] → 只覆盖 50+ 城市，小城市将回退到北京。可在后续迭代扩充城市列表。
- [快递 API 稳定性] → 免费 API 可能有调用限额或宕机。增加超时（10s）和友好降级提示。
- [定位权限拒绝] → 权限被拒后无法获取位置。UI 显示"无法定位"提示，其他功能正常。

## Migration Plan

1. 新增 `LocationTool.kt`、`ExpressTool.kt` 工具类
2. 更新 `ChatViewModel` 增加位置检测和快递查询方法
3. 更新 `AndroidManifest.xml` 添加定位权限声明
4. 更新 `ChatScreen.kt` 顶部状态栏显示位置
5. 灰度发布，收集定位和快递查询成功率日志

回滚：删除新工具类，回退 ViewModel 和 UI 变更，权限声明不影响已有功能。

## Open Questions

无。所有关键决策已在设计阶段确定。
