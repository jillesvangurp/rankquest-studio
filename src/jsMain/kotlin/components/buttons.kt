package components

import dev.fritz2.core.HtmlTag
import dev.fritz2.core.RenderContext
import dev.fritz2.core.ScopeContext
import org.w3c.dom.HTMLButtonElement

fun RenderContext.primaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "m-2 w-fit text-white bg-primary-700 hover:bg-blue-800 focus:ring-4 focus:ring-primary-300 font-medium rounded-lg text-sm px-5 py-2.5  dark:bg-primary-800 dark:hover:bg-primary-700 focus:outline-none dark:focus:ring-primary-800",
    id=id,
    scope = scope,
    content = content
)

fun RenderContext.secondaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "m-2 text-white bg-secondary-700 hover:bg-blue-800 focus:ring-4 focus:ring-secondary-300 font-medium rounded-lg text-sm px-5 py-2.5 dark:bg-secondary-800 dark:hover:bg-secondary-700 focus:outline-none dark:focus:ring-secondary-800",
    id=id,
    scope = scope,
    content = content
)

fun RenderContext.tertiaryButton(
    id: String? = null,
    scope: (ScopeContext.() -> Unit) = {},
    content: HtmlTag<HTMLButtonElement>.() -> Unit
) = button(
    baseClass = "m-2 text-white bg-tertiary-700 hover:bg-blue-800 focus:ring-4 focus:ring-tertiary-300 font-medium rounded-lg text-sm px-5 py-2.5 dark:bg-tertiary-800 dark:hover:bg-tertiary-700 focus:outline-none dark:focus:ring-tertiary-800",
    id=id,
    scope = scope,
    content = content
)