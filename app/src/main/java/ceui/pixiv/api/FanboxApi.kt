package ceui.pixiv.api

import ceui.lisa.utils.Common
import ceui.pixiv.ui.fanbox.FanboxWebBridge
import com.google.gson.Gson

/**
 * post.info —— 唯一带正文的接口，而且只能从 WebView 里发。
 */
suspend fun fetchFanboxPostInfo(bridge: FanboxWebBridge, postId: String): FanboxPost? {
    val raw = bridge.get("https://api.fanbox.cc/post.info?postId=$postId") ?: return null
    return runCatching {
        fanboxGson.fromJson(raw, FanboxPostDetailResponse::class.java).body?.post
    }.getOrElse {
        Common.showLog("fetchFanboxPostInfo 解析失败 postId=$postId: $it")
        null
    }
}

private val fanboxGson by lazy { Gson() }
