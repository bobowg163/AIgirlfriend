package com.example.aigirlfriend.data.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object ExpressTool {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val expressKeywords = listOf("快递", "物流", "到哪了", "包裹", "发货", "派送", "转运", "单号")

    suspend fun queryTracking(trackingNo: String, company: String? = null): String = withContext(Dispatchers.IO) {
        try {
            val companyCode = company ?: detectCompany(trackingNo)
            val url = "https://www.kuaidi100.com/query?type=${companyCode}&postid=${trackingNo}"
            Log.d("ExpressTool", "查询快递: $trackingNo, 公司: $companyCode")

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .header("Accept", "*/*")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Referer", "https://www.kuaidi100.com/")
                .build()
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return@withContext "暂时查不到这个快递的信息呢，宝宝再确认一下单号~"
            Log.d("ExpressTool", "API Response: $body")

            val json = JSONObject(body)
            val status = json.optString("status", "")
            val result = json.optInt("result", -1)
            val message = json.optString("message", "")
            val state = json.optString("state", "")

            // status="200" and result=1 都表示成功，result=0 也可能成功（有新物流时）
            if (status != "200" && result != 1) {
                Log.w("ExpressTool", "快递查询返回失败: status=$status, result=$result, msg=$message")
                // 尝试不带 company 的查询
                val altUrl = "https://www.kuaidi100.com/query?type=auto&postid=${trackingNo}"
                val altResponse = client.newCall(Request.Builder().url(altUrl).header("User-Agent", "Mozilla/5.0").build()).execute()
                val altBody = altResponse.body?.string() ?: ""
                Log.d("ExpressTool", "备用API响应: $altBody")
                val altJson = JSONObject(altBody)
                val altStatus = altJson.optString("status", "")
                val altData = altJson.optJSONArray("data")
                if (altStatus == "200" && altData != null && altData.length() > 0) {
                    return@withContext formatResult(trackingNo, altData)
                }
                return@withContext "暂时查不到这个快递的信息呢，宝宝再确认一下单号~"
            }

            val data = json.optJSONArray("data")
            if (data == null || data.length() == 0) {
                return@withContext "没找到这个快递的物流信息哦，可能单号有误呢~"
            }
            formatResult(trackingNo, data)
        } catch (e: Exception) {
            Log.e("ExpressTool", "快递查询异常: ${e.message}", e)
            "暂时查不到这个快递的信息呢，宝宝再确认一下单号~"
        }
    }

    private fun formatResult(trackingNo: String, data: org.json.JSONArray): String {
        val validItems = mutableListOf<org.json.JSONObject>()
        for (i in 0 until data.length()) {
            val item = data.getJSONObject(i)
            val context = item.optString("context", "").trim()
            // 跳过"查无结果"等无效条目
            if (context.isNotBlank() && context != "查无结果" && !context.contains("暂无")) {
                validItems.add(item)
            }
        }
        if (validItems.isEmpty()) {
            return "📦 快递 $trackingNo 暂未查到有效物流信息，可能单号有误或刚发货~"
        }
        val items = mutableListOf<String>()
        val maxItems = minOf(5, validItems.size)
        for (i in 0 until maxItems) {
            val item = validItems[i]
            val time = item.optString("time", "")
            val context = item.optString("context", "")
            val location = item.optString("location", "")
            val locationText = if (location.isNotBlank()) "[$location] " else ""
            items.add("$locationText$context（$time）")
        }
        return "📦 快递 $trackingNo 物流信息：\n" + items.joinToString("\n")
    }

    private fun detectCompany(trackingNo: String): String {
        val clean = trackingNo.trim().lowercase()
        return when {
            clean.startsWith("sf") -> "shunfeng"
            clean.startsWith("jd") -> "jd"
            clean.startsWith("jt") -> "jnt"
            clean.startsWith("em") || (trackingNo.startsWith("E") && trackingNo.length >= 13) -> "ems"
            clean.startsWith("yp") || trackingNo.startsWith("Y") || trackingNo.startsWith("y") -> "yuantong"
            // 韵达：RY/RYD/RYDDA/YD/YDDA 等前缀
            clean.startsWith("ry") || clean.startsWith("yd") || clean.startsWith("ryd") -> "yunda"
            // 申通：9开头，12-14位纯数字
            trackingNo.matches(Regex("^9[0-9]{11,13}$")) -> "shentong"
            // 中通：7开头，14-16位纯数字
            trackingNo.matches(Regex("^7[0-9]{13,15}$")) -> "zhongtong"
            // 德邦：DB开头
            clean.startsWith("db") -> "debang"
            // 默认：纯数字12-18位，优先尝试中通
            else -> "zhongtong"
        }
    }

    fun detectExpressQuery(message: String): Pair<String?, String>? {
        if (!expressKeywords.any { message.contains(it) }) return null

        val company = expressCompanyKeywords.keys.firstOrNull { message.contains(it) }

        // 优先匹配快递单号模式：连续的字母数字组合
        val trackingPattern = Regex("""[A-Za-z0-9]{8,30}""")
        val match = trackingPattern.find(message)
        val trackingNo = match?.value?.trim()
        if (trackingNo != null && trackingNo.length in 8..30) {
            Log.d("ExpressTool", "检测到快递查询: 公司=$company, 单号=$trackingNo")
            return Pair(company, trackingNo)
        }

        // 备选：从单词中提取可能的单号
        val words = message.split(Regex("[\\s,，。！？、]+"))
        for (word in words) {
            if (word.length in 8..30 && word.any { it.isLetterOrDigit() }) {
                Log.d("ExpressTool", "检测到快递查询: 公司=$company, 单号=$word")
                return Pair(company, word)
            }
        }

        Log.d("ExpressTool", "检测到快递意图但无单号")
        return null
    }

    private val expressCompanyKeywords = mapOf(
        "中通" to "zhongtong",
        "圆通" to "yuantong",
        "申通" to "shentong",
        "韵达" to "yunda",
        "顺丰" to "shunfeng",
        "邮政" to "ems",
        "EMS" to "ems",
        "极兔" to "jnt",
        "京东" to "jd",
        "德邦" to "debang"
    )
}
