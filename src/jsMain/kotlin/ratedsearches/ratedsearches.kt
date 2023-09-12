package ratedsearches

import Page
import com.jilesvangurp.rankquest.core.RatedSearch
import com.jilesvangurp.rankquest.core.SearchResultRating
import components.*
import dev.fritz2.core.RenderContext
import dev.fritz2.core.Store
import dev.fritz2.core.disabled
import dev.fritz2.core.storeOf
import dev.fritz2.headless.components.toast
import koin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import pageLink
import search.SearchResultsStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import kotlin.time.Duration.Companion.seconds

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
    centeredMainPanel {

        val ratedSearchesStore = koin.get<RatedSearchesStore>()
        val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()


        val showStore = storeOf<Map<String, Boolean>>(mapOf())
        activeSearchPluginConfigurationStore.data.filterNotNull().render { searchPluginConfiguration ->


            ratedSearchesStore.data.render { ratedSearches ->

                row {
                    primaryButton(text = "Clear", iconSource = SvgIconSource.Cross) {
                        disabled(ratedSearches.isNullOrEmpty())
                        clicks handledBy {
                            confirm(
                                "Are you sure you want to do this?",
                                "This remove all your rated searches. Make sure to download your rated searches first!"
                            ) {
                                ratedSearchesStore.update(listOf())
                                toast("messages", duration = 3.seconds.inWholeMilliseconds) {
                                    +"Cleared!"
                                }
                            }
                        }
                    }
                    jsonDownloadButton(
                        ratedSearchesStore,
                        "${searchPluginConfiguration.name}-rated-searches-${Clock.System.now()}.json",
                        ListSerializer(RatedSearch.serializer())
                    )
                    jsonFileImport(ListSerializer(RatedSearch.serializer())) { decoded ->
                        ratedSearchesStore.update(decoded)
                    }

                }

                if (ratedSearches.isNullOrEmpty()) {
                    p {
                        +"Create some test cases from the search screen. "
                        pageLink(Page.Search)
                    }
                } else {
                    ratedSearches.forEach { rs ->
                        ratedSearch(showStore, rs)
                    }
                }
            }
        }
    }
}


fun RenderContext.ratedSearch(showStore: Store<Map<String, Boolean>>, ratedSearch: RatedSearch) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    div("flex flex-col mx-10 hover:bg-blueBright-50") {
        showStore.data.render { showMap ->
            val show = showMap[ratedSearch.id] == true
            div("flex flex-row items-center") {
                iconButton(
                    svg = if (show) SvgIconSource.Minus else SvgIconSource.Plus,
                    title = if (show) "Collapse rated search" else "Expand rated search"
                ) {
                    clicks.map {
                        val m = showMap.toMutableMap()
                        if (show) m.remove(ratedSearch.id) else m[ratedSearch.id] = true
                        m
                    } handledBy showStore.update
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
                    p { +"RsId: ${ratedSearch.id} Rated documents" }
                    div("flex flex-row w-full gap-2 items-center") {
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
                        div("flex flex-row w-full gap-2 border-t border-blueMuted-200") {
                            div("w-1/12 bg-blueBright-50") {
                                iconButton(SvgIconSource.Delete, title = "Remove this result") {
                                    clicks.map {
                                        ratedSearch.copy(ratings = ratedSearch.ratings.filter { it.documentId != searchResultRating.documentId  })
                                    } handledBy { modified ->
                                        confirm("Remove this result?",description = "Remove ${searchResultRating.documentId} | ${searchResultRating.label}") {
                                            ratedSearchesStore.addOrReplace(modified)
                                        }
                                    }
                                }
                                +" "
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
                div {
                    val addMoreStore = storeOf(false)
                    a {
                        +"Add more documents"
                        clicks.map { !addMoreStore.current } handledBy addMoreStore.update
                    }
                    addMoreStore.data.render { show ->
                        if (show) {
                            overlay("absolute top-48 left-1/2 -translate-x-1/2 z-50 bg-white h-96 w-200 p-5 flex flex-col justify-between overflow-auto") {
                                val searchResultsStore = koin.get<SearchResultsStore>()
                                searchResultsStore.data.render { rs ->
                                    when (rs) {
                                        null -> {
                                            para { +"Do a search in the search screen and then come back here" }
                                        }

                                        else -> {
                                            if (rs.isFailure) {
                                                para { +"Oopsie ${rs.exceptionOrNull()}" }
                                            } else {
                                                val results = rs.getOrThrow()
                                                p("mb-2") { +"Found ${results.total} results in ${results.responseTime}" }

                                                ul("list-disc") {
                                                    results.searchResultList.forEach { result ->
                                                        li("ml-5") {
                                                            +"${result.id}${result.label?.let { l -> ": $l" } ?: ""} "
                                                            if (ratedSearch.ratings.firstOrNull { it.documentId == result.id } == null) {
                                                                a {
                                                                    +"Add to rated search"
                                                                    clicks.map {
                                                                        ratedSearch.copy(
                                                                            ratings = ratedSearch.ratings + SearchResultRating(
                                                                                result.id,
                                                                                1,
                                                                                result.label
                                                                            )
                                                                        )
                                                                    } handledBy ratedSearchesStore.addOrReplace
                                                                }
                                                            } else {
                                                                p { +"Already rated" }
                                                                a {
                                                                    +"Remove from rated search"
                                                                    clicks.map {
                                                                        ratedSearch.copy(
                                                                            ratings = ratedSearch.ratings.filter { it.documentId != result.id }
                                                                        )
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
                                div("flex flex=row") {
                                    secondaryButton {
                                        +"Done"
                                        clicks.map { false } handledBy addMoreStore.update
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
//    div("absolute h-screen w-screen top-0 left-0 bg-gray-300 bg-opacity-90 z-40") {
    overlay {
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
//        }
    }
}