package components

import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import dev.fritz2.core.Store
import dev.fritz2.core.accept
import dev.fritz2.core.placeholder
import dev.fritz2.core.selected
import dev.fritz2.core.storeOf
import dev.fritz2.core.transition
import dev.fritz2.core.type
import dev.fritz2.core.value
import dev.fritz2.headless.components.InputField
import dev.fritz2.headless.components.SwitchWithLabel
import dev.fritz2.headless.components.TextArea
import dev.fritz2.headless.components.inputField
import dev.fritz2.headless.components.listbox
import dev.fritz2.headless.components.switchWithLabel
import dev.fritz2.headless.components.textArea
import dev.fritz2.headless.foundation.Aria
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.events.Event
import org.w3c.files.File
import org.w3c.files.FileReader
import org.w3c.files.get
import org.w3c.xhr.ProgressEvent

fun RenderContext.textField(
    placeHolder: String? = null,
    label: String? = null,
    description: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: InputField<HTMLDivElement>.() -> Unit
) {
    border {
        inputField("flex flex-col items-center w-80%", id = id, scope = scope) {
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
                    inputs handledBy { it.target?.dispatchEvent(Event("change")) }
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
                |focus:border-blueBright-500 p-2 h-48""".trimMargin()
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
    store: Store<Pair<File,String>?>,
    fileType: String = ".json",
) {
    // Hidden file input for selecting files
    val fileInput = input("hidden") {
        type("file")
        accept(fileType)
    }.apply {
        domNode.addEventListener("change", { event ->
            console.log("change!")
            val inputElement = event.target as HTMLInputElement
            val file = inputElement.files?.item(0)

            if (file != null) {
                console.log("file!", file.name)
                val reader = FileReader()
                reader.onload = { loadEvent ->
                    val text = (loadEvent.target as FileReader).result as String
                    store.update(file to text)
                }
                reader.readAsText(file)
            } else {
                console.log("null file")
            }
        })
    }

    // Visible button to trigger the hidden file input
    button("btn btn-primary btn-sm") {
        +"Select File"
        clicks handledBy {
            fileInput.domNode.click() // Trigger file selection dialog
        }
    }
    store.data.render { p ->

        span("w-24") {
            if(p==null) {
                +"Select File"
            } else {
                +p.first.name.let { if(it.length>12) "..${it.substring(it.length - 12)}" else it }
            }
        }
    }
}

fun RenderContext.selectBox(
    selectedStore: Store<String>,
    items: List<String>,
) {
    selectedStore.data.render { selectedItem ->
        select("select select-bordered w-full max-w-xs") {
            value(selectedStore.data)
            selects handledBy {
                val element = it.target as HTMLSelectElement
                selectedStore.update(element.value)
            }
            items.forEach { item ->
                if (item == selectedItem) {
                    option {
                        selected(true)
                        +item
                    }
                } else {
                    option {
                        +item
                    }
                }
            }
        }
    }

//    listbox {
//        value(selectedStore)
//        listboxButton(
//            """bg-blueBright-700 border border-blueBright-500 text-white text-sm rounded-lg
//            |focus:ring-blueBright-600 focus:border-blueBright-500 block w-40 p-2.5""".trimMargin()
//        ) {
//            span { value.data.renderText() }
//        }
//        listboxItems("flex flex-col") {
//            transition(
//                on = opened,
//                enter = "transition duration-100 ease-out",
//                enterStart = "opacity-0 scale-95",
//                enterEnd = "opacity-100 scale-100",
//                leave = "transition duration-100 ease-in",
//                leaveStart = "opacity-100 scale-100",
//                leaveEnd = "opacity-0 scale-95"
//            )
//
//            // using a loop is a typical pattern to create the options
//            (listOfNotNull(emptyItem) + items).forEach { entry ->
//                listboxItem(entry) {
//                    span("block bg-blueBright-200 hover:bg-blueBright-300 text-blueBright-900 p-2") { +entry }
//                }
//            }
//        }
//    }
}
