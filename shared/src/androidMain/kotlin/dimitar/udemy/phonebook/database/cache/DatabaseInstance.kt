package dimitar.udemy.phonebook.database.cache

//
//class DatabaseInstance {
//    companion object {
//        @Volatile private var INSTANCE: Database? = null
//        fun getInstance(context: Context): Database {
//            synchronized(this) {
//                var instance = INSTANCE
//
//                if (instance == null) {
//                    instance = Database(DatabaseDriverFactory(context))
//                    INSTANCE = instance
//                }
//                return instance
//            }
//        }
//    }
//
//}