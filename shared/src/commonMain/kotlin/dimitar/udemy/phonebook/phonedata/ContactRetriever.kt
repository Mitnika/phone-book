package dimitar.udemy.phonebook.phonedata

import dimitar.udemy.phonebook.presenters.MainPresenter

expect class ContactRetriever {
    internal fun requestContactsFromPhone(mainPresenter: MainPresenter)

}