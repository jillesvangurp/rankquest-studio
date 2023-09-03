package examples.quotesearch

import com.jilesvangurp.rankquest.core.DEFAULT_JSON
import com.jilesvangurp.rankquest.core.SearchPlugin
import com.jilesvangurp.rankquest.core.SearchResults
import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import dev.fritz2.remote.http
import koin
import kotlinx.coroutines.flow.onEach
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import search.*
import kotlin.time.Duration.Companion.milliseconds


val quoteSearchModule = module {
    singleOf(::MovieQuotesStore)
}

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

fun List<MovieQuote>.searchPlugin(): SearchPlugin {
    val documentIndex = DocumentIndex(
        mutableMapOf(
            "quote" to TextFieldIndex(),
            "movie" to TextFieldIndex()
        )
    )
    val labels = mutableMapOf<String,String>()
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
                query = BoolQuery(
                    should = listOf(
                        MatchQuery("quote", text),
                        MatchQuery("movie", text)
                    )
                )
            }
            return SearchResults(hits.size.toLong(), 0.milliseconds, hits.map { (id, score) ->
                SearchResults.SearchResult(id, labels[id])
            }).let { Result.success(it) }
        }
    }
}

class MovieQuotesStore : RootStore<List<MovieQuote>>(listOf()) {
//    val searchPluginStore = storeOf(listOf<MovieQuote>().searchPlugin())
    val activeSearchPlugin by koin.inject<ActiveSearchPlugin>()

    val load = handle<String> { _, path ->
        http(path).get().body().let<String, List<MovieQuote>> { body ->
            DEFAULT_JSON.decodeFromString(body)
        }.let { quotes ->
            // set the id property
            quotes.indices.map { i-> quotes[i].copy(id="$i") }
        }.also {
            console.log("update plugin")
            activeSearchPlugin.update(it.searchPlugin())
        }
    }

    init {
        load("moviequotes.json")
    }
}


