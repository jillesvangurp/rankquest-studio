package components

import dev.fritz2.core.*
import dev.fritz2.headless.components.InputField
import dev.fritz2.headless.components.inputField
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLInputElement
import org.w3c.files.FileReader
import org.w3c.files.get
import org.w3c.xhr.ProgressEvent
import kotlin.random.Random
import kotlin.random.nextULong

fun RenderContext.textField(
    placeHolder: String? = null,
    inputLabelText: String? = null,
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    initialize: InputField<HTMLDivElement>.() -> Unit
) {
    inputField("flex flex-row border p-2 items-center w-fit", id = id, scope = scope) {
        initialize(this)
        inputLabelText?.let { l ->
            inputLabel("italic mr-5 w-20 text-right") {
                +l
            }
        }
        inputTextfield("w-80 basis-160 bg-blueBright-100 border border-blueBright-300 text-blueBright-900 text-sm rounded-lg focus:ring-blueBright-500 focus:border-blueBright-500 p-2.5") {
            placeHolder?.let { pl ->
                placeholder(pl)
            }
        }
    }
}

// "m-2 w-fit text-white bg-blueBright-600 hover:bg-blueBright-700 disabled:bg-gray-300 focus:ring-button-300 focus:ring-4 font-medium rounded-lg text-sm px-5 py-2.5 focus:outline-none"
fun RenderContext.textFileInput(
    fileType: String,
    textStore: Store<String>,
    baseClass: String? = "file:m-2 file:text-white file:font-medium file:bg-blueBright-600 hover:file:bg-blueBright-700 focus:file:ring-button-300 focus:file:ring-4 file:font-medium file:rounded-lg file:px-5 file:py-2.5 focus:file:outline-none hover:file:cursor-pointer"
)  {

    val fileInputId = "file-input-${Random.nextULong()}"
    input(baseClass = baseClass, id= fileInputId) {
        type("file")
        accept(fileType)
        +"Select Json"
        changes handledBy {
            it.currentTarget?.let {t ->
                val inputElement = t as HTMLInputElement
                inputElement.files?.get(0)?.let {file ->
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
