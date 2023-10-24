package components

import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.foundation.setInitialFocus
import handlerScope
import koin
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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

class BusyStore : RootStore<Boolean>(false, Job()) {
    val titleStore = storeOf("",Job())
    val messageStore = storeOf("",Job())

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