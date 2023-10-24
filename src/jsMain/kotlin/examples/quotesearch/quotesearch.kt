package examples.quotesearch

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import com.jilesvangurp.rankquest.core.pluginconfiguration.Metric
import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.BuiltinPlugins
import com.jilesvangurp.rankquest.core.plugins.ElasticsearchPluginConfiguration
import com.jilesvangurp.rankquest.core.plugins.PluginFactory
import com.jilesvangurp.rankquest.core.plugins.PluginFactoryRegistry
import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import com.jillesvangurp.ktsearch.deleteIndex
import com.jillesvangurp.ktsearch.index
import com.jillesvangurp.ktsearch.repository.repository
import components.confirm
import components.runWithBusy
import dev.fritz2.core.RootStore
import dev.fritz2.remote.http
import koin
import kotlinx.coroutines.Job
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.decodeFromJsonElement
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import search.*
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import kotlin.math.min
import kotlin.time.Duration.Companion.milliseconds


val quoteSearchModule = module {
    singleOf(::MovieQuotesStore)

    single {
        val movieQuotesStoreFactory = MovieQuotesStorePluginFactory(get())

        PluginFactoryRegistry().also {
            it.register("movies", movieQuotesStoreFactory)
        }
    }

}

val movieQuotesSearchPluginConfig = SearchPluginConfiguration(
    id = "movie-quotes",
    name = "movie-quotes",
    pluginType = "movies",
    fieldConfig = listOf(
        SearchContextField.StringField("q"),
        SearchContextField.IntField("size", 5)
    ),
    pluginSettings = null,
    metrics = Metric.entries.map { MetricConfiguration(it.name, it, it.supportedParams) }
)

val movieQuotesNgramsSearchPluginConfig = SearchPluginConfiguration(
    id = "movie-quotes-ngrams",
    name = "movie-quotes-ngrams",
    pluginType = "movies",
    fieldConfig = listOf(
        SearchContextField.StringField("q"),
        SearchContextField.IntField("size", 5)
    ),
    pluginSettings = null,
    metrics = Metric.entries.map { MetricConfiguration(it.name, it, it.supportedParams) }
)

val demoSearchPlugins = listOf(movieQuotesSearchPluginConfig, movieQuotesNgramsSearchPluginConfig)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class MovieQuote(
    // original data has no id, we overwrite this with a simple for loop
    @EncodeDefault val id: String = "-",
    val quote: String,
    val movie: String,
    val type: String,
    val year: Int
)

fun List<MovieQuote>.searchPlugin(nice: Boolean = true): SearchPlugin {
    val documentIndex = DocumentIndex(
        if (nice) {
            mutableMapOf(
                "quote" to TextFieldIndex(),
                "movie" to TextFieldIndex()
            )
        } else {
            val analyzer = Analyzer(
                tokenFilter = listOf(NgramTokenFilter(3))
            )
            mutableMapOf(
                "quote" to TextFieldIndex(analyzer, analyzer),
                "movie" to TextFieldIndex(analyzer, analyzer)
            )

        }
    )
    val labels = mutableMapOf<String, String>()
    this.indices.forEach {
        val q = this[it]
        labels[q.id] = "${q.movie}, ${q.year}: ${q.quote}"
        documentIndex.index(
            Document(
                q.id, mapOf(
                    "quote" to listOf(q.quote),
                    "movie" to listOf(q.movie),
                )
            )
        )
    }

    return object : SearchPlugin {
        override suspend fun fetch(
            searchContext: Map<String, String>,
            numberOfItemsToFetch: Int
        ): Result<SearchResults> {
            val text = searchContext["q"] ?: ""
            val hits = documentIndex.search {
                query = if (text.isNotBlank()) {
                    BoolQuery(
                        should = listOf(
                            MatchQuery("quote", text, prefixMatch = true),
                            MatchQuery("movie", text, prefixMatch = true, boost = 0.25),
                        )
                    )
                } else {
                    MatchAll()
                }
            }.let {
                it.subList(0, min(numberOfItemsToFetch, it.size))
            }
            return SearchResults(hits.size.toLong(), 0.milliseconds, hits.map { (id, score) ->
                SearchResults.SearchResult(id, labels[id] + " ($score)")
            }).let { Result.success(it) }
        }
    }
}

class MovieQuotesStore : RootStore<List<MovieQuote>>(listOf(), Job()) {
    // use inject here to dodge circular dependency
    val activeSearchPluginConfigurationStore by koin.inject<ActiveSearchPluginConfigurationStore>()

    fun searchClient(): SearchClient {
        val config = activeSearchPluginConfigurationStore.current
        return if (config != null && config.pluginType == BuiltinPlugins.ElasticSearch.name) {
            config.pluginSettings?.let {
                val esConfig = DEFAULT_JSON.decodeFromJsonElement<ElasticsearchPluginConfiguration>(it)
                val ktc = KtorRestClient(esConfig.host, esConfig.port, esConfig.https, esConfig.user, esConfig.password)
                SearchClient(ktc)
            }
        } else {
            null
        } ?: SearchClient()
    }

    val load = handle<String> { _, path ->
        http(path).get().body().let<String, List<MovieQuote>> { body ->
            DEFAULT_JSON.decodeFromString(body)
        }.let { quotes ->
            // set the id property
            quotes.indices.map { i -> quotes[i].copy(id = "$i") }
        }
    }
    val delRecipesES = handle {
        runWithBusy({
            val client = searchClient()
            client.deleteIndex("moviequotes")
        })
        it
    }

    val indexEs = handle {
        confirm(
            "Write to localhost:9200?",
            "This will attempt to write movies json to a locally running elasticsearch"
        ) {

            runWithBusy({
                val client = searchClient()
                val repository = client.repository(
                    "moviequotes",
                    MovieQuote.serializer()
                )
                repository.bulk(bulkSize = 500) {
                    current.forEach { m ->
                        index(m, id = m.id)
                    }
                }
            })
        }
        it
    }

    init {
        load("moviequotes.json")
        console.log("done loading")
    }
}

class MovieQuotesStorePluginFactory(val movieQuotesStore: MovieQuotesStore) : PluginFactory {
    override fun create(configuration: SearchPluginConfiguration): SearchPlugin {
        return when (configuration.name) {
            "movie-quotes" -> movieQuotesStore.current.searchPlugin()
            "movie-quotes-ngrams" -> movieQuotesStore.current.searchPlugin(false)

            else -> error("Unsupported name ${configuration.name}")
        }

    }
}


