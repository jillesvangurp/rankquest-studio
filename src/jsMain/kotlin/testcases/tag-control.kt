package testcases

import components.*
import dev.fritz2.core.RenderContext
import koin
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map

fun RenderContext.tagFilterEditor() {
    val testCaseSearchFilterStore = koin.get<TestCaseSearchFilterStore>()
    val ratedSearchesStore = koin.get<RatedSearchesStore>()

    testCaseSearchFilterStore.data.filterNotNull().render { filter ->
        leftRightRow {

            flexRow {

                ratedSearchesStore.data.renderNotNull { testCases ->
                    val allTags = testCases.flatMap { it.tags.orEmpty() }.toSet().sorted()
                    if (filter.tags.isNotEmpty()) {
                        p {
                            +"Tags:"
                        }
                    } else {
                        if(allTags.isEmpty()) {
                            p {
                                +"Add some tags to your test cases to enable tag filtering."
                            }
                        } else {
                            p {
                                +"No tags selected."
                            }
                        }
                    }
                    filter.tags.forEach { tag ->
                        secondaryButton(iconSource = SvgIconSource.Delete, text = tag) {
                            clicks.map { tag } handledBy testCaseSearchFilterStore.removeTag
                        }
                    }
                    allTags.filter { !filter.tags.contains(it) }.forEach { tag ->
                        primaryButton(iconSource = SvgIconSource.Plus, text = tag) {
                            clicks.map { tag } handledBy testCaseSearchFilterStore.addTag
                        }
                    }
                }
            }
            infoPopup(
                "Tag Filtering",
                """
                Use tags to filter your test cases. You can add tags to your test cases to make it easier to
                find them back, group them by search feature, etc.
                
                When running metrics, the tag filter applies as well. You can usw this to group test 
                cases and evaluate the impact of changes on particular groups of test cases.
                """.trimIndent()
            )
        }
    }
}