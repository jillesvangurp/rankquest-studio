package metrics

import com.jilesvangurp.rankquest.core.Metric
import com.jilesvangurp.rankquest.core.MetricResults
import com.jilesvangurp.rankquest.core.runAllMetrics
import com.jillesvangurp.ktsearch.DEFAULT_PRETTY_JSON
import components.SvgIconSource
import components.iconButton
import components.para
import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.headless.components.tooltip
import koin
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ratedsearches.RatedSearchesStore
import search.ActiveSearchPluginConfiguration

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

class MetricsOutputStore : RootStore<List<Pair<Metric, MetricResults>>>(listOf()) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val activeSearchPluginConfiguration = koin.get<ActiveSearchPluginConfiguration>()


    val measure = handle {
        ratedSearchesStore.current?.let { ratedSearches ->
            activeSearchPluginConfiguration.searchPlugin?.runAllMetrics(ratedSearches).also {
                console.log(it)
            }
        } ?: it
    }
}

fun RenderContext.metrics() {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val metricsOutputStore = koin.get<MetricsOutputStore>()

    ratedSearchesStore.data.render { ratedSearches ->
        if (ratedSearches == null) {
            p {
                +"Rate some searches first"
            }

        }
        primaryButton {
            +"Go!"
            clicks handledBy metricsOutputStore.measure
        }
        metricsOutputStore.data.render { metrics ->

            metrics.forEach { (metric, metricResult) ->
                metricResult(metric, metricResult)
            }
        }
    }
}

private fun RenderContext.metricResult(
    metric: Metric,
    metricResult: MetricResults
) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val ratedsearches = ratedSearchesStore.current
    val rss = ratedsearches?.associateBy { it.id }.orEmpty()

    div("flex flex-col mx-10 hover:bg-blueBright-50 w-full") {
        val expandedState = storeOf(false)
        expandedState.data.render { expanded ->
            div("flex flex-row w-full") {
                iconButton(
                    svg = if (expanded) SvgIconSource.Minus else SvgIconSource.Plus,
                    title = if (expanded) "Collapse details" else "Expand details"
                ) {
                    clicks.map { !expanded } handledBy expandedState.update
                }

                div("mx-3 w-full") { +"${metric.name}: ${+metricResult.metric}" }
            }
            if (expanded) {
                metricResult.details.forEach { metricResult ->
                    div("w-full") {
                        +metricResult.id
                        +": "
                        +rss[metricResult.id]!!.searchContext.toString()
                    }
                    div("flex flex-row w-full") {
                        div("w-full flex flax-col") {
                            div("w-full") {
                                div("flex flex-row w-full") {
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
                                    div("flex flex-row w-full") {
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
