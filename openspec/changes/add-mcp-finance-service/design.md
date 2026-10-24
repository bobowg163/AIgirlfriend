# Design

## Context

- 现有工具模式：`WeatherTool` / `ExpressTool` 是 `data/tools/` 下的 Kotlin object，提供 `detectXxxQuery(message)` 关键词检测 + 执行查询 + 返回格式化字符串；`ChatViewModel.sendMessage()` 在请求 AI 前依次检测天气/快递意图，把结果以「【天气信息】/【快递查询结果】」段注入 system prompt。
- 现有网络栈：OkHttp（已依赖），JSON 用 `org.json`（工具层）与 Gson（Retrofit 层）。
- 妙想 MCP Server 已验证（2026-10-24）：`https://mxapi.eastmoney.com/mxds/mcp`，StreamableHttp + JSON-RPC 2.0；`initialize` 与 `tools/list`、`tools/call` 均可免会话（响应无 `Mcp-Session-Id`，无需 session 续传）；认证头 `em_api_key: <KEY>`，Accept 需含 `text/event-stream`。
- API Key 管理参照 `PreferencesManager`（DataStore Preferences，Key 名 `mx_api_key`）。

## Goals / Non-Goals

**Goals:**
- 应用内直连妙想 MCP Server，按用户自然语言金融问题选择合适的金融数据工具并取回结果
- 结果注入 system prompt，由 AI（Qwen）以女朋友口吻自然表达，与天气/快递工具体验一致
- API Key 本地持久化、未配置/失败均有友好降级
- 不引入新第三方 MCP SDK（OkHttp 足够）

**Non-Goals:**
- 不做 MCP 工具列表的动态拉取/热更新（工具集在实现时写死映射表）
- 不支持用户在对话中手动指定调用哪个 MCP 工具
- 不做 MCP 结果缓存/离线
- 不改动天气、快递、语音、附件等现有工具的行为

## Decisions

### 1. 客户端：OkHttp 直连 JSON-RPC，不走 MCP SDK
- **选择**：新建 `MxMcpClient`（Kotlin object），`POST https://mxapi.eastmoney.com/mxds/mcp`，body 为 JSON-RPC 2.0 `tools/call`，header 带 `em_api_key` 与 `Accept: application/json, text/event-stream`；响应可能是 JSON 或 SSE（`data: {...}` 行），解析时兼容两种（取 `data:` 后的 JSON 行）。
- **理由**：项目已有 OkHttp + org.json；MCP StreamableHttp 本质是 HTTP POST JSON-RPC，SDK（如 mcp-java）会带入 Kotlin/Java 依赖链与版本管理成本，且工具集固定、协议简单。
- **备选**：引入 `io.modelcontextprotocol` SDK（更完整但依赖重、API 面向 JVM）；`mcp-remote` 代理（需要 Node 环境，Android 不可行）。

### 2. 工具选择：静态意图映射表
- **选择**：`MxFinanceTool` 内置「资产类别关键词 → 工具名」映射：
  - A股（股票名/代码、股价、涨幅、市盈率、分红、财报…）→ `mx_ashare_finance_data`
  - 基金 → `mx_fund_finance_data`；债券 → `mx_bond_finance_data`；港股 → `mx_hk_finance_data`；美股 → `mx_us_finance_data`；指数/板块 → `mx_index_block_finance_data`；宏观经济（GDP、CPI、利率）→ `mx_macro_data`；新闻 → `mx_finance_search_news`；公告 → `mx_finance_search_notice`
  - 兜底（金融词命中但类别不明）→ `mx_comprehensive_finance_data`
- **理由**：当前 11 个工具边界清晰；关键词路由比让 LLM 选工具更快、零额外 token 成本、行为可测。
- **备选**：用 LLM function-calling 动态选工具（更灵活但多一次 AI 请求、成本高）；`tools/list` 动态拉取（服务端工具变更后自动适配，代价是启动复杂度）。

### 3. 查询内容：直接透传用户原话
- **选择**：把用户整条消息（或去掉寒暄后的核心句）作为 `query` 参数直接发给妙想工具（其 `query` 字段本身接受自然语言）。
- **理由**：妙想工具支持自然语言问句（文档示例"格力电器的上市时间与最近5日的涨跌幅"），无需在 App 内做实体抽取。

### 4. 意图触发与结果注入：沿用天气/快递模式
- **选择**：`ChatViewModel.sendMessage()` 在检测天气/快递之后追加 `detectMxFinanceQuery(userMessage)`；命中且 Key 已配置时调用 `MxMcpClient`，得到格式化文本后以「【金融数据】…请根据以上信息以女朋友口吻回复」段拼入 system prompt。
- **理由**：与现有 `detectWeatherQuery`/`detectExpressQuery` 一致，改动面小、可观测（Log 同风格）。

### 5. Key 存储与优先级
- **选择**：`PreferencesManager` 新增 `MX_API_KEY = "mx_api_key"`；`StartScreen` 增加第二个输入项「妙想 API Key（可选）」；`ChatState` 增加 `mxApiKey`。未配置时金融问题走「未配置」提示，其余功能不受影响。
- **理由**：与 ModelScope Key 同构；妙想 Key 是可选增强项，不应阻塞主聊天流程。

### 6. 超时与并发
- **选择**：`MxMcpClient` 使用独立 OkHttp 实例（connect 15s / read 60s / write 15s），工具调用在 `Dispatchers.IO` 上执行；同一时刻至多一个进行中的 MCP 请求（简单 `synchronized`/flow 标志即可，无需队列）。
- **理由**：MCP 查询可能较慢（文档建议 timeout 120s），与聊天主请求解耦避免互堵。

## Risks / Trade-offs

- **关键词路由误判**：金融词表过宽会误触发（如聊"利息"命中宏观）。缓解：词表保守 + 未命中时不触发；误触发最多多一次 MCP 请求。
- **MCP 协议演进**：若服务端要求 `Mcp-Session-Id` 会话续传，需在客户端补 `initialize` 握手。当前探测显示无 session 头，实现时保留 `initialize` 兜底路径（首次调用失败重试一次 initialize + tools/call）。
- **Key 泄露**：Key 走本地 DataStore（非加密）。与 ModelScope Key 同风险级别；隐私说明中注明。可选后续：EncryptedFile。
- **SSE 响应兼容**：Accept 双值时服务端可能返回 SSE；客户端解析必须兼容 JSON 与 `data:` 行两种，避免解析失败被误报为"查不到"。
- **工具数据为空**（如 `{"data":[]}`）：按"未查到"处理并给出口语化提示，不透传空 JSON 给 AI。
