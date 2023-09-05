package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import components.LocalStoringStore
import components.SvgIconSource
import components.confirm
import components.iconButton
import dev.fritz2.core.RenderContext
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
        confirm(
            question = "Are you sure you want to delete $id?",
            description = "Deleting a rated search cannot be undone.",
            yes = "Delete it",
            no = "Cancel"
        ) {
            val updated = (current.orEmpty()).filter { it.id != id }
            update(updated)
        }
        current
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
    val ratedSearchesStore by koin.inject<RatedSearchesStore>()

    val showStore = storeOf(false)
    div("flex flex-col mx-10 hover:bg-blueBright-50") {
        div("flex flex-row") {
            showStore.data.render { show ->
                if (show) {
                    iconButton(SvgIconSource.Expand, "Expand rated search") {
                        clicks.map { !show } handledBy showStore.update
                    }
                } else {
                    iconButton(SvgIconSource.Collapse, title = "Collapse rated search") {
                        clicks.map { !show } handledBy showStore.update
                    }
                }
            }
            div("mx-3 grow") {
                +"${
                    ratedSearch.searchContext.map { (k, v) -> "$k: $v" }.joinToString(", ")
                } rated results: ${ratedSearch.ratings.size} "
            }
            iconButton(SvgIconSource.Delete, "Delete rated search") {
                clicks.map { ratedSearch.id } handledBy ratedSearchesStore.deleteById
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