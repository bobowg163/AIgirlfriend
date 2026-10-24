package com.example.aigirlfriend.data.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 东方财富「妙想」金融数据工具。
 *
 * 职责：
 * - 金融意图检测（detectMxFinanceQuery）：命中时返回选中的 MCP 工具名，未命中返回 null
 * - 金融数据查询（queryFinance）：透传用户原话给妙想 MCP 工具，把结果格式化为「要点 + 数据来源」纯文本
 *
 * 空结果 / 工具错误 / 网络失败都返回口语化提示文案，不向 AI 透传原始 JSON。
 */
object MxFinanceTool {

    private const val TAG = "MxFinanceTool"

    private const val TOOL_ASHARE = "mx_ashare_finance_data"
    private const val TOOL_US = "mx_us_finance_data"
    private const val TOOL_HK = "mx_hk_finance_data"
    private const val TOOL_FUND = "mx_fund_finance_data"
    private const val TOOL_BOND = "mx_bond_finance_data"
    private const val TOOL_INDEX = "mx_index_block_finance_data"
    private const val TOOL_MACRO = "mx_macro_data"
    private const val TOOL_NEWS = "mx_finance_search_news"
    private const val TOOL_NOTICE = "mx_finance_search_notice"
    private const val TOOL_COMPREHENSIVE = "mx_comprehensive_finance_data"

    /** 金融意图关键词（保守词表，避免误伤普通聊天词） */
    private val financeKeywords = listOf(
        "股价", "股价多少", "涨幅", "跌幅", "涨停", "跌停", "市盈率", "市值",
        "财报", "大盘", "股指", "沪指", "深指", "创业板", "科创板",
        "沪深", "上证", "恒生", "纳斯达克", "纽交所", "道琼斯",
        "分红", "股息", "K线", "行情", "股价波动",
        "GDP", "CPI", "PPI", "PMI", "M2", "LPR", "降息", "加息", "货币政策", "美联储", "央行",
        "财经新闻", "财经", "股票", "股市", "个股", "券商", "基金", "债券", "美股", "港股", "财报新闻",
        "A股", "a股", "北交所", "北证", "公告"
    )

    /** 资产类别 → 工具路由表（顺序敏感：先具体后泛化） */
    private val toolRoutes: List<Pair<List<String>, String>> = listOf(
        listOf("美股", "纳斯达克", "纽交所", "道琼斯", "标普") to TOOL_US,
        listOf("港股", "恒生") to TOOL_HK,
        listOf("基金", "ETF", "etf", "货币基金") to TOOL_FUND,
        listOf("债券", "国债", "可转债") to TOOL_BOND,
        listOf("大盘", "沪指", "深证", "沪深", "上证", "创业板", "科创板", "北证", "A股", "a股", "股指", "指数") to TOOL_INDEX,
        listOf("GDP", "CPI", "PPI", "PMI", "M2", "LPR", "降息", "加息", "货币政策", "美联储", "央行", "宏观") to TOOL_MACRO,
        listOf("公告") to TOOL_NOTICE,
        listOf("财经新闻", "财经", "股市", "新闻") to TOOL_NEWS,
        listOf("股票", "个股", "行情", "市盈率", "市值", "分红", "股息", "涨停", "跌停", "财报", "股价") to TOOL_ASHARE
    )

    /** 金融意图检测：命中返回选中的 MCP 工具名，未命中返回 null */
    fun detectMxFinanceQuery(message: String): String? {
        if (message.isBlank()) return null

        val matchedKeyword = financeKeywords.firstOrNull { message.contains(it) }
        if (matchedKeyword == null) {
            Log.d(TAG, "未命中金融意图: '$message'")
            return null
        }

        val tool = toolRoutes.firstOrNull { (kws, _) -> kws.any { message.contains(it) } }?.second
            ?: TOOL_COMPREHENSIVE
        Log.d(TAG, "金融意图: 关键词='$matchedKeyword' → 工具=$tool")
        return tool
    }

    /** 选中 MCP 工具，透传用户原话为 query，调用并格式化结果 */
    suspend fun queryFinance(userMessage: String, apiKey: String, tool: String? = null): String =
        withContext(Dispatchers.IO) {
            val selectedTool = tool ?: detectMxFinanceQuery(userMessage)
            if (selectedTool == null) {
                return@withContext "暂未识别到具体的金融查询意图，可以换个问法再试试～"
            }

            val result = MxMcpClient.callTool(apiKey, selectedTool, userMessage)

            if (!result.isOk) {
                Log.w(TAG, "MCP 查询失败: ${result.errorMessage}")
                return@withContext "📉 这个金融数据小薇暂时查不到呢，晚点再问问好吗？"
            }

            formatFinanceResult(selectedTool, userMessage, result.text)
        }

    /** 结果格式化为「要点 + 数据来源标注」纯文本，不透传原始 JSON 给 AI */
    private fun formatFinanceResult(tool: String, userMessage: String, rawText: String): String {
        // 1. 空数据
        val dataEmpty = try {
            val json = JSONObject(rawText)
            val data = json.optJSONArray("data")
            (data == null || data.length() == 0) ||
                rawText.isBlank()
        } catch (e: Exception) {
            false
        }
        if (dataEmpty) {
            return "📉 关于「$userMessage」，妙想那边暂时没查到有效数据呢，换个问法或晚点再试试好吗？"
        }

        // 2. 有效数据：截断超长内容，避免撑爆 system prompt
        val text = rawText.trim()
        val preview = if (text.length > 3000) text.take(3000) + "……（数据较长，已截断）" else text
        return "（来源：东方财富妙想 · $tool）\n$preview"
    }
}
