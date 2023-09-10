package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement


fun RenderContext.overlay(baseClass: String?="absolute top-48 left-1/2 z-50 bg-white min-h-48 w-96 p-5 flex flex-col justify-between over-flow-auto",content: HtmlTag<HTMLDivElement>.() -> Unit) {
    div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
        div(baseClass,content = content)
    }
}

fun RenderContext.overlayLarge(baseClass: String?="mx-auto z-50 bg-white h-screen w-5/6 p-5 flex flex-col justify-between overflow-y-auto",content: HtmlTag<HTMLDivElement>.() -> Unit) {
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
                    +title
                }
            }
            div("absolute top-48 left-1/2 -translate-x-1/2  z-50 bg-white h-3/6 w-96 p-5 flex flex-col justify-between overflow-y-auto") {
                // FIXME flickering transition
//                transition(
//                    enter = "transition duration-100 ease-out",
//                    enterStart = "opacity-0 scale-95",
//                    enterEnd = "opacity-100 scale-100",
//                    leave = "transition duration-100 ease-in",
//                    leaveStart = "opacity-100 scale-100",
//                    leaveEnd = "opacity-0 scale-95"
//                )

                div {

                }.domNode.innerHTML = html
                div("flex flex-row place-items-center mx-auto w-fit") {
                    primaryButton {
                        +close
                        clicks handledBy {
                            openStateStore.update(false)
                        }
                    }
                }
            }
        }
    }
}

