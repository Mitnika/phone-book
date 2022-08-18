package dimitar.udemy.phonebook.models

enum class InvalidType(val errorMessage: String) {
    EMPTY_FIRST_NAME("First Name is Empty"),
    EMPTY_LAST_NAME("Last Name is Empty"),
    NO_PHONE_NUMBERS("A Contact Must Have At Least One Phone Number"),
    EMPTY_PHONE_NUMBER("A Phone Number Cannot Be Empty"),
    ILLEGAL_SYMBOLS_IN_PHONE_NUMBER("There Cannot Be Letters Or Symbols in a Phone Number except for +, * and #")
}