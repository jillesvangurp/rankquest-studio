package ratedsearches

import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import components.*
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
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
            ratedSearches.map {
                if (it.id == rs.id) {
                    rs
                } else {
                    it
                }
            }
        }
    }

    val updateSearchResultRating = handle<Pair<String, SearchResultRating>> { current, (id, srr) ->
        val modifiedRating = current?.firstOrNull { it.id == id }?.let { rs ->
            rs.copy(ratings = rs.ratings.map {
                if (it.documentId == srr.documentId) {
                    srr
                } else {
                    it
                }
            })
        }
        if (modifiedRating != null) {
            current.map {
                if (it.id == modifiedRating.id) {
                    modifiedRating
                } else {
                    it
                }
            }
        } else {
            current
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
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

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
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    val showStore = storeOf(false)
    div("flex flex-col mx-10 hover:bg-blueBright-50") {
        showStore.data.render { show ->
            div("flex flex-row items-center") {
                    iconButton(
                        svg = if (show) SvgIconSource.Minus else SvgIconSource.Plus,
                        title = if (show) "Collapse rated search" else "Expand rated search"
                    ) {
                        clicks.map { !show } handledBy showStore.update
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
            if (show) {
                div("") {
                    p { +"RsId: ${ratedSearch.id}" }
                    div("flex flex-row w-full gap-3 items-center") {
                        div("w-1/12 bg-blueMuted-200") {
                            +"Doc Id"
                        }
                        div("w-1/12 bg-blueMuted-200") {
                            +"Rating"
                        }

                        div("w-7/12 bg-blueMuted-200") { +"Label" }
                        div("w-3/12 bg-blueMuted-200") { +"Comment" }
                    }
                    ratedSearch.ratings.sortedByDescending { it.rating }.forEach { searchResultRating ->
                        div("flex flex-row w-full gap-3 border-t border-blueMuted-200") {
                            div("w-1/12 bg-blueBright-50") {
                                +searchResultRating.documentId
                            }
                            div("w-1/12 bg-blueBright-50 hover:bg-blueBright-200") {
                                val editingStore = storeOf(false)
                                editingStore.data.render { editing ->
                                    if (editing) {
                                        modalFieldEditor(
                                            editingStore,
                                            ratedSearch.id,
                                            storeOf(searchResultRating.rating.toString())

                                        ) { s -> searchResultRating.copy(rating = s.toInt()) }
                                    } else {
                                        div {
                                            +searchResultRating.rating.toString()
                                            clicks.map { !editingStore.current } handledBy editingStore.update
                                        }
                                    }
                                }
                            }

                            div("w-7/12") { +(searchResultRating.label ?: "-") }
                            div("w-3/12 bg-blueBright-50 hover:bg-blueBright-200") {
                                val commentEditingStore = storeOf(false)
                                commentEditingStore.data.render { editing ->
                                    if (editing) {
                                        modalFieldEditor(
                                            commentEditingStore,
                                            ratedSearch.id,
                                            storeOf(searchResultRating.comment ?: ""),
                                        ) { s -> searchResultRating.copy(comment = s.takeIf { it.isNotBlank() }) }
                                    } else {
                                        div {
                                            +(searchResultRating.comment ?: "-")
                                            clicks.map { !commentEditingStore.current } handledBy commentEditingStore.update
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

private fun RenderContext.modalFieldEditor(
    editingStore: Store<Boolean>,
    ratedSearchId: String,
    fieldStore: Store<String>,
    transform: (String) -> SearchResultRating
) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
        div("absolute top-48 left-1/2 -translate-x-1/2 -translate-y-1/2 z-50 bg-white h-48 w-96 p-5 flex flex-col justify-between") {
            textField {
                value(fieldStore)
            }
            div("flex flex=row") {
                secondaryButton {
                    +"Cancel"
                    clicks.map { false } handledBy editingStore.update
                }
                primaryButton {
                    +"OK"
                    clicks.map {
                        ratedSearchId to transform(fieldStore.current)
                    } handledBy ratedSearchesStore.updateSearchResultRating
                    clicks.map { false } handledBy editingStore.update
                }
            }
        }
    }
}