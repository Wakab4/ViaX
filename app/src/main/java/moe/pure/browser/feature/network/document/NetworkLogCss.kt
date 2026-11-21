package moe.pure.browser.feature.network.document

import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** u9.d.g mode 6; these dimensions are CSS pixels, independent of Android density. */
internal object NetworkLogCss {
    fun render(style: GeneratedPageStyle): String = buildString {
        append("* {padding:0;margin:0;box-sizing:border-box;}")
        append("html{height:100%;-webkit-tap-highlight-color:").append(style.tap)
        append(";-webkit-focus-ring-color: rgba(0, 0, 0, 0);}")
        append("body{min-height:100%;max-width:100%; width: 600px;margin: auto;text-align: center;}")
        if (style.rtl) append("html{direction:rtl;}")
        append("body{background: transparent;}")
        append(".box {margin: 12px 0; text-align: left; vertical-align:middle;position:relative;display: block;padding-top:10px; padding-bottom:10px; padding-left:10px; padding-right:10px;}")
        append(".box a {width: 100%;height: 100%;position: absolute;left: 0;top: 0;}")
        append("span, .url, .box {word-break: break-all;}")
        append(".block{opacity:0.5;}.tag{background:#cd8282;padding:0 8px;margin:0 4px;color:white;font-size:12px;}.res{background:#5c91cb;}")
        append(".title {color: ").append(style.secondary).append(";font-size: 15px; padding: 4px 0px;}")
        append(".url {line-height: 1.2em; max-height: 4.8em;font-size: 15px;color: ").append(style.foreground)
        append("; white-space: normal; word-wrap: break-word; overflow: auto;text-overflow: ellipsis;}")
        append(".hint {line-height: 1.8em; color: ").append(style.secondary)
        append(";font-size: 15px; white-space: normal; word-wrap: break-word; overflow: auto;text-overflow: ellipsis; padding: 50px 5px; text-align: center; margin: auto;}")
        append("#filter-box{padding:10px}#filter{border: 1px solid ").append(style.tap)
        append(";border-radius:2px;-webkit-appearance:none;-moz-appearance:none;appearance:none;background:transparent;padding:5px;width:100%;font-size: 15px;color: ")
        append(style.foreground).append(";}")
        if (style.rtl) append(".box{text-align:right;}")
    }
}
