package dimitar.udemy.phonebook.models.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileModel(
    @SerialName("contact_model")
    val contactModel                : ContactModel      = ContactModel(),
    val phones                      : List<PhoneModel>  = ArrayList()
)
