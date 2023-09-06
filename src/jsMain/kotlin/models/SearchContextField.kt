package models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

//@Serializable
//sealed interface SearchContextField {
//    val name: String
//
//    @Serializable
//    @SerialName("str")
//    data class StringField(
//        override val name: String,
//        @EncodeDefault val placeHolder: String = "enter a query"
//    ): SearchContextField
//
//    @Serializable
//    @SerialName("int")
//    data class IntField(
//        override val name: String,
//        val defaultValue: Int = 0
//    ): SearchContextField
//
//    @Serializable
//    @SerialName("bool")
//    data class BoolField(
//        override val name: String,
//        val defaultValue: Boolean = false
//    ): SearchContextField
//}