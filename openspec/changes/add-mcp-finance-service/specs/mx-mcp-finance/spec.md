# Spec Delta

## Purpose

为 AI 女友应用接入东方财富「妙想」MCP 金融数据服务，使小薇能通过自然语言查询 A股/港股/美股行情、基金、债券、指数、财经新闻与公告等金融信息，并以女朋友口吻回复用户。

## ADDED Requirements

### Requirement: 妙想 MCP 金融数据查询
系统 SHALL 在用户消息命中金融意图（如股价、涨跌幅、基金、财报、大盘、财经新闻等关键词）时，通过 MCP 协议调用东方财富妙想 MCP Server（StreamableHttp，JSON-RPC）的金融数据工具，将查询结果注入 AI 回复上下文。

#### Scenario: 用户询问股票行情
- **WHEN** 用户发送包含股票名称或代码及行情意图的消息（如"贵州茅台今天股价多少"、"宁德时代最近涨跌幅"）
- **THEN** 系统 SHALL 调用 A股金融数据工具（`mx_ashare_finance_data`）以用户原话或改写后的自然语言查询发送请求，并把返回的数据以女朋友口吻自然回复

#### Scenario: 用户询问基金/债券/美股等其他资产
- **WHEN** 用户询问基金、债券、港股、美股、指数板块、宏观经济或财经新闻/公告
- **THEN** 系统 SHALL 选择对应的妙想工具（如 `mx_fund_finance_data`、`mx_bond_finance_data`、`mx_us_finance_data`、`mx_hk_finance_data`、`mx_index_block_finance_data`、`mx_macro_data`、`mx_finance_search_news`、`mx_finance_search_notice`）发起查询并回复

#### Scenario: 金融数据查询成功
- **WHEN** MCP 工具返回有效数据
- **THEN** 系统 SHALL 将数据要点（价格、涨跌幅、新闻标题等）融入小薇的回复，语气保持温柔亲切，不输出原始 JSON

#### Scenario: 非金融问题正常对话
- **WHEN** 用户消息未命中金融意图关键词（普通聊天、天气、快递等）
- **THEN** 系统 SHALL 不触发金融 MCP 查询，走原有对话流程，天气/快递工具行为不受影响

### Requirement: 妙想 API Key 本地配置
系统 SHALL 允许用户在应用内配置东方财富妙想服务的 API Key，并本地持久化保存，Key 不上传到任何服务器、不写入 git 仓库。

#### Scenario: 首次配置妙想 Key
- **WHEN** 用户在设置中填入妙想 API Key（`em_` 前缀）并保存
- **THEN** 系统 SHALL 将 Key 保存到本地存储（与 ModelScope API Key 相同的本地持久化机制），之后启动无需重新输入

#### Scenario: 未配置妙想 Key 时触发金融查询
- **WHEN** 用户未配置妙想 API Key 却发送金融类问题
- **THEN** 系统 SHALL 回复友好提示（如"妙想数据服务还没配置呢，设置里填一下 Key 就能查啦～"），不崩溃、不中断对话

### Requirement: 妙想查询失败降级
系统 SHALL 在 MCP 请求失败或工具返回错误时优雅降级，保持对话流畅。

#### Scenario: MCP 请求网络失败
- **WHEN** 妙想 MCP Server 请求超时、断网或认证失败
- **THEN** 系统 SHALL 回复友好提示（如"这个数据小薇暂时查不到呢，晚点再问问好吗？"），不中断对话，不弹出崩溃

#### Scenario: 工具返回错误
- **WHEN** MCP 工具调用返回错误（`isError` 为真或无有效数据）
- **THEN** 系统 SHALL 将"未查到"语义以女朋友口吻回复用户，可提示换个问法，不输出技术错误细节
