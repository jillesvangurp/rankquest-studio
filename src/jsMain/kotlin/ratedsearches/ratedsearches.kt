package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import koin
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink

val ratedSearchesModule = module {
    singleOf(::RatedSearchesStore)
}

class RatedSearchesStore : RootStore<List<RatedSearch>>(listOf()) {

    val addOrReplace = handle<RatedSearch> { ratedSearches, rs ->
        if (ratedSearches.firstOrNull() { it.id == rs.id } == null) {
            ratedSearches + rs
        } else {
            ratedSearches.map { it }
        }

    }

    val deleteById = handle<String> { ratedSearches, id ->
        ratedSearches.filter { it.id != id }
    }
}

fun RenderContext.ratedSearches() {
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()

    ratedSearchesStore.data.render {
        if(it.isEmpty()) {
            p {
                +"Create some test cases from the search screen. "
                pageLink(Page.Search)
            }
        }
        it.forEach {rs ->
            p { +"${rs.id} ${rs.searchContext}" }
            p { +rs.ratings.toString()}
        }
    }
}