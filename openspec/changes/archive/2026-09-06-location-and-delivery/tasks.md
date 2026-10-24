## 1. 依赖与权限准备

- [x] 1.1 在 `app/build.gradle.kts` 中添加 `com.google.android.gms:play-services-location` 依赖，验证 Gradle sync 成功
- [x] 1.2 在 `AndroidManifest.xml` 中添加 `ACCESS_FINE_LOCATION` 和 `ACCESS_COARSE_LOCATION` 权限声明，验证编译通过

## 2. 位置工具（LocationTool）

- [x] 2.1 创建 `data/tools/LocationTool.kt`，预置 50+ 中国主要城市坐标 Map（参照 `WeatherTool.kt` 的城市坐标结构）
- [x] 2.2 实现 `suspend fun resolveCity(): String`，使用 FusedLocationProviderClient 获取坐标，通过最近距离匹配返回城市名，失败时返回"北京"
- [x] 2.3 添加 `try/catch` 异常处理，日志输出城市解析结果，验证无崩溃

## 3. 快递工具（ExpressTool）

- [x] 3.1 创建 `data/tools/ExpressTool.kt`，封装快递查询 API（使用免费聚合 API，如 `https://api.oick.cn/express/query`）
- [x] 3.2 实现 `suspend fun queryTracking(trackingNo: String): String`，返回物流状态摘要（当前状态、最新中转站、预计送达时间）
- [x] 3.3 实现 `fun detectExpressQuery(message: String): Pair<String?, String>?`，从消息中提取快递关键词和单号，返回 `(快递公司, 单号)` 或 `null`
- [x] 3.4 添加超时（10s）和错误降级处理，失败时返回友好提示，验证日志输出

## 4. ViewModel 集成

- [x] 4.1 在 `ChatState` 中添加 `cachedCity: String? = null` 字段，验证 StateFlow 更新触发 UI 刷新
- [x] 4.2 在 `ChatViewModel` 中注入 `LocationTool`，初始化时异步获取缓存城市并存储到 DataStore（参照 `PreferencesManager` 模式）
- [x] 4.3 新增 `getExpressInfo(trackingNo: String)` suspend 方法，调用 `ExpressTool.queryTracking`
- [x] 4.4 修改 `detectWeatherQuery` 逻辑：优先使用缓存城市，未缓存时使用消息中提取的城市，最后降级为"北京"
- [x] 4.5 新增 `detectExpressQuery(message: String)` 方法，检测快递关键词和单号，返回物流信息字符串
- [x] 4.6 在 `sendMessage` 中集成快递查询：检测到快递意图时，并行获取位置和快递信息后注入系统提示词
- [x] 4.7 验证：发送"我的快递 1234567890 到哪了"时日志输出快递查询结果，发送"今天天气怎样"时日志输出缓存城市

## 5. UI 适配

- [x] 5.1 更新 `ChatScreen.kt` 顶部状态栏：当 `cachedCity` 非 null 时显示"📍 <城市名>"副标题，否则隐藏位置行
- [x] 5.2 在 `ChatViewModel` 中新增 `getLocationPermission()` 方法，使用 `ActivityResultContracts.RequestMultiplePermissions` 请求定位权限
- [x] 5.3 权限拒绝时显示 Toast 提示"需要定位权限才能使用位置相关功能"，不影响聊天
- [x] 5.4 验证：启动应用后顶部状态栏显示缓存城市，拒绝权限后状态栏不显示位置信息

## 6. 测试与验证

- [x] 6.1 运行 `./gradlew assembleDebug` 验证编译通过，生成 APK
- [x] 6.2 在设备上测试：授权定位后发送"今天天气怎样"，验证 AI 回复使用当前城市而非默认北京
- [x] 6.3 测试快递查询：API 验证成功，单号 753239647755884 返回中通物流信息（天津市签收）
- [x] 6.4 测试权限拒绝场景：拒绝定位权限后发送天气消息，验证降级为北京且不崩溃
- [x] 6.5 测试无单号场景：发送"我有个快递到哪了"，验证 AI 友好询问单号而非报错
- [x] 6.6 更新 `openspec/changes/location-and-delivery/tasks.md` 标记所有任务为完成

## 7. 设置弹窗清除聊天记录

- [x] 7.1 在 `ChatScreen.kt` 的设置弹窗底部新增"清除聊天记录"按钮（红色 TextButton），点击后弹出二次确认 `AlertDialog`
- [x] 7.2 确认按钮调用 `viewModel.clearChat()`，同时关闭设置弹窗；取消按钮仅关闭确认弹窗
- [x] 7.3 验证：点击设置图标 → 点击清除聊天记录 → 确认 → 聊天列表清空，重新打开应用后仍为空
