package metrics

import Page
import com.jilesvangurp.rankquest.core.*
import com.jilesvangurp.rankquest.core.pluginconfiguration.Metric
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricsOutput
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink
import testcases.RatedSearchesStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import kotlinx.coroutines.Job
import testcases.TestCaseSearchFilterStore
import testcases.tagFilterEditor
import kotlin.math.pow
import kotlin.math.roundToLong

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

fun Double.round(decimals: Int): Double {
    if (decimals > 17) {
        throw IllegalArgumentException("this probably doesn't do what you want; makes sense only for <= 17 decimals")
    }
    val factor = 10.0.pow(decimals.toDouble())
    return (this * factor).roundToLong() / factor
}

class MetricsOutputStore() : RootStore<List<MetricsOutput>?>(null, Job()) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
    val testCaseSearchFilterStore = koin.get<TestCaseSearchFilterStore>()
    val measure = handle {
        runWithBusy({
            ratedSearchesStore.current?.let { ratedSearches ->
                activeSearchPluginConfigurationStore.current?.let { config ->
                    val pluginFactory = pluginFactoryRegistry.get(config.pluginType)
                    if (pluginFactory == null) {
                        console.error("plugin not found for ${config.pluginType}")
                    } else {
                        console.log("Using ${pluginFactory::class.simpleName}")
                    }
                    val tags = testCaseSearchFilterStore.current?.tags.orEmpty()
                    console.log(tags)
                    pluginFactory?.let { pf ->
                        val plugin = pf.create(config)
                        console.info("measuring")
                        plugin.runMetrics(config, ratedSearches.filter { rs ->
                            if (tags.isEmpty()) {
                                console.log("no tags")
                                true
                            } else {
                                console.log(rs.id,rs.tags, rs.tags.orEmpty().containsAll(tags))
                                rs.tags.orEmpty().containsAll(tags)
                            }
                        })
                    }
                }
            }
        }) { results ->
            update(results?.mapNotNull { r ->
                if (r.isFailure) {
                    console.error(r.exceptionOrNull())
                    null
                } else {
                    r.getOrThrow()
                }
            })
        }
        it
    }
}

