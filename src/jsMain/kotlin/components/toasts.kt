package components

import dev.fritz2.core.RenderContext
import dev.fritz2.headless.components.toast
import kotlin.time.Duration.Companion.seconds

fun infoBubble(text: String) {
    toast("messages", duration = 3.seconds.inWholeMilliseconds, tag = RenderContext::div ) {
        div("bg-blueBright-200 p-5 rounded-lg border-2 border-blueBright-400 text-center w-96") {
            +text
        }
    }
}

fun warningBubble(text: String) {
    toast("messages", duration = 6.seconds.inWholeMilliseconds, tag = RenderContext::div ) {
        div("bg-yellow-200 p-5 rounded-lg border-2 border-yellow-400 text-center w-96") {
            +text
        }
    }
}

fun errorBubble(text: String, e: Throwable?=null) {
    if(e!=null) {
        console.error(e)
    }
    toast("messages", duration = 10.seconds.inWholeMilliseconds, tag = RenderContext::div) {
        div("bg-red-200 p-5 rounded-lg border-2 border-red-400 text-center w-96") {
            +text
            e?.let {
                +": ${e.message}"
            }
        }
    }
}