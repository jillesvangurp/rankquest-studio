@file:OptIn(ExperimentalSerializationApi::class)

package searchpluginconfig

import com.jillesvangurp.ktsearch.DEFAULT_PRETTY_JSON
import components.para
import components.primaryButton
import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.disabled
import examples.quotesearch.movieQuotesNgramsSearchPluginConfig
import examples.quotesearch.movieQuotesSearchPluginConfig
import koin
import kotlinx.coroutines.flow.map
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import org.w3c.dom.HTMLHeadingElement
import search.ActiveSearchPluginConfigurationStore


fun RenderContext.pluginConfiguration() {
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    div {
        h1(
            content = fun HtmlTag<HTMLHeadingElement>.() {
                +"Configure"
            })

        activeSearchPluginConfigurationStore.data.render { pc ->
            div("flex flex-row") {
                primaryButton {
                    +"Use Movie Quotes"
                    this.disabled(pc?.name == movieQuotesSearchPluginConfig.name)
                    clicks.map { movieQuotesSearchPluginConfig } handledBy activeSearchPluginConfigurationStore.update
                }
                primaryButton {
                    +"Use Movie Quotes With Ngrams"
                    this.disabled(pc?.name == movieQuotesNgramsSearchPluginConfig.name)
                    clicks.map { movieQuotesNgramsSearchPluginConfig } handledBy activeSearchPluginConfigurationStore.update
                }
            }
            if (pc != null) {
                para { +"Current configuration: ${pc.name}" }
                pre {
                    +DEFAULT_PRETTY_JSON.encodeToString(pc)
                }
            } else {
                para { +"No active search plugin comfiguration" }
            }

        }
    }
}