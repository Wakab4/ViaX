package moe.pure.browser.web.download

import org.json.JSONObject

/** Native za.b/a.b and image/blob/download-stream scripts use the same via receiver. */
internal object NativePageDownloadScript {
    fun hooks(secret: String): String {
        val token = JSONObject.quote(secret)
        return """
            (function(){let n='via-dl1';if(!window[n]){window[n]=true;
              function bind(a){if(a)a.addEventListener('click',function(){const name=a.getAttribute('download');if(!name||name==='undefined'||name==='null')return;const url=a.href;if(url&&url.length<=1048576)try{window.via.postMessage($token,JSON.stringify({action:104,url:url,name:name}))}catch(e){}})}
              function scan(){document.querySelectorAll('a[download]').forEach(bind)}
              if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',scan);else scan();
              const create=document.createElement;document.createElement=function(tag,options){if(typeof tag!=='string')return create.call(document,tag,options);const a=options?create.call(document,tag,options):create.call(document,tag);if(tag.toLowerCase()==='a')bind(a);return a};
            }})();
            (function(){if(!window['via-blob-test']){window['via-blob-test']={};const create=URL.createObjectURL;URL.createObjectURL=function(value){const url=create(value);if(!(value instanceof MediaSource))window['via-blob-test'][url]=value;return url}}})();
        """.trimIndent()
    }
    fun image(secret: String, url: String): String {
        val token = JSONObject.quote(secret); val target = JSONObject.quote(url)
        return """
            (function(){var x=new XMLHttpRequest;x.open('GET',$target,true);x.responseType='blob';
              x.onload=function(){if(x.status===200){var r=new FileReader;r.onloadend=function(){window.via.download($token,$target,r.result)};r.readAsDataURL(x.response)}else window.via.download($token,$target,'')};
              x.onerror=function(){window.via.download($token,$target,'')};x.send()})();
        """.trimIndent()
    }
    fun blob(secret: String, url: String): String {
        val token = JSONObject.quote(secret); val target = JSONObject.quote(url)
        return """
            (function(){function read(url,value){var r=new FileReader;r.readAsDataURL(value);r.onloadend=function(){window.via.download($token,url,r.result)}}
              function fetchBlob(url){var x=new XMLHttpRequest;x.open('GET',url,true);x.responseType='blob';x.onload=function(){if(this.status===200)read(url,this.response)};x.send()}
              var key='via-blob-test',url=$target;if(window[key]&&window[key][url])read(url,window[key][url]);else fetchBlob(url)})();
        """.trimIndent()
    }
    fun stream(secret: String, url: String): String {
        val token = JSONObject.quote(secret); val target = JSONObject.quote(url)
        return """
            (function(){fetch($target).then(function(response){return response.blob()}).then(function(value){return new Promise(function(resolve,reject){var r=new FileReader;r.onloadend=function(){resolve(r.result)};r.onerror=reject;r.readAsDataURL(value)})}).then(function(data){window.via.download($token,$target,data)}).catch(function(){})})();
        """.trimIndent()
    }
}
