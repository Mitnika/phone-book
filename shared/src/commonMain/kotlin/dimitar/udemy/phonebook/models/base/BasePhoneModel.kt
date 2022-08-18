package dimitar.udemy.phonebook.models.base

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BasePhoneModel(
    val number                  : String    = "",
    @SerialName("external_id")
    val externalId              : String?   = null
)
