package searchpluginconfig

import com.jilesvangurp.rankquest.core.pluginconfiguration.MetricConfiguration
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchContextField
import com.jilesvangurp.rankquest.core.pluginconfiguration.SearchPluginConfiguration
import components.*
import dev.fritz2.core.*
import koin
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonObject
import utils.md5Hash
import kotlin.random.Random


class SearchContextFieldsStore(fields: List<SearchContextField>) : RootStore<List<SearchContextField>>(fields)

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
    val searchContextFieldsStore = SearchContextFieldsStore(existing?.fieldConfig.orEmpty())
    templateVarEditor(searchContextFieldsStore, queryTemplateStore)

    row {
        secondaryButton {
            +"Cancel"
            clicks.map { "_" } handledBy selectedPluginStore.update
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
        infoPopup(helpTitle,helpText)
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

    h2 { +"Search Context Variables" }
    searchContextFieldsStore.data.render { fields ->

        fields.forEach { field ->
            val nameStore = storeOf(field.name)
            val typeStore = storeOf(field::class.simpleName!!)
            val defaultValueStore = when (field) {
                is SearchContextField.BoolField -> storeOf(field.defaultValue.toString())
                is SearchContextField.IntField -> storeOf(field.defaultValue.toString())
                is SearchContextField.StringField -> storeOf(field.defaultValue)
            }
            val placeHolderStore = when (field) {
                is SearchContextField.BoolField -> storeOf("")
                is SearchContextField.IntField -> storeOf(field.placeHolder)
                is SearchContextField.StringField -> storeOf(field.placeHolder)
            }


            border {
                row {
                    textField("", "name") {
                        value(nameStore)
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
                                textField("", "PlaceHolder") {
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
                                        name = nameStore.current, defaultValue = defaultValueStore.current.toBoolean()
                                    )
                                }

                                SearchContextField.IntField::class.simpleName!! -> {
                                    SearchContextField.IntField(
                                        name = nameStore.current,
                                        defaultValue = defaultValueStore.current.toIntOrNull() ?: 0,
                                        placeHolder = placeHolderStore.current,
                                    )

                                }

                                else -> {
                                    SearchContextField.StringField(
                                        name = nameStore.current,
                                        defaultValue = defaultValueStore.current,
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
                    row {
                        typeStore.data.render { fieldType ->
                            leftRightRow {
                                para {
                                    +"Set Field Type:"
                                }
                                row {
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
            para {
                +"Add more search context fields"
            }
            row {
                val fieldNameStore = storeOf("")
                textField("", "field") {
                    value(fieldNameStore)
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
                row {
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
        row {
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
