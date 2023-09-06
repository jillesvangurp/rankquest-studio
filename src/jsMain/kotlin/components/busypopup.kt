package components

import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import handlerScope
import koin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

val busyPopupModule = module {
    singleOf(::BusyStore)
}

suspend fun <R> busy(
    supplier: suspend () -> Result<R>,
    successMessage: String = "Done!",
    initialTitle: String = "Working",
    initialMessage: String = "Please wait ...",
    errorResult: suspend (Result<R>) -> Unit = {},
    processResult: suspend (R) -> Unit = { }
) {
    val busyStore = koin.get<BusyStore>()
    busyStore.withBusyState(supplier, successMessage, initialTitle, initialMessage, errorResult, processResult)
}

fun busyPopup() {
    // FIXME this doesn't work; figure out an alternative
    val busyStore = koin.get<BusyStore>()
    modal {
        openState(busyStore)
        modalPanel() {
            modalOverlay("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
                // some nice fade in/out effect for the overlay
                transition(
                    enter = "ease-out duration-300",
                    enterStart = "opacity-0",
                    enterEnd = "opacity-100",
                    leave = "ease-in duration-200",
                    leaveStart = "opacity-100",
                    leaveEnd = "opacity-0"
                )
            }
            modalTitle("absolute top-10 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-50 bg-white w-96 p-5") {
                div("absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2") {
                    busyStore.titleStore.data.renderText(this)
                }
            }
            div("absolute top-1/3 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white h-96 w-96 p-5") {
                transition(
                    enter = "transition duration-100 ease-out",
                    enterStart = "opacity-0 scale-95",
                    enterEnd = "opacity-100 scale-100",
                    leave = "transition duration-100 ease-in",
                    leaveStart = "opacity-100 scale-100",
                    leaveEnd = "opacity-0 scale-95"
                )
                // FIXME nice spinner thingy goes here
                div("absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 ") {
                    div("flex flex-col items-center") {
                        para {
                            busyStore.messageStore.data.renderText(this)
                        }
                        div("inline-block h-20 w-20 animate-spin rounded-full border-4 border-solid border-current border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]") {
                            span("!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]") {
                                +"..."
                            }
                        }
                    }
                }
            }
        }
    }
}

class BusyStore : RootStore<Boolean>(false) {
    val titleStore = storeOf("")
    val messageStore = storeOf("")

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> withBusyState(
        supplier: suspend () -> Result<T>,
        successMessage: String = "Done!",
        initialTitle: String = "Working",
        initialMessage: String = "Please wait ...",
        errorResult: suspend (Result<T>) -> Unit = {},
        processResult: suspend (T) -> Unit = {}
    ) {
        titleStore.update(initialTitle)
        messageStore.update(initialMessage)
        update(true)
        handlerScope.launch {
            val result = supplier.invoke()
            result.fold({
                processResult.invoke(it)
                titleStore.update(successMessage)
                delay(30.milliseconds)
                update(false) // not busy anymore & close
            }, {
                titleStore.update(it.message ?: "An Error Occurred")
                messageStore.update("Error: ${it::class.simpleName}")
                errorResult.invoke(result)
                console.warn("Failed with ${it::class.simpleName}: ${it.message}")
                delay(2.seconds)
                update(false) // not busy anymore & close
            })
        }
    }
}