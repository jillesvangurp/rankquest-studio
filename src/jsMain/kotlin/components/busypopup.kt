package components

import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.foundation.setInitialFocus
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

suspend fun <T> runWithBusy(
    supplier: suspend () -> T,
    successMessage: String = "Done!",
    initialTitle: String = "Working",
    initialMessage: String = "Please wait ...",
    errorResult: suspend (Result<T>) -> Unit = {},
    processResult: suspend (T) -> Unit = { }
) {
    busyResult(suspend {
        try {
            Result.success(supplier.invoke())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }, successMessage, initialTitle, initialMessage, errorResult, processResult)
}

suspend fun <R> busyResult(
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

fun busyPopupMountPoint() {
    val busyStore = koin.get<BusyStore>()
    modal {
        openState(busyStore)
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
                                busyStore.titleStore.data.renderText(this)
                            }
                        }
                        div("mt-2") {
                            div("flex flex-col items-center") {
                                para {
                                    busyStore.messageStore.data.renderText(this)
                                }
                                div("inline-block h-20 w-20 animate-spin rounded-full border-4 border-solid border-current border-r-transparent align-[-0.125em] motion-reduce:animate-[spin_1.5s_linear_infinite]") {
                                    span("!absolute !-m-px !h-px !w-px !overflow-hidden !whitespace-nowrap !border-0 !p-0 ![clip:rect(0,0,0,0)]") {
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
            val result = try {
                supplier.invoke()
            } catch (e: Exception) {
                Result.failure(e)
            }
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