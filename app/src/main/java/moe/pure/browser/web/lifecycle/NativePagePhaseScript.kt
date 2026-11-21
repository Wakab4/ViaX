package moe.pure.browser.web.lifecycle

import org.json.JSONObject

/** i6.r installs one top-document observer from onPageStarted, using the root secret and WebView id. */
internal object NativePagePhaseScript {
    fun create(secret: String, viewId: Int): String = """
        (function(){
            "use strict";
            function send(phase){
                var key=${JSONObject.quote(secret)}, message=JSON.stringify({action:101,runAt:phase,from:$viewId});
                if(window&&window.webkit)webkit.messageHandlers.viaAndroidBridgeMessageHandler.postMessage({key:key,msg:message});
                else if(window&&window.via)window.via.postMessage(key,message);
            }
            try{
                function head(){if(document.head){send(1);return true}return false}
                if(!head()){
                    const observer=new MutationObserver(function(records,owner){if(head())owner.disconnect()});
                    observer.observe(document.documentElement||document,{childList:true,subtree:true});
                }
            }catch(ignored){}
            try{
                if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',function(){send(2)});
                else send(2);
            }catch(ignored){}
            try{
                if(document.readyState!=='complete')window.addEventListener('load',function(){send(4)});
                else send(4);
            }catch(ignored){}
        })();
    """.trimIndent()
}
