package moe.pure.browser.feature.site

object SiteDecision {
    const val ALLOW = 1
    const val DENY = 2
    const val ASK = 3
    const val WIFI = 4
}

enum class SiteFeature(val title: String, val default: Int, val group: String, val singleSite: Boolean = true) {
    FONT_SIZE("字体大小", 100, "内容"),
    USER_AGENT("浏览器标识", 0, "内容"),
    DESKTOP("电脑模式", SiteDecision.DENY, "内容"),
    IMAGES("图像", SiteDecision.ALLOW, "内容"),
    JAVASCRIPT("JavaScript", SiteDecision.ALLOW, "内容"),
    COOKIES("Cookies", SiteDecision.ALLOW, "内容", false),
    THIRD_PARTY_COOKIES("拦截第三方 Cookies", SiteDecision.ALLOW, "内容", false),
    POPUPS("弹出式窗口", SiteDecision.ALLOW, "内容", false),
    ADBLOCK("广告拦截", SiteDecision.ALLOW, "基本"),
    INCOGNITO("隐身", SiteDecision.DENY, "基本"),
    MICROPHONE("麦克风", SiteDecision.ASK, "权限"),
    CAMERA("摄像头", SiteDecision.ASK, "权限"),
    CLIPBOARD("剪贴板", SiteDecision.ALLOW, "权限"),
    OPEN_APPS("打开应用", SiteDecision.ASK, "权限"),
    REDIRECTS("页面重定向", SiteDecision.ALLOW, "权限"),
    LOCATION("位置信息", SiteDecision.DENY, "权限"),
    VIBRATION("振动", SiteDecision.DENY, "权限", false),
    BACK_CACHE("返回不重载", SiteDecision.ALLOW, "高级");


}

data class SiteConfiguration(val domain: String, val enabled: Boolean, val values: Map<SiteFeature, Int>, val customUserAgent: String? = null,
    val native: moe.pure.browser.feature.site.data.NativeSiteConfig? = null)
