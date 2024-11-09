package searchpluginconfig

import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import pageLink
import utils.md5Hash
import kotlin.random.Random


class SearchContextFieldsStore(fields: List<SearchContextField>, job: Job) : RootStore<List<SearchContextField>>(fields, job)

fun RenderContext.pluginEditorButtonsAndSearchContextEditor(
    selectedPluginStore: Store<String>,
    existing: SearchPluginConfiguration?,
    configNameStore: Store<String>,
    metricConfigurationsStore: Store<List<MetricConfiguration>>,
    settingsGenerator: () -> JsonObject,
    editConfigurationStore: Store<SearchPluginConfiguration?>,
    queryTemplateStore: Store<String>?,
    helpTitle:String,
    helpText: String,
) {
    val pluginConfigurationStore = koin.get<PluginConfigurationsStore>()
    val searchContextFieldsStore = SearchContextFieldsStore(existing?.fieldConfig.orEmpty(), job)
    templateVarEditor(searchContextFieldsStore, queryTemplateStore)

    flexRow {
        secondaryButton {
            +"Cancel"
            clicks.handledBy {
                selectedPluginStore.update("")
                editConfigurationStore.update(null)
            }
        }
        primaryButton {
            if (existing == null) {
                +"Add Configuration"
            } else {
                +"Save"
            }
            clicks.map {
                SearchPluginConfiguration(
                    id = existing?.id ?: md5Hash(Random.nextLong()),
                    name = configNameStore.current,
                    pluginType = selectedPluginStore.current,
                    fieldConfig = searchContextFieldsStore.current,
                    metrics = metricConfigurationsStore.current,
                    pluginSettings = settingsGenerator.invoke()
                )
            } handledBy pluginConfigurationStore.addOrReplace
            clicks handledBy {
                // hide the overlay
                selectedPluginStore.update("")
                editConfigurationStore.update(null)
            }
        }
        infoPopup(helpTitle, helpText)
//        infoModal(helpTitle,helpText)
    }
}

