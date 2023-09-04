@file:OptIn(ExperimentalSerializationApi::class)

package searchpluginconfig

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.koin.dsl.module

val searchPluginConfigModule = module {

}

@Serializable
sealed interface SearchContextField {
    val name: String

    @Serializable
    @SerialName("str")
    data class StringField(
        override val name: String,
        @EncodeDefault val placeHolder: String = "enter a query"
    ):SearchContextField

    @Serializable
    @SerialName("int")
    data class IntField(
        override val name: String,
        val defaultValue: Int = 0
    ):SearchContextField

    @Serializable
    @SerialName("bool")
    data class BoolField(
        override val name: String,
        val defaultValue: Boolean = false
    ):SearchContextField
}

@Serializable
data class SearchPluginConfiguration(val pluginName: String, val fieldConfig: List<SearchContextField>, val pluginSettings: JsonObject)
