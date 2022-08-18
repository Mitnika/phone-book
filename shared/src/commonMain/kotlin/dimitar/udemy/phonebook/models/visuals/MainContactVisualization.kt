package dimitar.udemy.phonebook.models.visuals

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MainContactVisualization(
    val id                      : Long      = -1,
    @SerialName("display_name")
    val displayName             : String    = "",
    val picture                 : String    = ""
)