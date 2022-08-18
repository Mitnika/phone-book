package dimitar.udemy.phonebook.models.data

import dimitar.udemy.phonebook.models.StateConstants
import dimitar.udemy.phonebook.models.base.BasePhoneModel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PhoneModel(
    val id                      : Long              = -1,
    @SerialName("base_model")
    val baseModel               : BasePhoneModel    = BasePhoneModel(),
    val state                   : String            = StateConstants.STATE_UNEDITABLE
)