fun RenderContext.templateVarEditor(
    searchContextFieldsStore: Store<List<SearchContextField>>, queryTemplateStore: Store<String>?
) {
    if (queryTemplateStore != null) {
        queryTemplateStore.data handledBy {
            // make sure any new variables from the template are added
            val templateVarsRE = "\\{\\{\\s*(.*?)\\s*\\}\\}".toRegex(RegexOption.MULTILINE)
            val newVars = templateVarsRE.findAll(queryTemplateStore.current).let { matchResult ->
                matchResult.mapNotNull { m ->
                    m.groups[1]?.value?.let { field ->
                        console.log(field)
                        SearchContextField.StringField(field)
                    }
                }
            }.sortedBy { it.name }.distinctBy { it.name }.toList()

            newVars.filter { newVar ->
                console.log(newVar, searchContextFieldsStore.current.toString())
                searchContextFieldsStore.current.firstOrNull { it.name == newVar.name } == null
            }.takeIf { it.isNotEmpty() }?.let { fields ->
                console.log(fields.toString())
                searchContextFieldsStore.update((searchContextFieldsStore.current + fields).distinctBy { it.name })
            }
        }
    }

    div("text-left w-full") {

        h2 { +"Search Context Parameters" }
        para {+"""
            The search context contains all the parameters that your search API supports. 
            Parameters like 'query', 'from', 'size', and other things that your API supports.                       
        """.trimIndent()}
        para {
            +"""
                The search context parameters will be used to populate the search form and their 
                values are stored in test cases as well.
            """.trimIndent()
        }
    }
    searchContextFieldsStore.data.render { fields ->

        fields.forEach { field ->
            val nameStore = storeOf(field.name)
            val helpStore = storeOf(field.help)
            val typeStore = storeOf(field::class.simpleName!!)
            val defaultValueStore = when (field) {
                is SearchContextField.BoolField -> storeOf(field.defaultValue?.toString()?:"")
                is SearchContextField.IntField -> storeOf(field.defaultValue?.toString()?:"")
                is SearchContextField.StringField -> storeOf(field.defaultValue?:"")
            }
            val placeHolderStore = when (field) {
                is SearchContextField.BoolField -> storeOf("")
                is SearchContextField.IntField -> storeOf(field.placeHolder)
                is SearchContextField.StringField -> storeOf(field.placeHolder)
            }


            border {
                flexRow {
                    textField("", "Variable Name") {
                        value(nameStore)
                    }
                    textAreaField(label = "Help Text") {
                        value(helpStore)
                    }
                    defaultValueStore.data.render { defaultValue ->
                        when (field) {
                            is SearchContextField.BoolField -> {
                                val boolStore = storeOf(defaultValue.toBoolean())
                                boolStore.data handledBy {
                                    defaultValueStore.update(it.toString())
                                }
                                switchField {
                                    value(boolStore)
                                }
                            }

                            else -> {
                                textField("", "Default Value") {
                                    value(defaultValueStore)
                                }
                                textField("", "Place Holder") {
                                    value(placeHolderStore)
                                }

                            }
                        }
                    }
                    primaryButton {
                        +"Change"
                        clicks.map {
                            when (typeStore.current) {
                                SearchContextField.BoolField::class.simpleName!! -> {
                                    SearchContextField.BoolField(
                                        name = nameStore.current,
                                        help = helpStore.current,
                                        defaultValue = defaultValueStore.current.takeIf { it.isNotBlank() }?.toBoolean()
                                    )
                                }

                                SearchContextField.IntField::class.simpleName!! -> {
                                    SearchContextField.IntField(
                                        name = nameStore.current,
                                        help = helpStore.current,
                                        defaultValue = defaultValueStore.current.takeIf { it.isNotBlank() }?.toIntOrNull(),
                                        placeHolder = placeHolderStore.current,
                                    )
                                }

                                else -> {
                                    SearchContextField.StringField(
                                        name = nameStore.current,
                                        help = helpStore.current,
                                        defaultValue = defaultValueStore.current.takeIf { it.isNotBlank() },
                                        placeHolder = placeHolderStore.current,
                                    )
                                }
                            }
                        } handledBy { updatedField ->
                            searchContextFieldsStore.update(searchContextFieldsStore.current.map {
                                if (it.name == updatedField.name) {
                                    updatedField
                                } else {
                                    it
                                }
                            })
                        }
                    }
                    secondaryButton(iconSource = SvgIconSource.Cross) {
                        clicks handledBy {
                            searchContextFieldsStore.update(searchContextFieldsStore.current.filter { it.name != field.name })
                        }
                    }
                }

                div("w-full") {
                    flexRow {
                        typeStore.data.render { fieldType ->
                            leftRightRow {
                                para {
                                    +"Set Field Type:"
                                }
                                flexRow {
                                    primaryButton {
                                        +"int"
                                        disabled(fieldType == SearchContextField.IntField::class.simpleName!!)
                                        clicks.map { SearchContextField.IntField::class.simpleName!! } handledBy typeStore.update
                                    }
                                    primaryButton {
                                        +"bool"
                                        disabled(fieldType == SearchContextField.BoolField::class.simpleName!!)
                                        clicks.map { SearchContextField.BoolField::class.simpleName!! } handledBy typeStore.update
                                    }
                                    primaryButton {
                                        +"string"
                                        disabled(fieldType == SearchContextField.StringField::class.simpleName!!)
                                        clicks.map { SearchContextField.StringField::class.simpleName!! } handledBy typeStore.update
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        leftRightRow {

            flexRow {
                val fieldNameStore = storeOf("")
                textField("", "field", description = "Add more search context fields") {
                    value(fieldNameStore)
                    keypresss.filter { it.key == "Enter" } handledBy {

                        val fn = fieldNameStore.current
                        if(fn.isNotBlank()) {
                            searchContextFieldsStore.update(
                                fields + SearchContextField.StringField(
                                    fn,
                                    "",
                                    ""
                                )
                            )
                        }
                    }
                }
                fieldNameStore.data.render { fn ->
                    primaryButton(iconSource = SvgIconSource.Plus) {
                        disabled(fn.isBlank() || fn in fields.map { it.name })
                        clicks handledBy {
                            console.log("adding $fn")
                            searchContextFieldsStore.update(fields + SearchContextField.StringField(fn, "", ""))
                        }
                    }
                }
            }
        }

    }
}

fun RenderContext.mapEditor(store: Store<Map<String, String>>) {
    border {
        val keyStore = storeOf("")
        val valueStore = storeOf("")
        store.data.render { headers ->
            headers.forEach { (hn, hv) ->
                flexRow {
                    primaryButton(iconSource = SvgIconSource.Minus) {
                        clicks handledBy {
                            clicks handledBy {
                                val mutableMap = store.current.toMutableMap()
                                mutableMap.remove(hn)
                                store.update(mutableMap)
                            }
                        }
                    }
                    para { +"$hn: $hv" }
                }
            }
        }
        flexRow {
            textField("", "Name") {
                value(keyStore)
            }
            textField("", "Value") {
                value(valueStore)
            }
            keyStore.data.render { key ->
                valueStore.data.render { value ->

                    primaryButton(iconSource = SvgIconSource.Plus) {
                        disabled(key.isBlank() || value.isBlank())
                        clicks handledBy {
                            val mutableMap = store.current.toMutableMap()
                            mutableMap[key] = value
                            keyStore.update("")
                            valueStore.update("")
                            store.update(mutableMap)
                        }
                    }
                }
            }
        }
    }
}

fun RenderContext.noConfigYet() {
    p {
        +"You don't have any search plugins configured yet. Go to the "
        pageLink(Page.Conf)
        +" to fix it."
    }
}