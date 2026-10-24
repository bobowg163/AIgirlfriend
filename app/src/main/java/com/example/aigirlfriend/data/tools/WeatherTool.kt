package com.example.aigirlfriend.data.tools

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object WeatherTool {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    internal val cityCoordinates = mapOf(
        "北京" to Pair(39.9042, 116.4074),
        "上海" to Pair(31.2304, 121.4737),
        "广州" to Pair(23.1291, 113.2644),
        "深圳" to Pair(22.5431, 114.0579),
        "杭州" to Pair(30.2741, 120.1551),
        "南京" to Pair(32.0603, 118.7969),
        "成都" to Pair(30.5728, 104.0668),
        "武汉" to Pair(30.5928, 114.3055),
        "西安" to Pair(34.3416, 108.9398),
        "重庆" to Pair(29.5630, 106.5516),
        "天津" to Pair(39.0842, 117.2010),
        "苏州" to Pair(31.2990, 120.5853),
        "长沙" to Pair(28.2282, 112.9388),
        "昆明" to Pair(25.0406, 102.7128),
        "大连" to Pair(38.9140, 121.6147),
        "青岛" to Pair(36.0671, 120.3826),
        "厦门" to Pair(24.4798, 118.0894),
        "福州" to Pair(26.0745, 119.2965),
        "郑州" to Pair(34.7466, 113.6254),
        "济南" to Pair(36.6512, 116.9972),
        "哈尔滨" to Pair(45.8038, 126.5340),
        "长春" to Pair(43.8171, 125.3235),
        "沈阳" to Pair(41.8057, 123.4315),
        "台北" to Pair(25.0330, 121.5654),
        "香港" to Pair(22.3193, 114.1694),
        "澳门" to Pair(22.1987, 113.5439),
        "三亚" to Pair(18.2528, 109.5117),
        "桂林" to Pair(25.2746, 110.2876),
        "海口" to Pair(20.0444, 110.1999),
        "南宁" to Pair(22.8170, 108.3665),
        "兰州" to Pair(36.0611, 103.8343),
        "乌鲁木齐" to Pair(43.8256, 87.6168),
        "拉萨" to Pair(29.6500, 91.1409),
        "银川" to Pair(38.4872, 106.2309),
        "西宁" to Pair(36.6171, 101.7782),
        "呼和浩特" to Pair(40.8414, 111.7519),
        "石家庄" to Pair(38.0428, 114.5149),
        "太原" to Pair(37.8706, 112.5489),
        "合肥" to Pair(31.8206, 117.2272),
        "南昌" to Pair(28.6820, 115.8581),
        "贵阳" to Pair(26.6470, 106.6302),
        "宁波" to Pair(29.8683, 121.5440),
        "无锡" to Pair(31.4912, 120.3118),
        "常州" to Pair(31.8122, 119.9741),
        "温州" to Pair(28.0006, 120.6994),
        "东莞" to Pair(23.0208, 113.7518),
        "佛山" to Pair(23.0229, 113.1220),
        "珠海" to Pair(22.2707, 113.5767),
        "惠州" to Pair(23.1104, 114.4152),
        "江门" to Pair(22.5786, 113.0818),
        "中山" to Pair(22.5171, 113.3828),
        "汕头" to Pair(23.3681, 116.7158),
        "湛江" to Pair(21.2707, 110.3594),
        "茂名" to Pair(21.6631, 110.9256),
        "肇庆" to Pair(23.0780, 112.4898),
        "梅州" to Pair(24.2899, 116.1174),
        "河源" to Pair(23.7456, 114.6976),
        "清远" to Pair(23.6824, 113.0563),
        "韶关" to Pair(24.8104, 113.5964),
        "揭阳" to Pair(23.5466, 116.3683),
        "汕尾" to Pair(22.7857, 115.3750),
        "潮州" to Pair(23.6563, 116.6228),
        "阳江" to Pair(21.8596, 111.9826),
        "云浮" to Pair(22.9274, 112.0443),
        "玉林" to Pair(22.6559, 110.1954),
        "百色" to Pair(23.9051, 106.6171),
        "柳州" to Pair(24.3254, 109.4127),
        "梧州" to Pair(23.4783, 111.2775),
        "北海" to Pair(21.4733, 109.1169),
        "钦州" to Pair(21.9142, 108.6419),
        "防城港" to Pair(21.6893, 108.6236),
        "贵港" to Pair(23.1081, 109.5988),
        "贺州" to Pair(24.4198, 111.5486),
        "河池" to Pair(24.6854, 108.0782),
        "来宾" to Pair(23.7321, 109.2291),
        "崇左" to Pair(22.4029, 107.3764),
        "铜仁" to Pair(27.7193, 109.1919),
        "黔东南" to Pair(26.5907, 107.9955),
        "黔南" to Pair(26.2585, 107.9129),
        "黔西南" to Pair(25.0221, 104.9062),
        "安顺" to Pair(26.2448, 105.9322),
        "毕节" to Pair(27.3112, 105.2807),
        "遵义" to Pair(27.7079, 106.9307),
        "六盘水" to Pair(26.5541, 104.8177),
        "通辽" to Pair(43.6538, 122.2435),
        "赤峰" to Pair(42.2567, 118.8630),
        "鄂尔多斯" to Pair(39.8256, 109.7858),
        "呼伦贝尔" to Pair(49.2275, 119.7576),
        "包头" to Pair(40.6522, 109.8403),
        "巴彦淖尔" to Pair(41.7411, 107.4119),
        "乌兰察布" to Pair(40.9653, 113.1226),
        "兴安" to Pair(46.0782, 122.0377),
        "锡林郭勒" to Pair(43.9532, 116.0872),
        "阿拉善" to Pair(38.8429, 105.6958),
        "阿克苏" to Pair(41.1698, 80.2629),
        "喀什" to Pair(39.4677, 75.9898),
        "和田" to Pair(37.1150, 79.9249),
        "伊犁" to Pair(43.9190, 81.3232),
        "塔城" to Pair(46.7477, 82.9848),
        "阿勒泰" to Pair(47.8480, 88.1376),
        "博尔塔拉" to Pair(44.9021, 82.0577),
        "昌吉" to Pair(44.0152, 87.3030),
        "哈密" to Pair(42.8333, 93.5167),
        "巴音郭楞" to Pair(41.0150, 86.1469),
        "克孜勒苏" to Pair(39.7230, 75.9852),
        "日喀则" to Pair(29.2667, 88.8833),
        "昌都" to Pair(31.1409, 97.1717),
        "林芝" to Pair(29.6455, 94.3611),
        "山南" to Pair(28.9007, 91.7989),
        "那曲" to Pair(31.4799, 92.0535),
        "阿里" to Pair(32.5000, 80.1000)
    )

    suspend fun getWeather(city: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val matchedCity = cityCoordinates.keys.firstOrNull { c ->
                    c == city || c.contains(city) || city.contains(c)
                } ?: cityCoordinates.keys.firstOrNull { c ->
                    c.startsWith(city) || city.startsWith(c)
                } ?: "北京"

                val coords = cityCoordinates[matchedCity] ?: Pair(39.9042, 116.4074)
                Log.d("WeatherTool", "查询天气: '$city' -> 匹配: '$matchedCity' -> ${coords.first}, ${coords.second}")

                val url = "https://api.open-meteo.com/v1/forecast?" +
                        "latitude=${coords.first}&longitude=${coords.second}" +
                        "&current=temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m" +
                        "&timezone=Asia%2FShanghai"

                Log.d("WeatherTool", "API URL: $url")

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: throw Exception("空响应")
                Log.d("WeatherTool", "API Response: $body")

                val json = JSONObject(body)
                val current = json.optJSONObject("current")

                val temp = current?.optDouble("temperature_2m", 0.0)?.toInt() ?: 0
                val humidity = current?.optInt("relative_humidity_2m", 0) ?: 0
                val windSpeed = current?.optDouble("wind_speed_10m", 0.0) ?: 0.0
                val weatherCode = current?.optInt("weather_code", 0) ?: 0
                val appTemp = current?.optDouble("apparent_temperature", temp.toDouble())?.toInt() ?: temp

                val weatherDesc = getWeatherDescription(weatherCode)
                val feelsLike = if (appTemp != temp) ", 体感 ${appTemp}°C" else ""

                val result = "📍 $matchedCity 当前天气：$weatherDesc，气温 ${temp}°C，湿度 ${humidity}%，风速 ${"%.0f".format(windSpeed)} km/h$feelsLike"
                Log.d("WeatherTool", "结果: $result")
                result
            } catch (e: Exception) {
                Log.e("WeatherTool", "天气查询失败: ${e.message}", e)
                "暂时无法获取天气信息，请稍后重试~"
            }
        }
    }

    private fun getWeatherDescription(code: Int): String {
        return when (code) {
            0 -> "晴朗"
            1, 2, 3 -> "多云"
            45, 48 -> "雾"
            51, 53, 55 -> "毛毛雨"
            61, 63, 65 -> "下雨"
            66, 67 -> "冻雨"
            71, 73, 75 -> "下雪"
            77 -> "雪粒"
            80, 81, 82 -> "阵雨"
            85, 86 -> "雪阵雨"
            95, 96, 99 -> "雷雨"
            else -> "未知"
        }
    }
}
