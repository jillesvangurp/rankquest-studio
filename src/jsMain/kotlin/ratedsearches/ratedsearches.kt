package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

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
    p { +"TODO" }
}