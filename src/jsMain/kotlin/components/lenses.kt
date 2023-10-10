package components

import dev.fritz2.core.Inspector
import dev.fritz2.core.Lens
import dev.fritz2.core.SubInspector
import dev.fritz2.core.lensOf
import dev.fritz2.validation.ValidationMessage
import kotlin.reflect.KProperty1

/**
 * Short cut to create a lens using a property reference on a data class
 */
fun <T, V> KProperty1<T, V>.propertyLens(setter: (T, V) -> T) = lensOf(name, { get(it) }, setter)

fun <X, D> Inspector<D?>.mapNotNull(lens: Lens<D, X?>): Inspector<X?>? {
    return if (data != null) {
        val parent = this
        // SubInspector does not like nullable things so wrap the Inspector
        SubInspector(object : Inspector<D> {
            override val data: D
                get() = parent.data!!
            override val path: String
                get() = parent.path
        }, lens)
    } else {
        null
    }
}

data class ValidationError(
    val message: String,
    override val path: String,
    override val isError: Boolean = true,
) :  ValidationMessage


