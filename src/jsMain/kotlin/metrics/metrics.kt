package metrics

import Page
import com.jilesvangurp.rankquest.core.MetricResults
import com.jilesvangurp.rankquest.core.SearchResultRating
import com.jilesvangurp.rankquest.core.pluginconfiguration.Metric
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricsOutput
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink
import testcases.RatedSearchesStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore

val metricsModule = module {
    singleOf(::MetricsOutputStore)
}

class MetricsOutputStore : RootStore<List<MetricsOutput>?>(null) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val pluginFactoryRegistry = koin.get<PluginFactoryRegistry>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    val measure = handle {
        runWithBusy({
            ratedSearchesStore.current?.let { ratedSearches ->
                activeSearchPluginConfigurationStore.current?.let { config ->
                    pluginFactoryRegistry.get(config.pluginType)?.let { pf ->
                        val plugin = pf.create(config)
                        coroutineScope {
                            config.metrics.map { metricConfiguration ->
                                async {
                                    MetricsOutput(
                                        config.name, metricConfiguration, metricConfiguration.metric.run(
                                            plugin, ratedSearches, metricConfiguration.params
                                        )
                                    )
                                }
                            }.awaitAll()
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
                        infoPopup(
                            "Exploring Metrics", """
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
                        """.trimIndent()
                        )
                    }
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

                div { +"Metric: ${+metricResult.metric}" }
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
                                +"${metricResult.metric}"
                            }
                            div("flex flex-row w-full hover:bg-blueBright-200") {
                                div("w-full flex flax-col") {
                                    div("w-full") {
                                        h2 { +"Rated results" }
                                        div("ml-5 flex flex-row w-full bg-blueBright-200") {
                                            div("w-1/6") {
                                                +"Doc ID"
                                            }
                                            div("w-1/6") {
                                                +"Rating"
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
                                                    +doc.docId
                                                }
                                                div("w-1/6") {
                                                    +(rss[metricResult.id]?.ratings?.firstOrNull { it.documentId == doc.docId }?.rating?.toString()
                                                        ?: "1")
                                                }

                                                div("w-3/6") {
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
                Precision at K is measures the proportion of relevant items or documents 
                among the top K items that are returned for a given search query. The more relevant 
                results, the higher the precision.
            """.trimIndent()

            Metric.RecallAtK -> """
                Recall@k is a measure of whether the rated results are part of the result list. For a 
                perfect score of 1, all rated results should be found. 
                
            """.trimIndent()

            Metric.MeanReciprocalRank -> """
                Reciprocal rank measures the quality of the first relevant result. The further down
                the list the result is, the lower the score.
            """.trimIndent()

            Metric.ExpectedReciprocalRank -> """
                Expected reciprocal rank calculates the probability that each result is the one the 
                user is looking for. If a documents with lower rating appear before one with a higher
                rating, the score is lower than if the better rated results appear first.
            """.trimIndent()

            Metric.DiscountedCumulativeGain -> """
                Discounted Cumulative Gain calculates a score that adds up the gain of each result
                relative to its position in the results. 
                
                Two gain functions are supported; a linear gain function that simply uses the rating 
                as is and an exponential one that uses an exponential of the rating to exaggerate 
                the effect of important results appearing near the top.
            """.trimIndent()

            Metric.NormalizedDiscountedCumulativeGain -> """
                NDCG is similar to DCG but it divides the score by an ideal DCG that is calculated 
                from the provided ratings. Consequently, the score is always below 0 and 1 where 
                higher scores indicate that the results return the best rated results first.
            """.trimIndent()
        }
    )



