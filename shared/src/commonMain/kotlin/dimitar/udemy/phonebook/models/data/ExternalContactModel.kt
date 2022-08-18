package dimitar.udemy.phonebook.models.data

import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ExternalContactModel(
    @SerialName("base_contact")
    val baseContact                 : BaseContactModel      = BaseContactModel(),
    @SerialName("phone_numbers")
    val phoneNumbers                : List<BasePhoneModel>  = ArrayList()
)