package moe.pure.browser.feature.network.document

import moe.pure.browser.feature.network.PageRequest

/** u9.d.c and u9.c.h preserve the original raw URL row and innerHTML-based filtering. */
internal object NetworkLogMarkup {
    fun row(entry: PageRequest, time: String): String = buildString {
        append("<div class=\"box")
        if (entry.blocked) append(" block")
        append("\"><a href=\"").append(entry.url).append("\" title=\"").append(entry.url)
        append("\"></a><p class=\"title\">").append(time).append("<font class=\"tag\">")
        append(if (entry.blocked) "block" else "load").append("</font>")
        if (entry.extension.isNotEmpty()) append("<font class=\"res tag\">").append(entry.extension).append("</font>")
        append("</p><p class=\"url\">").append(entry.url).append("</p></div>")
    }

    const val filter = """<div id="filter-box"><select id="filter" onchange="filterChange()"><option value="">All</option><option value="js">JS</option><option value="css">CSS</option><option value="ico|png|jpg|gif|jpeg|webp|svg|avif">Image</option><option value="block">Blocked</option><option value="~js|css|ico|png|jpg|gif|jpeg|webp|svg|avif|block">Other</option></select></div>"""

    const val boldDomainScript = """function getDomain(a){var b=a.split("/"),c="";return a.indexOf("://")>-0&&b.length>2&&""!=b[2]&&(c=b[2]),c}function boldDomain(){var b,c,d,a=document.getElementsByClassName("url");for(d=a.length-1;d>=0;d--)b=a[d].innerHTML,c=getDomain(b),""!=c&&(a[d].innerHTML=b.replace(c,"<b>"+c+"</b>"))}boldDomain();"""

    const val filterScript = """function filterChange(){var filter=document.getElementById("filter");var filterValue=filter.options[filter.selectedIndex].value;var inverted=filterValue.length>0&&filterValue.charAt(0)=="~";if(inverted){filterValue=filterValue.substring(1)}var rules=filterValue.split("|");var es=document.getElementsByClassName("box");var temp;var show;for(var i=0,size=es.length;i<size;i++){temp=es[i].getElementsByClassName("title")[0].innerHTML;show=rules.length<1;for(var j=0,jsize=rules.length;j<jsize;j++){if(temp.indexOf(rules[j])>=0){show=true;break}}es[i].style.display=inverted!=show?"block":"none"}filter.blur()}"""
}
