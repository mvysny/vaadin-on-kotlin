package com.github.vok.framework.sql2o

import org.sql2o.Connection

/**
 * Finds all instances of given entity. Fails if there is no table in the database with the name of [databaseTableName]. The list is eager
 * and thus it's useful for smallish tables only.
 */
inline fun <reified T : Any> Connection.findAll(): List<T> = createQuery("select * from ${T::class.java.databaseTableName}").executeAndFetch(T::class.java)

/**
 * Retrieves entity with given [id]. Returns null if there is no such entity.
 */
inline fun <reified T : Entity<*>> Connection.findById(id: Any): T? =
        createQuery("select * from ${T::class.java.databaseTableName} where id = :id")
                .addParameter("id", id)
                .executeAndFetchFirst(T::class.java)

/**
 * Retrieves entity with given [id]. Fails if there is no such entity.
 */
inline fun <reified T : Entity<*>> Connection.getById(id: Any): T =
    requireNotNull(findById<T>(id)) { "There is no ${T::class.java} for id $id" }

/**
 * Deletes all rows from given database table.
 */
inline fun <reified T: Any> Connection.deleteAll() {
    createQuery("delete from ${T::class.java.databaseTableName}").executeUpdate()
}

/**
 * Counts all rows in given table.
 */
inline fun <reified T: Any> Connection.getCount(): Long {
    val count = createQuery("select count(*) from ${T::class.java.databaseTableName}").executeScalar()
    return (count as Number).toLong()
}

/**
 * Just let your entity's companion class to implement this interface, say:
 *
 * ```
 * data class Person(...) : Entity<Long> {
 *   companion class : Dao<Person>
 * }
 * ```
 *
 * You can now use `Person.findAll()`, `Person[25]` and other nice methods :)
 */
interface Dao<T: Any>


/**
 * Finds all instances of given entity. Fails if there is no table in the database with the name of [databaseTableName]. The list is eager
 * and thus it's useful for smallish tables only.
 */
inline fun <reified T: Any> Dao<T>.findAll(): List<T> = db { con.findAll<T>() }

/**
 * Retrieves entity with given [id]. Fails if there is no such entity. See [Dao] on how to add this to your entities.
 */
inline operator fun <ID: Any, reified T: Entity<ID>> Dao<T>.get(id: ID): T = db { con.getById(id) }

/**
 * Retrieves entity with given [id]. Returns null if there is no such entity.
 */
inline fun <ID: Any, reified T : Entity<ID>> Dao<T>.findById(id: ID): T? = db { con.findById(id) }

/**
 * Deletes all rows from given database table.
 */
inline fun <reified T: Any> Dao<T>.deleteAll(): Unit = db { con.deleteAll<T>() }

/**
 * Counts all rows in given table.
 */
inline fun <reified T: Any> Dao<T>.count(): Long = db { con.getCount<T>() }
