package com.example.aigirlfriend.data.tools

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * 东方财富「妙想」MCP Server 客户端。
 *
 * 协议：StreamableHttp + JSON-RPC 2.0（POST https://mxapi.eastmoney.com/mxds/mcp），
 * 认证：请求头 em_api_key。响应可能是纯 JSON，也可能是 SSE（data: {...} 行），
 * 本客户端两种格式都能解析。
 */
object MxMcpClient {

    const val MCP_URL = "https://mxapi.eastmoney.com/mxds/mcp"

    private const val TAG = "MxMcpClient"

    private val JSON_MEDIA = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    /** MCP tools/call 结果：text 为工具输出文本，isError 标记工具级错误 */
    data class McpToolResult(
        val text: String,
        val isError: Boolean = false,
        val errorMessage: String? = null
    ) {
        val isOk: Boolean get() = !isError && errorMessage == null
    }

    private var initialized = false
    private var sessionHeader: String? = null

    /** 调用一个 MCP 工具，返回结果或带错误标记的失败结果（不抛未捕获异常） */
    suspend fun callTool(apiKey: String, tool: String, query: String): McpToolResult =
        withContext(Dispatchers.IO) {
            try {
                val rpc = JSONObject()
                    .put("jsonrpc", "2.0")
                    .put("id", 1)
                    .put("method", "tools/call")
                    .put(
                        "params",
                        JSONObject().put("name", tool).put("arguments", JSONObject().put("query", query))
                    )

                val result = post(apiKey, rpc.toString())

                // 兜底：若认证/会话类错误（当前探测显示服务端不要求会话，仅保留 401/403 兜底）
                if (!initialized && result.errorIsSessionLike()) {
                    Log.d(TAG, "服务端要求会话，执行 initialize 后重试")
                    val initRpc = JSONObject()
                        .put("jsonrpc", "2.0")
                        .put("id", 0)
                        .put("method", "initialize")
                        .put(
                            "params",
                            JSONObject()
                                .put("protocolVersion", "2024-11-05")
                                .put("capabilities", JSONObject())
                                .put(
                                    "clientInfo",
                                    JSONObject().put("name", "aigirlfriend").put("version", "1.0")
                                )
                        )
                    val initResp = post(apiKey, initRpc.toString())
                    initialized = true
                    sessionHeader = initResp.sessionId
                    val retried = postWithSession(apiKey, rpc.toString())
                    if (retried.sessionId != null) sessionHeader = retried.sessionId
                    normalizedResult(retried)
                } else {
                    normalizedResult(result)
                }
            } catch (e: Exception) {
                Log.e(TAG, "MCP 调用失败: ${e.message}", e)
                McpToolResult(text = "", isError = true, errorMessage = e.message ?: "网络请求失败")
            }
        }

    private fun normalizedResult(raw: RawResponse): McpToolResult {
        if (raw.error != null) {
            return McpToolResult(text = "", isError = true, errorMessage = "HTTP ${raw.error}")
        }
        if (raw.body.isNullOrBlank()) {
            return McpToolResult(text = "", isError = true, errorMessage = "空响应")
        }
        if (raw.sessionId != null) sessionHeader = raw.sessionId
        return parseRpcResponse(raw.body, raw.sessionId)
    }

    /** 解析 JSON-RPC 响应：兼容 JSON 与 SSE（data: 行）两种格式 */
    private fun parseRpcResponse(rawBody: String?, sessionId: String?): McpToolResult {
        if (rawBody.isNullOrBlank()) {
            return McpToolResult(text = "", isError = true, errorMessage = "空响应")
        }

        val payload = if (rawBody.startsWith("event:") || rawBody.startsWith("data:")) {
            rawBody.lineSequence()
                .map { it.trim() }
                .filter { it.startsWith("data:") }
                .map { it.removePrefix("data:").trim() }
                .firstOrNull { it.isNotEmpty() }
                ?: return McpToolResult(text = "", isError = true, errorMessage = "SSE 响应无数据")
        } else {
            rawBody.trim()
        }

        val json = JSONObject(payload)

        // JSON-RPC 级错误
        json.optJSONObject("error")?.let { err ->
            return McpToolResult(text = "", isError = true, errorMessage = err.optString("message", "未知错误"))
        }

        val result = json.optJSONObject("result")
            ?: return McpToolResult(text = "", isError = true, errorMessage = "响应缺少 result")

        val toolIsError = result.optBoolean("isError", false)
        val contents = result.optJSONArray("content")
        val text = if (contents != null && contents.length() > 0) {
            contents.getJSONObject(0).optString("text", "")
        } else {
            result.optString("text", "")
        }

        return McpToolResult(text = text, isError = toolIsError, errorMessage = if (toolIsError) "工具返回错误" else null)
    }

    private fun post(apiKey: String, body: String): RawResponse {
        return postWithSession(apiKey, body)
    }

    private fun postWithSession(apiKey: String, body: String): RawResponse {
        val builder = Request.Builder()
            .url(MCP_URL)
            .header("em_api_key", apiKey)
            .header("Accept", "application/json, text/event-stream")
            .post(body.toRequestBody(JSON_MEDIA))
        sessionHeader?.let { builder.header("Mcp-Session-Id", it) }

        return client.newCall(builder.build()).execute().use { response ->
            val respBody = response.body?.string().orEmpty()
            val sessionId = response.header("Mcp-Session-Id")
            Log.d(TAG, "MCP 响应: ${response.code}, 长度=${respBody.length}")
            RawResponse(
                body = respBody,
                sessionId = sessionId,
                error = if (!response.isSuccessful) response.code.toString() else null
            )
        }
    }

    private class RawResponse(
        val body: String?,
        val sessionId: String?,
        val error: String?
    ) {
        fun errorIsSessionLike(): Boolean =
            error == "401" || error == "403"
    }
}
