# Tasks

## 1. 妙想 API Key 配置

- [x] 1.1 `PreferencesManager` 新增 `MX_API_KEY = "mx_api_key"` 的 `getMxApiKey()/saveMxApiKey()/clearMxApiKey()`（与 ModelScope Key 同构）
- [x] 1.2 `ChatState` 增加 `mxApiKey: String?` 字段；`ChatViewModel.init` 中加载已存 Key
- [x] 1.3 `StartScreen` 增加「妙想 API Key（可选）」输入项与保存按钮，填入后写入 DataStore

## 2. MCP 客户端

- [x] 2.1 新建 `data/tools/MxMcpClient.kt`：独立 OkHttp 实例（connect 15s/read 60s/write 15s），`POST https://mxapi.eastmoney.com/mxds/mcp`，header `em_api_key` + `Accept: application/json, text/event-stream`，body 为 JSON-RPC 2.0 `tools/call`
- [x] 2.2 响应解析兼容 JSON 与 SSE（`data: {...}` 行）两种格式；返回 `McpToolResult(text, isError)`
- [x] 2.3 首次调用失败时兜底重试一次：先发 `initialize` 再 `tools/call`
- [x] 2.4 失败/超时/401/403 统一返回友好错误标记，不抛出未捕获异常

## 3. 金融意图检测与工具选择

- [x] 3.1 新建 `data/tools/MxFinanceTool.kt`：资产类别关键词 → 工具名静态映射表（A股→`mx_ashare_finance_data`、基金→`mx_fund_finance_data`、债券→`mx_bond_finance_data`、港股→`mx_hk_finance_data`、美股→`mx_us_finance_data`、指数/板块→`mx_index_block_finance_data`、宏观→`mx_macro_data`、新闻→`mx_finance_search_news`、公告→`mx_finance_search_notice`、兜底→`mx_comprehensive_finance_data`）
- [x] 3.2 `detectMxFinanceQuery(message): Boolean` 金融意图检测（词表保守：股价、涨幅、市盈率、基金、财报、大盘、股指、股息、GDP、CPI、新闻、公告等；避免误伤普通聊天词）
- [x] 3.3 `queryFinance(userMessage, apiKey): String` 选择工具、透传用户原话为 `query`、调用 `MxMcpClient`、解析 `content[0].text`；`data:[]` 空结果按"未查到"返回口语化文案
- [x] 3.4 结果格式化为「要点 + 数据来源标注」纯文本（禁止把原始 JSON 塞给 AI）

## 4. 聊天流程集成

- [x] 4.1 `ChatViewModel.sendMessage()` 在天气/快递检测后追加金融检测：命中且 `mxApiKey` 非空时调用 `MxFinanceTool.queryFinance`，把结果以「【金融数据】…请根据以上信息以女朋友口吻回复」段拼入 system prompt
- [x] 4.2 命中但 Key 未配置：不发起请求，AI 收到「妙想服务未配置」提示段（或直接在回复层给引导文案），不崩溃
- [x] 4.3 查询失败/超时/未查到：注入「未查到」提示段，AI 以女朋友口吻安抚回复，不中断对话

## 5. 验证

- [ ] 5.1 本地真机/模拟器：配置 Key 后问"贵州茅台今天股价"、"沪深300最近表现"、"今天财经新闻"，确认触发对应工具且回复自然
- [ ] 5.2 未配置 Key 时问金融问题，确认出现引导配置提示而非崩溃
- [ ] 5.3 断网/错误 Key 场景确认降级提示正常
- [ ] 5.4 回归：天气、快递、语音、附件流程行为不变
- [x] 5.5 `./gradlew :app:assembleDebug` 编译通过
