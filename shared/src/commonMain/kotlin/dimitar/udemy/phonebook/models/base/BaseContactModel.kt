package dimitar.udemy.phonebook.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BaseContactModel(
    @SerialName("first_name")
    val firstName               : String    = "",
    @SerialName("last_name")
    val lastName                : String    = "",
    val picture                 : String    = "",
    @SerialName("external_id")
    val externalId              : String?   = null
)
