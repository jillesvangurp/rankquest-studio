package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import components.LocalStoringStore
import components.SvgIconSource
import components.icon
import dev.fritz2.core.RenderContext
import dev.fritz2.core.RootStore
import dev.fritz2.core.storeOf
import koin
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink

val ratedSearchesModule = module {
    singleOf(::RatedSearchesStore)
}

class RatedSearchesStore : LocalStoringStore<List<RatedSearch>>(
    listOf(),
    key = "rated-searches",
    serializer = ListSerializer(RatedSearch.serializer())
) {

    val addOrReplace = handle<RatedSearch> { current, rs ->
        val ratedSearches = current.orEmpty()
        if (ratedSearches.firstOrNull() { it.id == rs.id } == null) {
            ratedSearches + rs
        } else {
            ratedSearches.map { it }
        }

    }

    val deleteById = handle<String> { current, id ->
        (current.orEmpty()).filter { it.id != id }
    }
}

fun RenderContext.ratedSearches() {
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()

    ratedSearchesStore.data.render {
        if (it.isNullOrEmpty()) {
            p {
                +"Create some test cases from the search screen. "
                pageLink(Page.Search)
            }
        } else {
            it.forEach { rs ->
                ratedSearch(rs)
            }
        }
    }
}


fun RenderContext.ratedSearch(ratedSearch: RatedSearch) {
    val showStore = storeOf(false)
    div("flex flex-col mx-10") {
        div("flex flex-row") {
            div {
                +"${
                    ratedSearch.searchContext.map { (k, v) -> "$k: $v" }.joinToString(", ")
                } rated results: ${ratedSearch.ratings.size} "
            }
            showStore.data.render { show ->
                if (show) {
                    icon(SvgIconSource.Expand)
                } else {
                    icon(SvgIconSource.Collapse)
                }
                clicks.map { !show } handledBy showStore.update
            }

        }
        showStore.data.render { show ->
            if (show) {
                div("") {
                    p { +"RsId: ${ratedSearch.id}" }
                    div("flex flex-row w-full gap-3") {
                        div("w-1/12 bg-blueMuted-200") {
                            +"Document Id"
                        }
                        div("w-1/12 bg-blueMuted-200") {
                            +"Rating"
                        }

                        div("w-7/12 bg-blueMuted-200") { +"Label" }
                        div("w-3/12 bg-blueMuted-200") { +"Comment" }
                    }
                    ratedSearch.ratings.sortedByDescending { it.rating }.forEach {
                        div("flex flex-row w-full gap-3 border-t border-blueMuted-200") {
                            div("w-1/12 bg-blueBright-50") {
                                +it.documentId
                            }
                            div("w-1/12 bg-blueBright-50 hover:bg-blueBright-200") {
                                +it.rating.toString()
                            }

                            div("w-7/12") { +(it.label ?: "-") }
                            div("w-3/12 bg-blueBright-50 hover:bg-blueBright-200") { +(it.comment ?: "-") }
                        }
                    }
                }
            }
        }
    }

}