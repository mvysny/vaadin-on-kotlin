package com.github.vok.framework.sql2o

import org.sql2o.Connection

/**
 * Finds all instances of given entity. Fails if there is no table in the database with the name of [databaseTableName]. The list is eager
 * and thus it's useful for smallish tables only.
 */
fun <T : Any> Connection.findAll(clazz: Class<T>): List<T> = createQuery("select * from ${clazz.databaseTableName}").executeAndFetch(clazz)

/**
 * Retrieves entity with given [id]. Returns null if there is no such entity.
 */
fun <T : Entity<*>> Connection.findById(clazz: Class<T>, id: Any): T? =
        createQuery("select * from ${clazz.databaseTableName} where id = :id")
                .addParameter("id", id)
                .executeAndFetchFirst(clazz)

/**
 * Retrieves entity with given [id]. Fails if there is no such entity.
 */
fun <T : Entity<*>> Connection.getById(clazz: Class<T>, id: Any): T =
    requireNotNull(findById<T>(clazz, id)) { "There is no $clazz for id $id" }

/**
 * Deletes all rows from given database table.
 */
fun <T: Any> Connection.deleteAll(clazz: Class<T>) {
    createQuery("delete from ${clazz.databaseTableName}").executeUpdate()
}

/**
 * Counts all rows in given table.
 */
fun <T: Any> Connection.getCount(clazz: Class<T>): Long {
    val count = createQuery("select count(*) from ${clazz.databaseTableName}").executeScalar()
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
inline fun <reified T: Any> Dao<T>.findAll(): List<T> = db { con.findAll<T>(T::class.java) }

/**
 * Retrieves entity with given [id]. Fails if there is no such entity. See [Dao] on how to add this to your entities.
 */
inline operator fun <ID: Any, reified T: Entity<ID>> Dao<T>.get(id: ID): T = db { con.getById(T::class.java, id) }

/**
 * Retrieves entity with given [id]. Returns null if there is no such entity.
 */
inline fun <ID: Any, reified T : Entity<ID>> Dao<T>.findById(id: ID): T? = db { con.findById(T::class.java, id) }

/**
 * Deletes all rows from given database table.
 */
inline fun <reified T: Any> Dao<T>.deleteAll(): Unit = db { con.deleteAll(T::class.java) }

/**
 * Counts all rows in given table.
 */
inline fun <reified T: Any> Dao<T>.count(): Long = db { con.getCount(T::class.java) }

fun <T: Any> Connection.deleteById(clazz: Class<T>, id: Any) = createQuery("delete from ${clazz.databaseTableName} where id=:id")
        .addParameter("id", id)
        .executeUpdate()

/**
 * Deletes row with given ID. Does nothing if there is no such row.
 */
inline fun <reified T: Any> Dao<T>.deleteById(id: Any): Unit = db { con.deleteById(T::class.java, id) }

fun <T: Any> Connection.deleteBy(clazz: Class<T>, block: SqlWhereBuilder<T>.()->Filter<T>) {
    val filter = block(SqlWhereBuilder())
    val query = createQuery("delete from ${clazz.databaseTableName} where ${filter.toSQL92()}")
    filter.getSQL92Parameters().entries.forEach { (name, value) -> query.addParameter(name, value) }
    query.executeUpdate()
}

/**
 * Allows you to delete rows by given where clause:
 *
 * ```
 * Person.deleteBy { "name = :name"("name" to "Albedo") }  // raw sql where clause with parameters
 * Person.deleteBy { Person::name eq "Rubedo" }  // fancy type-safe criteria
 * ```
 *
 * I'm not sure whether this is actually any good - it's too fancy, too smart, too bloody complex. Only useful when
 * you need to construct queries programatically. But if you want more complex stuff or even joins, fall back and just write
 * SQL:
 *
 * ```
 * db { con.createQuery("delete from Foo where name = :name").addParameter("name", name).executeUpdate() }
 * ```
 *
 * Way easier to understand.
 */
inline fun <reified T: Any> Dao<T>.deleteBy(noinline block: SqlWhereBuilder<T>.()->Filter<T>): Unit = db { con.deleteBy(T::class.java, block) }

/**
 * Returns the metadata for this entity.
 */
inline val <reified T: Entity<*>> Dao<T>.meta: EntityMeta get() = EntityMeta(T::class.java)
