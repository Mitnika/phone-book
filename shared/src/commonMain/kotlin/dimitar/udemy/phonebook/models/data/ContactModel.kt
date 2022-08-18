package dimitar.udemy.phonebook.models.data

import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.base.BaseContactModel
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContactModel(
    val id                      : Long              = -1,
    @SerialName("base_model")
    val baseModel               : BaseContactModel  = BaseContactModel(),
    val state                   : String            = StateConstants.STATE_UNEDITABLE
)
