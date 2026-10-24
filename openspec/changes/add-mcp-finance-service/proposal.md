# Proposal

## Why

小薇目前只能聊天、看图片、查天气和查快递，缺少金融/股票类问答能力。东方财富「妙想」数据服务已提供 MCP Server（StreamableHttp），通过 API Key 可查询 A股/港股/美股行情、财务与估值、基金、债券、指数、财经新闻与公告、宏观数据等。接入该服务可让小薇用自然语言回答股票、基金、大盘和财经新闻问题，扩展陪伴的实用性。

## What Changes

- 新增 MCP 客户端能力：应用内直连东方财富妙想 MCP Server（`https://mxapi.eastmoney.com/mxds/mcp`，StreamableHttp，`em_api_key` 请求头认证），通过 JSON-RPC 调用 `tools/call` 执行金融数据查询工具
- 新增妙想 API Key 配置：欢迎页提供输入项，Key 存入本地 DataStore（与现有 ModelScope API Key 相同的本地持久化方式），不上传、不写入 git
- 聊天意图扩展：用户消息命中金融意图关键词（如股价、涨跌幅、基金、财报、大盘等）时，自动调用合适的 MCP 工具（如 `mx_ashare_finance_data`、`mx_fund_finance_data`、`mx_comprehensive_finance_data`、`mx_finance_search_news`），把查询结果注入系统提示词，让 AI 以女朋友口吻自然回复
- 降级行为：Key 未配置、网络失败或工具报错时，回复友好提示（如"这个数据小薇暂时查不到呢"），不中断对话

## Capabilities

### New Capabilities

- `mx-mcp-finance`: 东方财富妙想 MCP 金融数据服务 — MCP 客户端调用、工具选择、API Key 配置与持久化、金融意图检测与降级

### Modified Capabilities

- （无现有 capability 的需求级变更；天气/快递工具流程保持原样）

## Impact

- 新增代码：`data/tools/MxMcpClient.kt`（MCP JSON-RPC 客户端）、`data/tools/MxFinanceTool.kt`（金融意图检测 + 工具选择 + 结果格式化）
- 修改代码：`PreferencesManager.kt`（新增 MX API Key 存取）、`StartScreen.kt`（新增 Key 输入项）、`ChatViewModel.kt`（发送消息时检测金融意图并注入 MCP 查询结果）
- 依赖：无新增第三方依赖（MCP StreamableHttp 走现有 OkHttp；JSON 用现有 org.json/gson）
- 网络：新增出站请求到 `mxapi.eastmoney.com`；隐私说明需注明妙想 Key 仅存本地
- 规格：新增 `specs/mx-mcp-finance/spec.md`（delta）
