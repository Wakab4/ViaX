package moe.pure.browser.feature.about.document

import moe.pure.browser.feature.internalpages.render.GeneratedPageStyle

/** u9.d.g mode5; the About text layout does not contain the mode1 row/icon rules. */
internal object AboutDocumentCss {
    fun render(style: GeneratedPageStyle): String = buildString {
        append("* {padding:0;margin:0;box-sizing:border-box;}")
        append("html{height:100%;-webkit-tap-highlight-color:").append(style.tap)
        append(";-webkit-focus-ring-color: rgba(0, 0, 0, 0);}")
        append("body{min-height:100%;max-width:100%; width: 600px;margin: auto;text-align: center;}")
        if (style.rtl) append("html{direction:rtl;}")
        append("body{background: transparent;}")
        append("div{color: ").append(style.foreground)
        append(";max-width:800px; padding-left:5%; padding-right:5%; text-align:left;}")
        append("div>h1{font-size: 20px; padding-top:55px; padding-bottom:45px; text-align: left; }")
        append("div>p{ text-align: left; line-height: 1.8em; text-indent:2em;font-size: 15px;}")
        append("a{color: #6f8de1; font-size: 15px; line-height: 1.8em; text-decoration:none; }")
        if (style.rtl) append("div,h1,p,div>h1,div>p{text-align:right;}")
    }
}
