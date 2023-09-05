package metrics

import com.jilesvangurp.rankquest.core.Metric
import com.jilesvangurp.rankquest.core.MetricResults
import com.jilesvangurp.rankquest.core.runAllMetrics
import com.jillesvangurp.ktsearch.DEFAULT_PRETTY_JSON
import components.para
import components.primaryButton
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import koin
import kotlinx.serialization.encodeToString
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ratedsearches.RatedSearchesStore
import search.ActiveSearchPluginConfiguration

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

class MetricsOutputStore: RootStore<List<Pair<Metric,MetricResults>>>(listOf()) {
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()
    val activeSearchPluginConfiguration by koin.inject<ActiveSearchPluginConfiguration>()


    val measure = handle {
        ratedSearchesStore.current?.let {ratedSearches ->
            activeSearchPluginConfiguration.searchPlugin?.runAllMetrics(ratedSearches).also {
                console.log(it)
            }
        } ?: it
    }
}

fun RenderContext.metrics() {
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()
    val metricsOutputStore by koin.inject<MetricsOutputStore>()

    ratedSearchesStore.data.render { ratedSearches ->
        if(ratedSearches==null) {
            p {
                +"Rate some searches first"
            }

        }
        primaryButton {
            +"Go!"
            clicks handledBy metricsOutputStore.measure
        }
        metricsOutputStore.data.render {metrics ->
            metrics.forEach { (metric,metricResult) ->
                para { +metric.name }
                pre {
                    +DEFAULT_PRETTY_JSON.encodeToString(metricResult)
                }
            }
        }
    }
}