fun RenderContext.metrics() {
    centeredMainPanel {

        val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
        val ratedSearchesStore = koin.get<RatedSearchesStore>()
        val metricsOutputStore = koin.get<MetricsOutputStore>()

        val expandedState = storeOf(mapOf<String, Boolean>())
        activeSearchPluginConfigurationStore.data.render { searchPluginConfiguration ->
            if (searchPluginConfiguration == null) {
                para {
                    +"Configure a search plugin first. "
                    pageLink(Page.Conf)
                }
            } else {
                ratedSearchesStore.data.render { ratedSearches ->
                    if (ratedSearches == null) {
                        p {
                            +"Rate some searches first. "
                            pageLink(Page.TestCases)
                        }

                    }
                    leftRightRow {
                        row {
                            primaryButton(text = "Run Metrics", iconSource = SvgIconSource.Equalizer) {
                                clicks handledBy metricsOutputStore.measure
                            }
                            jsonDownloadButton(
                                contentStore = metricsOutputStore,
                                fileName = "${searchPluginConfiguration.name} metrics ${Clock.System.now()}.json",
                                serializer = ListSerializer(MetricsOutput.serializer())
                            )

                            jsonFileImport(ListSerializer(MetricsOutput.serializer())) { decoded ->
                                metricsOutputStore.update(decoded)
                            }

                        }
                        infoPopup("Exploring Metrics", metricsInfo)
                    }
                    tagFilterEditor()
                    metricsOutputStore.data.render { metrics ->
                        div("w-full") {
                            metrics?.forEach { (_, metric, metricResult) ->
                                metricResult(expandedState, metric, metricResult)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun RenderContext.metricResult(
    expandedState: Store<Map<String, Boolean>>, metricConfiguration: MetricConfiguration, metricResult: MetricResults
) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    expandedState.data.render { em ->
        val expanded = em[metricConfiguration.name] == true

        ratedSearchesStore.data.render { ratedsearches ->

            val rss = ratedsearches?.associateBy { it.id }.orEmpty()

            div("flex flex-col mx-10 my-3 hover:bg-blueBright-50 p-2 rounded-lg border-2 border-blueBright-400") {
                div("flex flex-row w-full align-middle justify-between") {
                    h2 {
                        +metricConfiguration.name
                    }
                    infoPopup(metricConfiguration.metric.title, metricConfiguration.metric.explanation)
                }

                div {
                    +"Metric: "
                    renderMetricsScore(metricResult.metric, metricConfiguration.expected ?: 0.75)
                }
                div {
                    metricResult.scores.stats.let { resultStats ->
                        +"min: ${resultStats.min.round(3)}, max: ${resultStats.max.round(3)}, median: ${
                            resultStats.median.round(
                                3
                            )
                        }, \u03C3: ${resultStats.standardDeviation.round(3)}, variance: ${resultStats.variance.round(3)}"
                    }
                }
                para { +"SearchConfiguration: ${metricConfiguration.name}" }
                div("flex flex-row w-full") {
                    iconButton(
                        svg = if (expanded) SvgIconSource.Minus else SvgIconSource.Plus,
                        title = if (expanded) "Collapse details" else "Expand details"
                    ) {
                        if (expanded) {
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

                    div("mx-3 w-full") {
                        +"SearchContext: (${
                            metricConfiguration.params.map { "${it.name} = ${it.value}" }.joinToString(", ")
                        })"
                    }
                }
                if (expanded) {
                    metricResult.details.forEach { metricResult ->
                        border {


                            div("w-full") {
                                +metricResult.id
                                +": "
                                +rss[metricResult.id]!!.searchContext.toString()
                            }
                            div {
                                renderMetricsScore(metricResult.metric, metricConfiguration.expected ?: 0.75)
                            }
                            div("flex flex-row w-full hover:bg-blueBright-200") {
                                div("w-full flex flax-col") {
                                    div("w-full") {
                                        h2 { +"Rated results" }
                                        div("ml-5 flex flex-row w-full bg-blueBright-200") {
                                            div("w-1/6") {
                                                +"Rating"
                                            }
                                            div("w-1/6") {
                                                +"Doc ID"
                                            }
                                            div("w-3/6") {
                                                +"Label"
                                            }
                                            div("w-1/6") {
                                                +"Metric"
                                            }
                                        }
                                        metricResult.hits.forEach { (doc, score) ->
                                            div("ml-5 flex flex-row w-full") {
                                                div("w-1/6") {
                                                    +(rss[metricResult.id]?.ratings?.firstOrNull { it.documentId == doc.docId }?.rating?.toString()
                                                        ?: "1")
                                                }
                                                div("w-1/6 overflow-hidden") {
                                                    +doc.docId
                                                    showTooltip(doc.docId)
                                                }

                                                div("w-3/6 overflow-hidden") {
                                                    +(doc.label ?: "-")
                                                    showTooltip(doc.label ?: "-")
                                                }
                                                div("w-1/6 overflow-hidden") {
                                                    renderMetricsScore(score, metricConfiguration.expected ?: 0.75)
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
                                                            iconButton(SvgIconSource.Plus) {
                                                                clicks.mapNotNull {
                                                                    ratedSearch.copy(ratings = ratedSearch.ratings.filter { it.documentId != doc.docId })

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
                        }
                    }
                }
            }
        }
    }
}


val Metric.title
    get() = when (this) {
        Metric.PrecisionAtK -> "Precision at K"
        Metric.RecallAtK -> "Recall at K"
        Metric.MeanReciprocalRank -> "Mean Reciprocal Rank"
        Metric.ExpectedReciprocalRank -> "Expected Reciprocal Rank"
        Metric.DiscountedCumulativeGain -> "Discounted Cumulative Gain"
        Metric.NormalizedDiscountedCumulativeGain -> "Normalized Discounted Cumulative Gain"
    }
val Metric.explanation
    get() = renderMarkdown(
        when (this) {
            Metric.PrecisionAtK -> """
                **Precision At K**: This metric calculates the precision of search results by considering the 
                top 'k' results returned by the search engine. 'Precision' refers to the number of relevant 
                results divided by 'k'. The relevantRatingThreshold is the minimum rating a document 
                must have to be considered relevant.
            """.trimIndent()

            Metric.RecallAtK -> """
                **Recall At K**: Similar to PrecisionAtK, this metric looks at the top 'k' results but 
                focuses on 'recall', which measures how many of the relevant documents are retrieved. 
                The relevantRatingThreshold again specifies the minimum rating for relevance.
                
            """.trimIndent()

            Metric.MeanReciprocalRank -> """
                **Mean Reciprocal Rank**: This evaluates the position of the first relevant result in the list of 
                search results. Specifically, it is the reciprocal of the rank at which the first relevant document 
                is found. The relevantRatingThreshold sets the relevance rating threshold.
            """.trimIndent()

            Metric.ExpectedReciprocalRank -> """
                **Expected Reciprocal Rank**: ERR is an extension of MRR that takes into account the graded 
                relevance of results. It calculates the expected reciprocal rank of the first relevant or 
                highly relevant result. maxRelevance sets the maximum relevance grade.
            """.trimIndent()

            Metric.DiscountedCumulativeGain -> """
                **Discounted Cumulative Gain**: DCG measures the quality of the search results with higher 
                relevance documents appearing earlier in the search result list. The useLinearGains parameter 
                determines whether to use a linear gain (true) or a exponential gain (false) for the 
                discounting factor. Exponential gain exaggerates the effect of results being near the top.
            """.trimIndent()

            Metric.NormalizedDiscountedCumulativeGain -> """
                **Normalized Discounted Cumulative Gain**: This is a normalization of the DCG metric to account for 
                the ideal order of documents. It divides the DCG of the results by the DCG of the ideal order to
                 provide a score between 0 and 1. The useLinearGains indicates whether to apply linear gains.
            """.trimIndent()
        }
    )


fun RenderContext.renderMetricsScore(actual: Double, threshold: Double) {
    if (actual < threshold) {
        span("text-red-600") { +actual.round(3).toString() }
    } else {
        span("text-green-600") { +actual.round(3).toString() }
    }
}

val metricsInfo = """
The metrics screen is of course the whole point of this application. After you've configured your 
search plugin and created your test cases, you can run and explore metrics in this screen.

## Demo content

If you enable show demo content in the configuration screen and use one of the two demo
plugins, you can load some sample test cases in the test cases screen. And then you 
can get some metrics here. 

## Running metrics

Simply click the button and wait for the results to complete. A spinner 
will show while this is happening. If you have a lot of test cases, this may take a while.
 
## Reviewing your metrics

After it completes, it will show you the results for each metric. You can expand each metric
with the plus button to dig into the details. 

It will list for each test case the score for each rated result that appeared in the search
results. 

## Adding unrated results to your test cases

Sometimes, a test case and search plugin configuration will produce results that should be
included in the test case. 

## Import and Export

You can download results in json format and re-import it to explore the metrics later.

Note, future versions of this tool may add the ability to compare metrics as well.

## Available Metrics

${Metric.entries.map {
    "${it.explanation}\n\n"
}}    
""".trimIndent()