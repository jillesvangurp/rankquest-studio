package metrics

import com.jilesvangurp.rankquest.core.MetricResults
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.Store
import dev.fritz2.core.storeOf
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ratedsearches.RatedSearchesStore
import search.ActiveSearchPluginConfigurationStore

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

class MetricsOutputStore : RootStore<List<Pair<MetricConfiguration, MetricResults>>>(listOf()) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    val measure = handle {
        runWithBusy({
            ratedSearchesStore.current?.let { ratedSearches ->
                activeSearchPluginConfigurationStore.current?.let { config ->
                    pluginFactoryRegistry.get(config.pluginType)?.let { pf ->
                        val plugin = pf.create(config)
                        config.metrics.map { metricConfiguration ->
                            metricConfiguration to metricConfiguration.metric.run(
                                plugin,
                                ratedSearches,
                                metricConfiguration.params
                            )
                        }
                    }
                }
            } ?: listOf()
        }) {
            update(it)
        }
        it
    }
}

fun RenderContext.metrics() {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val metricsOutputStore = koin.get<MetricsOutputStore>()

    val expandedState = storeOf(mapOf<String, Boolean>())
    ratedSearchesStore.data.render { ratedSearches ->
        if (ratedSearches == null) {
            p {
                +"Rate some searches first"
            }

        }
        primaryButton {
            +"Run Metrics"
            clicks handledBy metricsOutputStore.measure
        }
        metricsOutputStore.data.render { metrics ->

            metrics.forEach { (metric, metricResult) ->
                metricResult(expandedState, metric, metricResult)
            }
        }
    }
}

private fun RenderContext.metricResult(
    expandedState: Store<Map<String, Boolean>>,
    metricConfiguration: MetricConfiguration,
    metricResult: MetricResults
) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    expandedState.data.render { em ->
        val expanded = em[metricConfiguration.name] == true

        ratedSearchesStore.data.render { ratedsearches ->

            val rss = ratedsearches?.associateBy { it.id }.orEmpty()

            div("flex flex-col mx-10 hover:bg-blueBright-50 w-full") {
                h2 {
                    +metricConfiguration.name
                }
                div("flex flex-row w-full") {
                    iconButton(
                        svg = if (expanded) SvgIconSource.Minus else SvgIconSource.Plus,
                        title = if (expanded) "Collapse details" else "Expand details"
                    ) {
                        if(expanded) {
                            clicks.map {
                                val m = em.toMutableMap()
                                m.remove(metricConfiguration.name)
                                m
                            } handledBy expandedState.update
                        } else {
                            clicks.map {
                                val m = em.toMutableMap()
                                m[metricConfiguration.name] = true
                                m
                            } handledBy expandedState.update
                        }
                    }

                    div("mx-3 w-full") { +"SearchContext: (${metricConfiguration.params.map { "${it.name} = ${it.value}" }.joinToString(", ")})" }
                }
                div { +"Metric: ${+metricResult.metric}" }
                if (expanded) {
                    metricResult.details.forEach { metricResult ->
                        div("w-full") {
                            +metricResult.id
                            +": "
                            +rss[metricResult.id]!!.searchContext.toString()
                        }
                        div("flex flex-row w-full hover:bg-blueBright-200") {
                            div("w-full flex flax-col") {
                                div("w-full") {
                                    h2 { +"Rated results" }
                                    div("ml-5 flex flex-row w-full bg-blueBright-200") {
                                        div("w-1/6") {
                                            +"Doc ID"
                                        }
                                        div("w-4/6") {
                                            +"Label"
                                        }
                                        div("w-1/6") {
                                            +"Metric"
                                        }
                                    }
                                    metricResult.hits.forEach { (doc, score) ->
                                        div("ml-5 flex flex-row w-full") {
                                            div("w-1/6") {
                                                +doc.docId
                                            }
                                            div("w-4/6") {
                                                +(doc.label ?: "-")
                                            }
                                            div("w-1/6") {
                                                +score.toString()
                                            }
                                        }
                                    }
                                    if (metricResult.unRated.isNotEmpty()) {
                                        h2 { +"Unrated results" }
                                        metricResult.unRated.forEach { doc ->
                                            div("ml-5 flex flex-row w-full") {
                                                div("w-1/6") {
                                                    +doc.docId
                                                }
                                                div("w-4/6") {
                                                    +(doc.label ?: "-")
                                                }

                                                rss[metricResult.id]?.let { ratedSearch ->
                                                    if (ratedSearch.ratings.firstOrNull { it.documentId == doc.docId } == null) {
                                                        iconButton(SvgIconSource.Plus) {
                                                            clicks.mapNotNull {
                                                                ratedSearch.copy(
                                                                    ratings = ratedSearch.ratings + SearchResultRating(
                                                                        documentId = doc.docId,
                                                                        rating = 1,
                                                                        label = doc.label
                                                                    )
                                                                )

                                                            } handledBy ratedSearchesStore.addOrReplace
                                                        }
                                                    } else {
                                                        iconButton(SvgIconSource.Minus) {
                                                            clicks.mapNotNull {
                                                                ratedSearch.copy(
                                                                    ratings = ratedSearch.ratings.filter { it.documentId != doc.docId }
                                                                )

                                                            } handledBy ratedSearchesStore.addOrReplace
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (metricResult.unRated.isNotEmpty()) {
                            div("flex flex-row") {
                                div("w-1/12") {
                                    +"Hits"
                                }
                            }
                            div {
                                div("w-11/12") {
                                    div {
                                        div("flex flex-row w-full") {
                                            div("w-1/6") {
                                                +"Doc Id"
                                            }
                                            div("w-5/6") {
                                                +"Label"
                                            }
                                        }
                                        metricResult.unRated.forEach { docId ->
                                            div("flex flex-row w-full") {
                                                div("w-1/6") {
                                                    +docId.docId
                                                }
                                                div("w-5/6") {
                                                    +(docId.label ?: "-")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

