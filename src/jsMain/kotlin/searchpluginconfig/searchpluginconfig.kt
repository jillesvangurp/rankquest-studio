@file:OptIn(ExperimentalSerializationApi::class)

package searchpluginconfig

import com.jillesvangurp.ktsearch.DEFAULT_PRETTY_JSON
import components.para
import components.primaryButton
import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import examples.quotesearch.movieQuotesSearchPluginConfig
import koin
import kotlinx.coroutines.flow.map
import kotlinx.serialization.*
import kotlinx.serialization.json.JsonObject
import org.koin.dsl.module
import org.w3c.dom.HTMLHeadingElement
import search.ActiveSearchPluginConfiguration

val searchPluginConfigModule = module {

}

@Serializable
sealed interface SearchContextField {
    val name: String

    @Serializable
    @SerialName("str")
    data class StringField(
        override val name: String,
        @EncodeDefault val placeHolder: String = "enter a query"
    ):SearchContextField

    @Serializable
    @SerialName("int")
    data class IntField(
        override val name: String,
        val defaultValue: Int = 0
    ):SearchContextField

    @Serializable
    @SerialName("bool")
    data class BoolField(
        override val name: String,
        val defaultValue: Boolean = false
    ):SearchContextField
}

@Serializable
data class SearchPluginConfiguration(val pluginName: String, val fieldConfig: List<SearchContextField>, val pluginSettings: JsonObject)

fun RenderContext.pluginConfiguration() {
    val activeSearchPluginConfiguration = koin.get<ActiveSearchPluginConfiguration>()
    div {
        h1(
        content = fun HtmlTag<HTMLHeadingElement>.() {
 +"Configure"
})

        activeSearchPluginConfiguration.data.render { pc ->
            if(pc != null) {
                para { +"Current configuration: ${pc.pluginName}" }
                pre {
                    +DEFAULT_PRETTY_JSON.encodeToString(pc)
                }
            }

            primaryButton {
                +"Use Movie Quotes"
                this.disabled(pc?.pluginName == movieQuotesSearchPluginConfig.pluginName)
                clicks.map { movieQuotesSearchPluginConfig } handledBy activeSearchPluginConfiguration.update
            }
        }
    }
}