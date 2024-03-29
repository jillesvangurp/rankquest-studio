package testcases

import Page
import com.jilesvangurp.rankquest.core.*
import components.*
import dev.fritz2.core.*
import dev.fritz2.headless.components.modal
import dev.fritz2.headless.components.toast
import dev.fritz2.remote.http
import koin
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import org.koin.core.module.dsl.singleOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.events.Event
import pageLink
import search.SearchResultsStore
import searchpluginconfig.ActiveSearchPluginConfigurationStore
import searchpluginconfig.noConfigYet
import kotlin.time.Duration.Companion.seconds

val ratedSearchesModule = module {
    singleOf(::RatedSearchesStore)
    singleOf(::TestCaseSearchFilterStore)
}

class TestCaseSearchFilterStore : LocalStoringStore<TestCaseSearchFilter>(
    initialData = TestCaseSearchFilter(),
    key = "test-case-search-filter",
    serializer = TestCaseSearchFilter.serializer()
) {
    val addTag = handle<String> { original, tag ->
        (original ?: TestCaseSearchFilter()).copy(tags = (original?.tags.orEmpty() + tag).distinct())
    }

    val removeTag = handle<String> { original, tag ->
        (original ?: TestCaseSearchFilter()).copy(tags = original?.tags.orEmpty() - tag)
    }
}

@Serializable
data class TestCaseSearchFilter(val tags: List<String> = listOf())

class RatedSearchesStore() : LocalStoringStore<List<RatedSearch>>(
    listOf(),
    key = "rated-searches",
    serializer = ListSerializer(RatedSearch.serializer())
) {

    val addOrReplace = handle<RatedSearch> { current, rs ->
        val ratedSearches = current.orEmpty()
        if (ratedSearches.firstOrNull() { it.id == rs.id } == null) {
            infoBubble("Adding ${rs.id} with ${rs.ratings.size} ratings")
            ratedSearches + rs
        } else {
            infoBubble("Updating ${rs.id}")
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
            no = "Cancel",
            job = job
        ) {
            val updated = (current.orEmpty()).filter { it.id != id }
            update(updated)
        }
        current
    }
}

