package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.foundation.setInitialFocus
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement


fun RenderContext.overlay(baseClass: String?="absolute top-48 left-1/2 z-50 bg-white min-h-48 w-96 p-5 flex flex-col justify-between over-flow-auto",content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
        div(baseClass,content = content)
    }
}

fun RenderContext.overlayLarge(baseClass: String?="mx-auto z-50 bg-white h-screen w-5/6 p-5 flex flex-col overflow-y-auto",content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
        div(baseClass,content = content)
    }
}


suspend fun confirm(question:String="Are you sure?!", description:String="If you click yes, the action will be completed", yes: String="Yes!",no:String="No get me out of here", conditionalBlock: suspend ()->Unit) {
    val openStateStore = storeOf(true)
    modal {
        openState(openStateStore)
        modalPanel {
            modalOverlay("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
                transition(
                    enter = "ease-out duration-300",
                    enterStart = "opacity-0",
                    enterEnd = "opacity-100",
                    leave = "ease-in duration-200",
                    leaveStart = "opacity-100",
                    leaveEnd = "opacity-0"
                )
            }
            modalTitle("absolute top-10 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-50 text-white bg-blueBright-700 w-96 p-2 items-center") {
                paraCentered {
                    +question
                }
            }
            div("absolute top-48 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white h-48 w-96 p-5 flex flex-col justify-between") {
//                transition(
//                    enter = "transition duration-100 ease-out",
//                    enterStart = "opacity-0 scale-95",
//                    enterEnd = "opacity-100 scale-100",
//                    leave = "transition duration-100 ease-in",
//                    leaveStart = "opacity-100 scale-100",
//                    leaveEnd = "opacity-0 scale-95"
//                )

                paraCentered {
                    +description
                }
                div("flex flex-row place-items-center mx-auto w-fit") {
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

fun RenderContext.infoBubble(title:String="Are you sure?!", html:String, close:String="Close") {
    val openStateStore = storeOf(false)
    secondaryButton {
        +"?"
        clicks.map { true } handledBy openStateStore.update
    }
    modal {
        openState(openStateStore)
        modalPanel("w-full fixed z-10 inset-0 overflow-y-auto") {
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
                                +title
                            }
                        }
                        div("mt-2") {
                            div {

                            }.domNode.innerHTML = html
                            div("flex flex-row place-items-center mx-auto w-fit overflow-y-auto") {
                                primaryButton {
                                    +close
                                    clicks handledBy {
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

