package components

import com.jillesvangurp.serializationext.DEFAULT_JSON
import com.jillesvangurp.serializationext.DEFAULT_PRETTY_JSON
import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import dev.fritz2.core.Store
import dev.fritz2.core.disabled
import dev.fritz2.core.download
import dev.fritz2.core.href
import dev.fritz2.core.storeOf
import dev.fritz2.core.title
import dev.fritz2.routing.encodeURIComponent
import kotlin.random.Random
import kotlin.random.nextULong
import kotlinx.browser.document
import kotlinx.serialization.KSerializer
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.MouseEvent

fun RenderContext.primaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    iconSource: SvgIconSource? = null,
    text: String? = null,
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    "btn btn-primary",
    id = id,
    scope = scope,
    content = {
        if (iconSource != null || text != null) {
            div("flex flex-row gap-2 flex-nowrap align-middle") {
                iconSource?.let {
                    iconImage(iconSource, baseClass = "h-5 w-5 fill-white place-items-center")
                }
                text?.let {
                    div {
                        +text
                    }
                }
            }
        }
        content.invoke(this)
    }
)

fun RenderContext.secondaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    iconSource: SvgIconSource? = null,
    text: String? = null,
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = """btn btn-secondary""".trimMargin(),
    id = id,
    scope = scope,
    content = {
        if (iconSource != null || text != null) {
            div("flex flex-row gap-2 place-items-center") {
                iconSource?.let {
                    iconImage(iconSource, baseClass = "h-5 w-5 fill-white place-items-center")
                }
                text?.let {
                    span {
                        +text
                    }
                }
            }
        }
        content.invoke(this)
    }
)
fun RenderContext.secondaryButtonSmall(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    iconSource: SvgIconSource? = null,
    text: String? = null,
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = """btn btn-secondary btn-sm""".trimMargin(),
    id = id,
    scope = scope,
    content = {
        if (iconSource != null || text != null) {
            div("flex flex-row gap-2 place-items-center") {
                iconSource?.let {
                    iconImage(iconSource, baseClass = "h-4 w-4 fill-white place-items-center")
                }
                text?.let {
                    span {
                        +text
                    }
                }
            }
        }
        content.invoke(this)
    }
)

fun RenderContext.navButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = """my-2 w-fit text-white bg-blueGrayMuted-600 hover:bg-blueGrayMuted-700 focus:ring-buttonNav-300 
        |focus:ring-4 font-medium rounded-lg text-sm px-6 py-2 focus:outline-none""".trimMargin(),
    id = id,
    scope = scope,
    content = content
)

fun RenderContext.activeNavButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = """my-2 w-fit text-white bg-blueGray-600 hover:bg-blueGray-700 focus:ring-buttonNavAct-300 focus:ring-4 
        |font-medium rounded-lg text-sm px-6 py-2 focus:outline-none""".trimMargin(),
    id = id,
    scope = scope,
    content = content
)

fun <T> RenderContext.jsonDownloadButton(
    contentStore: Store<T?>,
    fileName: String,
    serializer: KSerializer<T>,
    buttonText: String = "Download",
    converter: (T) -> String = {content ->
        DEFAULT_PRETTY_JSON.encodeToString(serializer, content)
    },
    after: (suspend () -> Unit)?=null
) {
    contentStore.data.render { content ->
        val downloadLinkId = "link-${Random.nextULong()}"
        if (content != null) {
            val downloadContent = converter.invoke(content)
            a("hidden", id = downloadLinkId) {
                +"invisible"

                href("data:application/json;charset=utf-8,$downloadContent")
                download(fileName)
            }
        }
        primaryButton {
            // invisible link that we simulate a click on
            div("flex flex-row gap-2 align-middle") {
                iconImage(SvgIconSource.Download, baseClass = "w-6 h-6 fill-white place-items-center")
                div("text-sm") {
                    +buttonText
                }
            }
            disabled(content == null)
            clicks handledBy {
                val e = document.createEvent("MouseEvents") as MouseEvent
                e.initEvent("click", bubbles = true, cancelable = true)
                // issue a click on the link to cause the download to happen
                document.getElementById(downloadLinkId)?.dispatchEvent(e)
                    ?: console.log("could not find link to click")
                infoBubble("Downloaded ")
                after?.let {
                    after.invoke()
                }
            }
        }
    }
}

fun <T> RenderContext.jsonDownloadButton(
    content: T,
    fileName: String,
    serializer: KSerializer<T>,
    buttonText: String = "Download",
) {
    val downloadLinkId = "link-${Random.nextULong()}"
    if (content != null) {
        val downloadContent = encodeURIComponent(
            DEFAULT_PRETTY_JSON.encodeToString(serializer, content)
        )
        a("hidden", id = downloadLinkId) {
            +"invisible"

            href("data:application/json;charset=utf-8,$downloadContent")
            download(fileName)
        }
    }
    primaryButton {
        // invisible link that we simulate a click on
        div("flex flex-row gap-2 place-items-center") {
            iconImage(SvgIconSource.Download, baseClass = "h-5 w-5 fill-white place-items-center")
            span {
                +buttonText
            }
        }
        disabled(content == null)
        clicks handledBy {
            val e = document.createEvent("MouseEvents") as MouseEvent
            e.initEvent("click", bubbles = true, cancelable = true)
            // issue a click on the link to cause the download to happen
            document.getElementById(downloadLinkId)?.dispatchEvent(e)
                ?: console.log("could not find link to click")
            infoBubble("Downloaded ")
        }
    }
}

fun <T> RenderContext.jsonFileImport(
    serializer: KSerializer<T>,
    buttonText: String = "Import",
    after: (suspend () -> Unit)?=null,
    onImport: (T) -> Unit,
) {
    flexRow {
        val textStore = storeOf("")
        textStore.data.render { text ->
            textFileInput(
                fileType = ".json",
                textStore = textStore,
            )
            primaryButton(text = buttonText, iconSource = SvgIconSource.Upload) {
                disabled(text.isBlank())
                clicks handledBy {
                    try {
                        val decoded = DEFAULT_JSON.decodeFromString(serializer, text)
                        onImport.invoke(decoded)
                    } catch (e: Exception) {
                        errorBubble("Parse error for file: ${e.message}")
                    }
                    after?.invoke()
                }
            }
        }
    }
}

fun RenderContext.iconButton(
    svg: SvgIconSource,
    title: String = "",
    baseClass: String? = "w-5 h-5 fill-blueBright-500 hover:fill-blueBright-900",
    block: (HtmlTag<HTMLButtonElement>.() -> Unit)? = null
) {
    button(baseClass) {
        svg {
            attr("viewBox",svg.viewBox)
            content(svg.content)
        }
        title(title)

        block?.invoke(this)
    }
}