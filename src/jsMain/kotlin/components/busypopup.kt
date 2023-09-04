package components

import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.foundation.focusIn
import handlerScope
import koin
import kotlinx.browser.document
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import kotlin.time.Duration.Companion.milliseconds

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
    val busyStore by koin.inject<BusyStore>()
    modal {
        openState(busyStore)
        modalPanel {
            modalOverlay {
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
            modalTitle {
                busyStore.messageStore.data.renderText(this)
            }
            div {
                transition(
                    enter = "transition duration-100 ease-out",
                    enterStart = "opacity-0 scale-95",
                    enterEnd = "opacity-100 scale-100",
                    leave = "transition duration-100 ease-in",
                    leaveStart = "opacity-100 scale-100",
                    leaveEnd = "opacity-0 scale-95"
                )
                // FIXME nice spinner thingy goes here
                para {+"!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!"}
                div("inline-block h-8 w-8 animate-spin rounded-full border-4 border-solid border-current border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]") {
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