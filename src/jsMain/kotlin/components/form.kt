package components

import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import dev.fritz2.core.placeholder
import dev.fritz2.headless.components.InputField
import dev.fritz2.headless.components.inputField
import org.w3c.dom.HTMLDivElement

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