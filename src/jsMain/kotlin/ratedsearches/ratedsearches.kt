package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import dev.fritz2.core.RootStore
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val ratedSearchesModule = module {
    singleOf(::RatedSearchesStore)
}

class RatedSearchesStore : RootStore<List<RatedSearch>>(listOf()) {

}