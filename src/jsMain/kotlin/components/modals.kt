package components

import dev.fritz2.core.*
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.foundation.setInitialFocus
import koin
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement


enum class ZPriority {
    NORMAL,
    PLUS,
    TOP
}

fun RenderContext.zDiv(priority: ZPriority, content: HtmlTag<HTMLDivElement>.() -> Unit) {
    when(priority) {
        ZPriority.NORMAL -> div("z-5", content = content) // below fritz2 Modal (confirm dialog)
        ZPriority.PLUS -> div("z-20", content = content)
        ZPriority.TOP -> div("z-50", content = content)
    }
}

fun RenderContext.overlay(
    baseClass: String? = "mx-auto bg-white h-screen md:w-3/6 p-5 flex flex-col overflow-y-auto",
    priority: ZPriority = ZPriority.NORMAL,
    closeHandler: (()-> Unit)?=null,
    content: HtmlTag<HTMLDivElement>.() -> Unit,
) {
    zDiv(priority) {
        closeHandler?.let {
            clicks handledBy {
                closeHandler.invoke()
            }
        }
        div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90") {
            div(baseClass, content = content)

        }
    }
}

fun RenderContext.overlayLarge(
    baseClass: String? = "mx-auto bg-white h-screen md:w-5/6 p-5 flex flex-col overflow-y-auto",
    priority: ZPriority = ZPriority.NORMAL,
    content: HtmlTag<HTMLDivElement>.() -> Unit
) {
    zDiv(priority) {
        div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90") {
            div(baseClass, content = content)
        }
    }
}

suspend fun RenderContext.modalMountPoint(id: String = "modal-mount-point") {
    div(id = id) {

    }
}

suspend fun confirm(
    question: String = "Are you sure?!",
    description: String = "If you click yes, the action will be completed",
    yes: String = "Yes!",
    no: String = "No get me out of here",
    conditionalBlock: suspend () -> Unit
) {
    val openStateStore = storeOf(true)
    modal {
        openState(openStateStore)
        modalPanel("w-full fixed inset-0 overflow-y-auto") {
            div("flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0") {
                modalOverlay("fixed inset-0 bg-blueMuted-300 bg-opacity-75 transition-opacity") {
                    transition(
                        "ease-out duration-300",
                        "opacity-0",
                        "opacity-100",
                        "ease-in duration-200",
                        "opacity-100",
                        "opacity-0"
                    )
                }
                /* <!-- This element is to trick the browser into centering the modal contents. --> */
                span("hidden sm:inline-block sm:align-middle sm:h-screen") {
                    attr("aria-hidden", "true")
                    +" "
                }
                div(
                    """inline-block align-bottom sm:align-middle w-full sm:max-w-4xl px-6 py-5 sm:p-14 
                    | bg-white rounded-lg
                    | shadow-xl transform transition-all 
                    | text-left overflow-hidden""".trimMargin()
                ) {
                    transition(
                        "ease-out duration-300",
                        "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95",
                        "opacity-100 translate-y-0 sm:scale-100",
                        "ease-in duration-200",
                        "opacity-100 translate-y-0 sm:scale-100",
                        "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                    )
                    div("mt-3 text-center sm:mt-0 sm:text-left") {
                        modalTitle("text-white bg-blueBright-700 p-2 items-center") {
                            paraCentered {
                                +question
                            }
                        }
                        div("mt-2") {
                            paraCentered {
                                +description
                            }
                            rowCentered {
                                secondaryButton {
                                    +no
                                    clicks handledBy {
                                        openStateStore.update(false)
                                    }
                                }
                                primaryButton {
                                    +yes
                                    clicks handledBy {
                                        conditionalBlock.invoke()
                                        openStateStore.update(false)
                                    }
                                    setInitialFocus()
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.infoPopupFile(markdownFile: String, zPriority: ZPriority = ZPriority.TOP) {
    val infoPopoverOpenStore = storeOf(false)
    secondaryButton(iconSource = SvgIconSource.Question) {
        clicks.map { true } handledBy infoPopoverOpenStore.update
    }
    infoPopoverOpenStore.data.render {opened ->
        if(opened) {
            overlay(priority = zPriority, closeHandler={infoPopoverOpenStore.update(false)}) {
                markdownFile(markdownFile)
                primaryButton {
                    +"Close"
                    clicks.map { false } handledBy infoPopoverOpenStore.update
                }
            }
        }
    }
}


fun RenderContext.infoPopup(title: String = "Title TODO", markdown: String, zPriority: ZPriority = ZPriority.TOP) {
    val infoPopoverOpenStore = storeOf(false)
    secondaryButton(iconSource = SvgIconSource.Question) {
        clicks.map { true } handledBy infoPopoverOpenStore.update
    }
    infoPopoverOpenStore.data.render {opened ->
        if(opened) {
            overlay(priority = zPriority, content = {
                h1 { +title }
                markdownDiv(markdown)
                primaryButton {
                    +"Close"
                    clicks.map { false } handledBy infoPopoverOpenStore.update
                }
            }, closeHandler = null)
        }
    }
}

fun busyPopupMountPoint() {
    val busyStore = koin.get<BusyStore>()
    modal {
        openState(busyStore)
        modalPanel("w-full fixed z-50 inset-0 overflow-y-auto") {
            div("flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0") {
                modalOverlay("fixed inset-0 bg-blueMuted-300 bg-opacity-75 transition-opacity") {
                    transition(
                        "ease-out duration-300",
                        "opacity-0",
                        "opacity-100",
                        "ease-in duration-200",
                        "opacity-100",
                        "opacity-0"
                    )
                }
                /* <!-- This element is to trick the browser into centering the modal contents. --> */
                span("hidden sm:inline-block sm:align-middle sm:h-screen") {
                    attr("aria-hidden", "true")
                    +" "
                }
                div(
                    """inline-block align-bottom sm:align-middle w-full sm:max-w-4xl px-6 py-5 sm:p-14 
                    | bg-white rounded-lg
                    | shadow-xl transform transition-all 
                    | text-left overflow-hidden""".trimMargin()
                ) {
                    transition(
                        "ease-out duration-300",
                        "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95",
                        "opacity-100 translate-y-0 sm:scale-100",
                        "ease-in duration-200",
                        "opacity-100 translate-y-0 sm:scale-100",
                        "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                    )
                    div("mt-3 text-center sm:mt-0 sm:text-left") {
                        modalTitle("text-white bg-blueBright-700 p-2 items-center") {
                            paraCentered {
                                busyStore.titleStore.data.renderText(this)
                            }
                        }
                        div("mt-2") {
                            div("flex flex-col items-center") {
                                para {
                                    busyStore.messageStore.data.renderText(this)
                                }
                                // spinner
                                div(
                                    """inline-block h-20 w-20 animate-spin rounded-full 
                                    |border-4 border-solid border-current border-r-transparent 
                                    |align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]""".trimMargin()
                                ) {
                                    span("""!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]""") {
                                        +"..."
                                    }
                                }
                                setInitialFocus()
                            }
                        }
                    }
                }
            }
        }
    }
}


