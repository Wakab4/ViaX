package moe.pure.browser.feature.internalpages.render

/** u9.d.g(mode=1), shared Native list styling with its CSS-only clock icon. */
object GeneratedListCss {
    fun render(style: GeneratedPageStyle): String {
        val text = style.foreground
        val hint = style.secondary
        val icon = style.icon
        val tap = style.tap
        return buildString {
            append("* {padding:0;margin:0;box-sizing:border-box;}")
            append("html{height:100%;-webkit-tap-highlight-color:").append(tap)
            append(";-webkit-focus-ring-color: rgba(0, 0, 0, 0);}")
            append("body{min-height:100%;max-width:100%; width: 600px;margin: auto;text-align: center;}")
            if (style.rtl) append("html{direction:rtl;}")
            append("body{background: transparent;}#content {text-align:left;}")
            append(".box {vertical-align:middle;position:relative;display: block;padding: 20px 14px;border-bottom: 0px solid ").append(tap).append("}")
            append(".box a {width: 100%;height: 100%;position: absolute;left: 0;top: 0;}")
            append(".title {padding-left: 28px;color: ").append(text)
            append(";font-size: 15px;white-space: nowrap; overflow: hidden;text-overflow: ellipsis;margin:auto;}")
            append(".url {display: none;}.hint {color: ").append(hint)
            append(";font-size: 15px; white-space: normal; word-wrap: break-word; overflow: auto;text-overflow: ellipsis;padding: 50px 5px;text-align: center;margin: auto; line-height: 1.8em;}")
            append(".tag.icon {color: ").append(icon)
            append("; position: absolute; margin-left: 3px; margin-top: 5px; width: 7px; height: 7px; border-radius: 1px 1px 1px 0; border-left: solid 1px currentColor; border-top: solid 1px currentColor; } .tag.icon:before {content: ''; position: absolute; left: 1px; top: 1px; width: 9px; height: 10px; border-radius: 1px; border-left: solid 1px currentColor; border-right: solid 1px currentColor; border-bottom: solid 1px currentColor; -webkit-transform: rotate(-45deg); transform: rotate(-45deg); } .tag.icon:after {content: ''; position: absolute; left: 3px; top: 3px; width: 2px; height: 2px; border-radius: 50%; background-color: currentColor; }")
            append(".icon_handle {width:50px;/*height:100%;position:absolute;top:0;left:0;*/ z-index: 999; }")
            append(".bookmark.icon{color: ").append(icon)
            append(";position:absolute;margin-left:5px;margin-top:3px;width:12px;height:15px;border-radius:1px 1px 0 0;border-top:solid 1px currentColor;border-left:solid 1px currentColor;border-right:solid 1px currentColor}.bookmark.icon:before{content:'';position:absolute;top:10px;left:1px;width:7px;height:7px;border-top:solid 1px currentColor;border-left:solid 1px currentColor;-webkit-transform:rotate(45deg);transform:rotate(45deg)}")
            append(".clock.icon {color: ").append(icon)
            append("; position: absolute; margin-left: 1px; margin-top: 5px; width: 15px; height: 15px; border: solid 1px currentColor; border-radius: 8px;} .clock.icon:before {content: ''; position: absolute; top: 7px; left: 6.5px; width: 4.5px; height: 1px; background-color: currentColor; -webkit-transform-origin: 0% 0%; transform-origin: 0% 0%; transform:rotate(45deg);} .clock.icon:after {content: ''; position: absolute; top: 2px; left: 5.8px; width: 1px; height: 6px; background-color: currentColor; -webkit-transform-origin: 0% 0%; transform-origin: 0% 0%;}")
            append(".btn {padding: 12px 20px; color: #ffffff; border-radius: 2px; border: 0; outline: none;font-size: 15px;; margin: 10px;}.sort-ghost {opacity: 0.3;}")
            if (style.rtl) append("#content {text-align: right;}.title{padding-left:0px;padding-right: 28px;}")
        }
    }
}
