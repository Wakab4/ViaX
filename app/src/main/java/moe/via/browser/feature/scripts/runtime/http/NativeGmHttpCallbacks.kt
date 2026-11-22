package moe.via.browser.feature.scripts.runtime.http

import org.json.JSONObject

/** o5.b.c and i6.j0.i snapshot once and invoke ready-state/event handlers with the same JS object. */
internal class NativeGmHttpCallbacks(private val dispatch: (String) -> Unit, private val options: NativeGmHttpOptions) {
    fun loadStart(response: NativeGmHttpResponse) = emit(options.onLoadStart, response, true)
    fun ready(response: NativeGmHttpResponse) = emit(options.onReadyStateChange, response, false)
    fun progress(response: NativeGmHttpResponse) = emit(options.onProgress, response, false)
    fun loaded(response: NativeGmHttpResponse) = emit(options.onLoad, response, true)
    fun error(response: NativeGmHttpResponse) = emit(options.onError, response, true)
    fun timeout() = emit(options.onTimeout, null, false)
    fun uploaded(response: NativeGmHttpResponse) = emit(options.upload?.optString("onprogress"), response, false)

    private fun emit(event: String?, response: NativeGmHttpResponse?, includeReady: Boolean) {
        val names = if (includeReady) listOf(options.onReadyStateChange, event) else listOf(event)
        val handlers = names.filterNot { it.isNullOrEmpty() }
        if (handlers.isEmpty()) return
        if (response != null) response.progress = options.onProgress == event
        val value = if (response == null) "undefined" else "JSON.parse(${JSONObject.quote(response.json().toString())})"
        val calls = handlers.joinToString("") { "unsafeWindow.$it(obj);" }
        val script = """
            javascript: (function(){var obj=$value;
            if(obj&&obj.responseType){var type=obj.responseType;
              if(type==='undefined'){obj.responseType=undefined;obj.response=obj.responseText;}
              else if(type==='json'){try{obj.response=JSON.parse(obj.responseText)}catch(ignored){}}
              else if(type==='document'){try{obj.response=new DOMParser().parseFromString(obj.responseText,'text/html')}catch(ignored){}}
              else if(type==='blob'||type==='arraybuffer'||type==='stream'){
                if(obj.responseDataUrl){
                  const [metadata,data]=obj.responseDataUrl.split(','),mime=metadata.match(/data:([^;]+)/)[1],decoded=atob(data);
                  delete obj.responseDataUrl;obj.responseText=decoded;
                  const bytes=new Uint8Array(decoded.length);for(let i=0;i<decoded.length;i++)bytes[i]=decoded.charCodeAt(i);
                  if(type==='blob')obj.response=new Blob([bytes],{type:mime});
                  else if(type==='stream')obj.response=new ReadableStream({start(controller){controller.enqueue(bytes)},pull(controller){controller.close()},cancel(){}});
                  else obj.response=bytes.buffer;
                }
              }else obj.response=obj.responseText;
            }$calls})();
        """.trimIndent()
        dispatch(script)
    }
}