fun RenderContext.importExport() {

    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()

    val showPopup = storeOf(false)
    activeSearchPluginConfigurationStore.data.renderNotNull { searchPluginConfiguration ->

        primaryButton {
            +"Import/Export"
            clicks.map { true } handledBy showPopup.update
        }
        modal {
            openState(showPopup)
            modalPanel("w-full fixed inset-0 overflow-y-auto") {
                div("flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0") {
                    clicks handledBy {
                        showPopup.update(false)
                    }
                    modalOverlay("fixed inset-0 bg-blueMuted-300 bg-opacity-75 transition-opacity") {
                        transition(
                            "ease-out duration-300",
                            "opacity-0",
                            "opacity-100",
                            "ease-in duration-200",
                            "opacity-100",
                            "opacity-0"
                        )
                    }
                    div(
                        """inline-block align-bottom sm:align-middle w-full sm:max-w-4xl px-6 py-5 sm:p-14 
                    | bg-white rounded-lg
                    | shadow-xl transform transition-all 
                    | text-left overflow-hidden""".trimMargin()
                    ) {
                        transition(
                            "ease-out duration-300",
                            "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95",
                            "opacity-100 translate-y-0 sm:scale-100",
                            "ease-in duration-200",
                            "opacity-100 translate-y-0 sm:scale-100",
                            "opacity-0 translate-y-4 sm:translate-y-0 sm:scale-95"
                        )
                        div("mt-3 text-center sm:mt-0 sm:text-left") {
                            modalTitle("text-white bg-blueBright-700 p-2 items-center") {
                                paraCentered {
                                    +"Import / Export"
                                }
                            }


                            div("mt-2") {
                                flexCol {
                                    h1 { +"Ranquest Json Format" }
                                    flexRow {
                                        jsonDownloadButton(
                                            ratedSearchesStore,
                                            "${searchPluginConfiguration.name}-rated-searches-${Clock.System.now()}.json",
                                            ListSerializer(RatedSearch.serializer()),
                                            after = {showPopup.update(false)},
                                        )
                                        jsonFileImport(
                                            serializer = ListSerializer(RatedSearch.serializer()),
                                            after = {showPopup.update(false)}
                                        ) { decoded ->
                                            ratedSearchesStore.update(decoded)
                                        }
                                    }
                                    h1 { +"Ranked Rating Evaluator (RRE) format" }
                                    flexRow {
                                        jsonDownloadButton(
                                            ratedSearchesStore,
                                            "${searchPluginConfiguration.name}-rre-${Clock.System.now()}.json",
                                            ListSerializer(RatedSearch.serializer()),
                                            buttonText = "Download RRE",
                                            converter = { c: List<RatedSearch> ->
                                                DEFAULT_JSON.encodeToString(RRE.serializer(), c.toRRE())
                                            },
                                            after = {showPopup.update(false)}
                                        )
                                        jsonFileImport(
                                            serializer = RRE.serializer(),
                                            buttonText = "RRE Import",
                                            after = {showPopup.update(false)},
                                        ) { decoded ->
                                            ratedSearchesStore.update(decoded.toRatings())
                                        }
                                    }
                                }
                            }

                            flexRowCentered {
                                secondaryButton {
                                    +"Close"
                                    clicks handledBy {
                                        showPopup.update(false)
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

fun RenderContext.testCases() {
    centeredMainPanel {
        val ratedSearchesStore = koin.get<RatedSearchesStore>()
        val activeSearchPluginConfigurationStore = koin.get<ActiveSearchPluginConfigurationStore>()
        val showDemoContentStore = koin.get<Store<Boolean>>(named("showDemo"))
        val testCaseSearchFilterStore = koin.get<TestCaseSearchFilterStore>()

        val showStore = storeOf<Map<String, Boolean>>(mapOf())
        activeSearchPluginConfigurationStore.data.render { searchPluginConfiguration ->
            if (searchPluginConfiguration == null) {
                noConfigYet()
            } else {
                leftRightRow {
                    flexRow {
                        ratedSearchesStore.data.render { ratedSearches ->
                            primaryButton(text = "Clear", iconSource = SvgIconSource.Cross) {
                                disabled(ratedSearches.isNullOrEmpty())
                                clicks handledBy {
                                    confirm(
                                        "Are you sure you want to do this?",
                                        "This remove all your rated searches. Make sure to download your rated searches first!",
                                        job = job
                                    ) {
                                        ratedSearchesStore.update(listOf())
                                        toast("messages", duration = 3.seconds.inWholeMilliseconds) {
                                            +"Cleared!"
                                        }
                                    }
                                }
                            }
                        }
                        importExport()
                        showDemoContentStore.data.render { showDemo ->
                            if (showDemo) {
                                primaryButton {
                                    +"Load Demo Movies Search Test Cases"
                                    clicks handledBy {
                                        confirm(
                                            "Are you sure?",
                                            "This will override your current test cases. Download them first!",
                                            job = job
                                        ) {
                                            http("movie-quotes-test-cases.json").get().body()
                                                .let<String, List<RatedSearch>> { body ->
                                                    DEFAULT_JSON.decodeFromString(body)
                                                }.let { testCases ->
                                                    ratedSearchesStore.update(testCases)
                                                }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    infoPopup(
                        "Creating Test Cases", testCaseScreenHelp
                    )
                }

                ratedSearchesStore.data.render { ratedSearches ->
                    if(ratedSearches.isNullOrEmpty()) {
                        p {
                            +"Create some test cases from the "
                            pageLink(Page.Search)
                            +" screen."
                        }
                    } else {
                        tagFilterEditor()
                        testCaseSearchFilterStore.data.filterNotNull().render { filter ->

                            ratedSearchesStore.data.filterNotNull().map {
                                if (filter.tags.isEmpty()) {
                                    it
                                } else {
                                    it.filter { it.tags?.containsAll(filter.tags) == true }
                                }
                            }.renderEach { rs ->
                                val rsStore = ratedSearchesStore.mapNull(listOf()).mapByElement(rs) { it.id }
                                div {
                                    testCase(showStore, rsStore)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.testCase(showStore: Store<Map<String, Boolean>>, rsStore: Store<RatedSearch>) {
    val ratedSearchesStore = koin.get<RatedSearchesStore>()
    val testCaseSearchFilterStore = koin.get<TestCaseSearchFilterStore>()

    div("flex flex-col mx-10 hover:bg-blueBright-50") {
        showStore.data.render { showMap ->
            rsStore.data.render { ratedSearch ->

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
                        ratedSearch.searchContext.forEach { (k, v) ->
                            +"$k: "
                            b {
                                +v
                                +" "
                            }
                        }
                        +"- Rated results: ${ratedSearch.ratings.size} "
                    }
                    iconButton(SvgIconSource.Delete, "Delete rated search") {
                        clicks.map { ratedSearch.id } handledBy ratedSearchesStore.deleteById
                    }
                }
                if (show) {
                    div {
                        p { +"RsId: ${ratedSearch.id} Rated documents" }
                        p {
                            val rsCommentStore = rsStore.map(RatedSearch::comment.propertyLens { ratedSearch, s ->
                                ratedSearch.copy(comment = s)
                            })
                            rsCommentStore.data.render { comment ->
                                val commentEditingStore = storeOf(false)
                                commentEditingStore.data.render { editing ->
                                    if (editing) {
                                        modalFieldEditor(
                                            "Comment",
                                            commentEditingStore,
                                            rsCommentStore
                                        )
                                    } else {
                                        div("w-full cursor-pointer hover:bg-blueBright-200") {
                                            +(comment ?: "_")
                                            clicks.map { !commentEditingStore.current } handledBy commentEditingStore.update
                                        }
                                    }
                                }
                            }
                        }
                        div {
                            val tagsStore =
                                rsStore.map(RatedSearch::tags.propertyLens { o, ts -> o.copy(tags = ts) }).mapNull(
                                    listOf()
                                )
                            leftRightRow {
                                val showTagEditorStore = storeOf(false)
                                flexRow {

                                    div { +"Tags:" }

                                    testCaseSearchFilterStore.data.render { currentFilter ->
                                        tagsStore.data.renderEach { tag ->
                                            secondaryButton(text = tag) {
                                                disabled(currentFilter?.tags?.contains(tag) == true)
                                                clicks.map { tag } handledBy testCaseSearchFilterStore.addTag
                                            }
                                        }
                                    }
                                }
                                primaryButton(iconSource = SvgIconSource.Edit, text = "Edit Tags") {
                                    clicks.map { true } handledBy showTagEditorStore.update
                                }

                                showTagEditorStore.data.render { show ->
                                    if (show) {
                                        overlay(closeHandler = null) {
                                            val tagsEditStore = storeOf(tagsStore.current)
                                            val newTagStore = storeOf("")
                                            col {
                                                div("mb-6") {
                                                    flexRow {
                                                        textField(placeHolder = "MyKeyword") {
                                                            val element = this
                                                            value(newTagStore)
                                                            keypresss.filter { it.key == "Enter" } handledBy {
                                                                val newTag = newTagStore.current
                                                                if (newTag.isNotBlank()) {
                                                                    tagsEditStore.update((tagsEditStore.current + newTag).distinct())
                                                                    newTagStore.update("")
                                                                }
                                                            }
                                                            inputs handledBy {
                                                                it.target?.dispatchEvent(Event("change"))
                                                            }
                                                        }
                                                        newTagStore.data.render { newTag ->
                                                            primaryButton(iconSource = SvgIconSource.Plus) {
                                                                disabled(newTag.isBlank())
                                                                clicks handledBy {
                                                                    tagsEditStore.update((tagsEditStore.current + newTag).distinct())
                                                                    newTagStore.update("")
                                                                }
                                                            }
                                                        }
                                                    }
                                                    tagsEditStore.data.renderIf({ it.isNotEmpty() }) {
                                                        p {
                                                            +"Current tags:"
                                                        }
                                                        flexRow {
                                                            tagsEditStore.data.renderEach { tag ->
                                                                primaryButton(
                                                                    iconSource = SvgIconSource.Cross,
                                                                    text = tag
                                                                ) {
                                                                    clicks.map { tagsEditStore.current - tag } handledBy tagsEditStore.update
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                flexRowCentered {
                                                    secondaryButton(iconSource = SvgIconSource.Cross, text = "Cancel") {
                                                        clicks.map { false } handledBy showTagEditorStore.update
                                                    }
                                                    primaryButton(text = "OK") {
                                                        clicks.map { tagsEditStore.current } handledBy tagsStore.update
                                                        clicks.map { false } handledBy showTagEditorStore.update
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

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
                        val searchRatingsStore =
                            rsStore.map(RatedSearch::ratings.propertyLens { ratedSearch, searchResultRatings ->
                                ratedSearch.copy(ratings = searchResultRatings)
                            })
                        searchRatingsStore.data.renderEach { searchResultRating ->
                            val searchRatingStore =
                                searchRatingsStore.mapByElement(searchResultRating) { it.documentId }
                            div("flex flex-row w-full gap-2 border-t border-blueMuted-200") {
                                div("w-1/12 bg-blueBright-50") {
                                    iconButton(SvgIconSource.Delete, title = "Remove this result") {
                                        clicks.map {
                                            ratedSearch.copy(ratings = ratedSearch.ratings.filter { it.documentId != searchResultRating.documentId })
                                        } handledBy { modified ->
                                            confirm(
                                                "Remove this result?",
                                                description = "Remove ${searchResultRating.documentId} | ${searchResultRating.label}",
                                                job = job
                                            ) {
                                                rsStore.update(modified)
                                            }
                                        }
                                    }
                                    +" "
                                    +searchResultRating.documentId
                                }
                                div("w-1/12 bg-blueBright-50 hover:bg-blueBright-200") {
                                    val editingStore = storeOf(false)
                                    editingStore.data.render { editing ->
                                        val ratingStore =
                                            searchRatingStore.map(SearchResultRating::rating.propertyLens { o, v ->
                                                o.copy(rating = v)
                                            })
                                        starRating(ratingStore)
                                    }
                                }

                                div("w-7/12") { +(searchResultRating.label ?: "-") }
                                div("w-3/12 bg-blueBright-50 hover:bg-blueBright-200 cursor-pointer") {
                                    val sRRCommentStore =
                                        searchRatingStore.map(SearchResultRating::comment.propertyLens { ratedSearch, comment ->
                                            ratedSearch.copy(
                                                comment = comment
                                            )
                                        })

                                    sRRCommentStore.data.render { comment ->
                                        val commentEditingStore = storeOf(false)
                                        commentEditingStore.data.render { editing ->
                                            if (editing) {
                                                modalFieldEditor(
                                                    "Comment",
                                                    commentEditingStore,
                                                    sRRCommentStore
                                                )
                                            } else {
                                                div("w-full") {
                                                    +(comment ?: "_")
                                                    clicks.map { !commentEditingStore.current } handledBy commentEditingStore.update
                                                }
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
                                overlay(
                                    "absolute top-48 left-1/2 -translate-x-1/2 z-50 bg-white h-96 w-200 p-5 flex flex-col justify-between overflow-auto",
                                    content = {
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
                                    },
                                    closeHandler = null
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun RenderContext.modalFieldEditor(
    title: String,
    editingStore: Store<Boolean>,
    store: Store<String?>,
) {
    val fieldStore = storeOf(store.current ?: "-")
    overlay(closeHandler = null) {
        h1 {
            +title
        }
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
                    fieldStore.current.takeIf { it != "-" && it.isNotBlank() }
                } handledBy store.update
                clicks.map { false } handledBy editingStore.update
            }
        }
    }
}

fun RenderContext.starRating(store: Store<Int>) {
    store.data.render { stars ->

        div("flex flex-row align-middle mx-3", content = fun HtmlTag<HTMLDivElement>.() {
            iconButton(SvgIconSource.Cross, baseClass = "w-6 h-6 fill-blueBright-400 hover:fill-blueBright-800") {
                disabled(stars == 0)
                clicks handledBy {
                    store.update(0)
                }
            }
            (1..5).forEach {
                starButton(stars, it, store)
            }
        })
    }
}

private fun HtmlTag<HTMLDivElement>.starButton(
    stars: Int,
    number: Int,
    store: Store<Int>,
) {
    if (number > stars) {
        iconButton(SvgIconSource.Star, baseClass = "w-6 h-6 fill-blueBright-200 hover:fill-blueBright-600") {
            disabled(stars == number)
            clicks handledBy {
                store.update(number)
            }
        }
    } else {
        iconButton(SvgIconSource.Star, baseClass = "w-6 h-6 fill-blueBright-600 hover:fill-blueBright-100") {
            disabled(stars == number)
            clicks handledBy {
                store.update(number)
            }
        }
    }
}

private val testCaseScreenHelp = """
    This screen allows you to review and modify your test cases. When you create a test case
    from the search screen. Simply do a search and then click the "Add Testcase" button.
    The results simply get rated in descending order. You can then use this 
    screen to change the ratings. 
    
    ## Demo content
    
    If you enable show demo content in the configuration screen and use one of the two demo
    plugins, you can load some sample test cases here and play with those. 
    
    ## What is a Test Case
    
    A test case is a rated search with:
    
    - An id, which is a content hash of the search context
    - A search context with parameters to query your search service
    - A list of rated search results. 
    - A comment field that you can use to document your reasons for the rating or inclusion
    
    ## Ratings and their meaning
    
    A rating is a number of 0 or higher. How high your ratings should be is up to you. But for most
    metrics, something simple like a rating between 0 and 5 should be more than enough.
    
    A rating of zero means the document is not relevant. Higher ratings indicate a higher relevance.
    
    ## Adding more results to a test case
    
    If you do another search in the search tool and then switch back to the test cases screen,
    you can add results from the search screen to any test case. This is a nice way to
    add documents that you know should be produced that the current search does not produce.
    
    Another way to add results to test cases is from the metrics screen. When you review
     the metrics, the the details for each test case will list any unrated results and give you
     the opportunity to add those to the test case.
    
    ## Importing and Exporting
    
    You can download your test cases as a json file and later re-import them. You should use
    this feature to store your ratings in a safe place. A good practice is to keep them in a git
    repository. You can create specialized ratings files for different use cases, topics, etc. 
    
    You also need to do this if you want to use [rankquest-cli](https://github.com/jillesvangurp/rankquest-cli).
""".trimIndent()
