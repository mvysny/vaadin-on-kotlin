package example.crud_sql2o

import example.crud_sql2o.LastAddedPersonCache.Companion.get
import example.crud_sql2o.personeditor.Person
import eu.vaadinonkotlin.vaadin8.Session
import org.slf4j.LoggerFactory
import java.io.Serializable

/**
 * A demo of a session-bound class which caches the last added person. Use the [Session.lastAddedPersonCache] global property to retrieve instances.
 *
 * Note the private constructor, to avoid accidentally constructing this class by hand. To correctly manage instances of this class, simply
 * let [get] do it for you.
 * @author mvy
 */
class LastAddedPersonCache private constructor() : Serializable {
    init {
        log.warn("LastAddedPersonCache created")
    }

    /**
     * A simple placeholder where the last-created person is stored.
     */
    var lastAdded: Person? = null

    companion object {
        private val log = LoggerFactory.getLogger(LastAddedPersonCache::class.java)
        internal fun get() = Session.getOrPut { LastAddedPersonCache() }
    }
}

/**
 * Retrieves the session-scoped instance of this class, creating and binding it to the current session if necessary.
 *
 * WARNING: you can only read the property while holding the Vaadin UI lock! That is, this class is not accessible from a background thread.
 */
val Session.lastAddedPersonCache: LastAddedPersonCache get() = LastAddedPersonCache.get()
