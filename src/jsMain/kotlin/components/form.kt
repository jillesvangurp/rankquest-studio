package components

import dev.fritz2.core.*
import dev.fritz2.headless.components.*
import dev.fritz2.headless.foundation.Aria
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import org.w3c.xhr.ProgressEvent
import kotlin.random.Random
import kotlin.random.nextULong

fun RenderContext.textField(
    placeHolder: String? = null,
    label: String? = null,
    description: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: InputField<HTMLDivElement>.() -> Unit
) {
    border {
        inputField("flex flex-col items-center w-full", id = id, scope = scope) {
            initialize(this)
            div("flex flex-row items-center w-full") {
                label?.let { l ->
                    inputLabel("italic mr-5 w-2/6 text-right") {
                        +l
                    }
                }
                inputTextfield(
                    """w-4/6 basis-160 bg-blueBright-100 border border-blueBright-300 text-blueBright-900 
                |text-sm rounded-lg focus:ring-blueBright-500 focus:border-blueBright-500 p-2.5""".trimMargin()
                ) {
                    placeHolder?.let { pl ->
                        placeholder(pl)
                    }
                }
            }
            description?.let {
                inputDescription("block w-full") {
                    div {
                        +description
                    }
                }
            }
        }
    }
}

fun RenderContext.textAreaField(
    placeHolder: String? = null,
    label: String? = null,
    description: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: TextArea<HTMLDivElement>.() -> Unit
) {
    border {
        textArea("flex flex-col items-center w-full", id = id, scope = scope) {
            initialize(this)
            div("flex flex-row items-center w-full") {
                label?.let { l ->
                    textareaLabel("italic mr-5 w-2/6 text-right") {
                        +l
                    }
                }
                textareaTextfield(
                    """w-4/6 basis-160 bg-blueBright-100 border border-blueBright-300 
                |text-blueBright-900 text-sm rounded-lg focus:ring-blueBright-500 
                |focus:border-blueBright-500 p-2""".trimMargin()
                ) {
                    placeHolder?.let { pl ->
                        placeholder(pl)
                    }
                }
            }
            description?.let {
                textareaDescription("block w-full") {
                    div {
                        +description
                    }
                }
            }
        }
    }
}

fun RenderContext.switchField(
    label: String? = null,
    description: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: SwitchWithLabel<HTMLDivElement>.() -> Unit
) {
    border {
        switchWithLabel("flex flex-col place-items-center w-full", id = id, scope = scope) {
            initialize(this)

            div("flex flex-row items-center w-full") {
                label?.let {
                    switchLabel("italic mr-5 w-2/6 text-right") {
                        +label
                    }
                }
                div("w-4/6") {
                    switchToggle(
                        """relative inline-flex flex-shrink-0 h-6 w-11
                    | cursor-pointer rounded-full
                    | border-2 border-transparent ring-1 ring-blueBright-400  
                    | transition-colors ease-in-out duration-200 
                    | focus:outline-none focus:ring-4 focus:ring-blueBright-600""".trimMargin()
                    ) {

                        span("sr-only") { +"Use setting" }
                        className(enabled.map { if (it) "bg-blueBright-800" else "bg-blueBright-200" })
                        span(
                            """inline-block h-5 w-5 
                    | rounded-full bg-white shadow pointer-events-none 
                    | ring-0 
                    | transform transition ease-in-out duration-200""".trimMargin()
                        ) {
                            className(enabled.map { if (it) "translate-x-5" else "translate-x-0" })
                            attr(Aria.hidden, "true")
                        }
                    }
                }
            }
            description?.let {
                switchDescription("block w-full") {
                    div {
                        +description
                    }
                }
            }
        }
    }
}

fun RenderContext.textFileInput(
    textStore: Store<String>,
    fileType: String=".json",
    fileInputId:String = "file-input-${Random.nextULong()}",
    baseClass: String? = """file:my-2 file:text-white file:font-medium file:bg-blueBright-600 hover:file:bg-blueBright-700 
        |focus:file:ring-button-300 focus:file:ring-4 file:font-medium file:rounded-lg file:px-5 file:py-2.5 
        |focus:file:outline-none hover:file:cursor-pointer""".trimMargin()
) {
    input(baseClass = baseClass, id = fileInputId) {
        type("file")
        accept(fileType)
        +"Select Json"
        changes handledBy {
            it.currentTarget?.let { t ->
                val inputElement = t as HTMLInputElement
                inputElement.files?.get(0)?.let { file ->
                    val reader = FileReader()
                    reader.onload = {
                        val pe = it as ProgressEvent
                        val text = (pe.target as FileReader).result as String
                        textStore.update(text)
                        it
                    }
                    reader.readAsText(file)
                }
            }
        }
    }
}

fun RenderContext.selectBox(
    selectedStore: Store<String>,
    items: List<String>,
    emptyItem: String? = null,
) {
    listbox {
        value(selectedStore)
        listboxButton("""bg-blueBright-700 border border-blueBright-500 text-white text-sm rounded-lg 
            |focus:ring-blueBright-600 focus:border-blueBright-500 block w-40 p-2.5""".trimMargin()) {
            span { value.data.renderText() }
        }
        listboxItems("flex flex-col") {
            transition(
                on = opened,
                enter = "transition duration-100 ease-out",
                enterStart = "opacity-0 scale-95",
                enterEnd = "opacity-100 scale-100",
                leave = "transition duration-100 ease-in",
                leaveStart = "opacity-100 scale-100",
                leaveEnd = "opacity-0 scale-95"
            )

            // using a loop is a typical pattern to create the options
            (listOfNotNull(emptyItem) + items).forEach { entry ->
                listboxItem(entry) {
                    span("block bg-blueBright-200 hover:bg-blueBright-300 text-blueBright-900 p-2") { +entry }
                }
            }
        }
    }
}
