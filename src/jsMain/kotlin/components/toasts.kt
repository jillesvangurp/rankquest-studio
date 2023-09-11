package components

import dev.fritz2.core.RenderContext
import dev.fritz2.headless.components.toast
import kotlin.time.Duration.Companion.seconds

fun RenderContext.infoBubble(text: String) {
    toast("messages", duration = 3.seconds.inWholeMilliseconds, ) {
        div("bg-blueBright-200 p-5 rounded-lg border-2 border-blueBright-400 text-center w-96") {
            +text
        }
    }
}

fun RenderContext.warningBubble(text: String) {
    toast("messages", duration = 3.seconds.inWholeMilliseconds, ) {
        div("bg-yellow-200 p-5 rounded-lg border-2 border-yellow-400 text-center w-96") {
            +text
        }
    }
}

fun RenderContext.errorBubble(text: String, e: Throwable?=null) {
    if(e!=null) {
        console.error(e)
    }
    toast("messages", duration = 3.seconds.inWholeMilliseconds, ) {
        div("bg-red-200 p-5 rounded-lg border-2 border-red-400 text-center w-96") {
            +text
            e?.let {
                +": ${e.message}"
            }
        }
    }
}