import components.*
import dev.fritz2.core.RenderContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val cookiePermissionModule = module {
    singleOf(::CookiePermissionStore)
}

@Serializable
data class CookiePermission(val ok: Boolean = true, val dateAgreed: Instant = Clock.System.now())

class CookiePermissionStore() : LocalStoringStore<CookiePermission>(null, "permissions", CookiePermission.serializer())

fun RenderContext.cookiePopup() {
    val cookiePermissionStore = koin.get<CookiePermissionStore>()
    cookiePermissionStore.data.distinctUntilChanged().render { permissions ->
        if (permissions?.ok != true) {
            overlay(content = {
                h1 { +"Cookies and permissions" }
                para {
                    +"""
                        This website uses browser local storage to store json content 
                        that you create in the app. This information is never shared elsewhere.
                        In order to use this app, you have to agree to the usage of local storage. 
                        """.trimIndent()
                }
                primaryButton {
                    +"Agreed"

                    clicks.map {
                        CookiePermission()
                    } handledBy cookiePermissionStore.update
                    clicks handledBy {infoBubble("Welcome to Rankquest Query!")}
                }
            }, closeHandler = null)
        }
    }
}