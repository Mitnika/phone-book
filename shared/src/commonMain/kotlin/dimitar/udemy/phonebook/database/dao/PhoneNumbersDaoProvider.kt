package dimitar.udemy.phonebook.database.dao

object PhoneNumbersDaoProvider {

    private var dao: PhoneNumbersDao? = null

    fun getInstance(): PhoneNumbersDao {
        if (dao == null) {
            dao = PhoneNumbersDao()
        }
        return dao!!
    }
}