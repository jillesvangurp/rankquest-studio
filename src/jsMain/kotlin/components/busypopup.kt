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

val busyPopupModule = module {
    singleOf(::BusyStore)
}

suspend fun <R> busy(
    supplier: suspend () -> Result<R>,
    successMessage: String = "Done!",
    waitMessage: String = "Please wait ...",
    confirmationMessage: String? = null, // enables a confirmation
    errorResult: suspend (Result<R>) -> Unit = {},
    processResult: suspend (R) -> Unit = { }
) {
    val busyStore by koin.inject<BusyStore>()
    busyStore.withBusyState(supplier, successMessage, waitMessage, errorResult, processResult)
}

fun busyPopup() {
    // FIXME this doesn't work; figure out an alternative
    val busyStore by koin.inject<BusyStore>()
    modal {
        openState(busyStore)
        modalPanel {
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
            modalTitle("absolute top-10 left-1/2 transform -translate-x-1/2 -translate-y-1/2 z-50") {
                busyStore.messageStore.data.renderText(this)
            }
            div("absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50") {
                transition(
                    enter = "transition duration-100 ease-out",
                    enterStart = "opacity-0 scale-95",
                    enterEnd = "opacity-100 scale-100",
                    leave = "transition duration-100 ease-in",
                    leaveStart = "opacity-100 scale-100",
                    leaveEnd = "opacity-0 scale-95"
                )
                // FIXME nice spinner thingy goes here
                div("inline-block h-20 w-20 animate-spin rounded-full border-4 border-solid border-current border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]") {
                    span("!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]") {
                        +"..."
                    }
                }
            }
        }
    }
}

class BusyStore : RootStore<Boolean>(false) {
    val messageStore = storeOf("")

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun <T> withBusyState(
        supplier: suspend () -> Result<T>,
        successMessage: String = "Done!",
        waitMessage: String = "Please wait ...",
        errorResult: suspend (Result<T>) -> Unit = {},
        processResult: suspend (T) -> Unit = {}
    ) {
        messageStore.update(waitMessage)
        update(true)
        console.log("SHOW")
        handlerScope.launch {
            val result = supplier.invoke()
            result.fold({
                processResult.invoke(it)
                messageStore.update(successMessage)
                delay(1000)
                update(false) // not busy anymore & close
            }, {
                messageStore.update(it.message ?: "Failed with ${it::class.simpleName}")
                errorResult.invoke(result)
                console.warn("Failed with ${it::class.simpleName}: ${it.message}")
                delay(1500)
                update(false) // not busy anymore & close
            })
            console.log("HIDE")
        }
    }
}