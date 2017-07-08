package com.github.vok.framework.sql2o

import java.io.Serializable
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

/**
 * Optional annotation which allows you to change the table name.
 * @property dbname the database table name; defaults to an empty string which will use the [Class.getSimpleName] as the table name.
 */
@Target(AnnotationTarget.CLASS)
annotation class Table(val dbname: String = "")

/**
 * Annotate a field with this to exclude it from being mapped into a database table column.
 */
@Target(AnnotationTarget.FIELD)
annotation class Ignore

/**
 * Establishes a very simple mapping between an object and a database table, and adds useful utility methods [save]
 * and [delete].
 *
 * Automatically will try to store/update/retrieve all non-transient fields declared by this class and all superclasses; either use
 * [Transient] or [Ignore] to exclude fields.
 *
 * Note that [Sql2o] works with all pojos and does not require any annotation/interface. Thus, if your table has no primary
 * key or there is other reason you don't want to use this interface, you can still use your class with [db], you'll only
 * lose those utility methods.
 */
interface Entity<ID> : Serializable {
    /**
     * The ID primary key. You can use the [Column] annotation to change the actual db column name.
     */
    var id: ID?

    /**
     * The meta-data about this entity.
     */
    val meta get() = EntityMeta(javaClass)

    /**
     * Creates a new row in a database (if [id] is null) or updates the row in a database (if [id] is not null).
     *
     * When creating, expects the [id] field to be null. It is expected that the database will generate an id for us (by sequences,
     * `auto_increment` or other means). That generated ID is then automatically stored into the [id] field.
     */
    fun save() {
        db {
            if (id == null) {
                // not yet in the database, insert
                val fields = meta.persistedFieldDbNames - meta.idDbname
                con.createQuery("insert into ${meta.databaseTableName} (${fields.joinToString()}) values (${fields.map { ":$it" }.joinToString()})")
                        .bind(this@Entity)
                        .executeUpdate()
                id = con.key as ID
            } else {
                val fields = meta.persistedFieldDbNames - meta.idDbname
                con.createQuery("update ${meta.databaseTableName} set ${fields.map { "$it = :$it" }.joinToString()} where ${meta.idDbname} = :${meta.idDbname}")
                        .bind(this@Entity)
                        .executeUpdate()
            }
        }
    }

    /**
     * Deletes this entity from the database. Fails if [id] is null, since it is expected that the entity is not yet in the database.
     */
    fun delete() {
        check(id != null) { "The id is null, the entity is not yet in the database" }
        db {
            con.createQuery("delete from ${meta.databaseTableName} where ${meta.idDbname} = :id")
                    .addParameter("id", id)
                    .executeUpdate()
        }
    }
}

data class EntityMeta(val entity: Class<out Entity<*>>) : Serializable {
    /**
     * The name of the database table backed by this entity. Defaults to [Class.getSimpleName]
     * (no conversion from `camelCase` to `hyphen_separated`) but you can annotate your class with [Table.dbname] to override
     * that.
     */
    val databaseTableName: String get() = entity.databaseTableName
    /**
     * A list of database names of all persisted fields in this entity.
     */
    val persistedFieldDbNames: Set<String> get() = entity.persistedFieldNames

    /**
     * The database name of the ID column.
     */
    val idDbname: String get() {
        val idField = checkNotNull(entity.findDeclaredField("id")) { "Unexpected: entity $entity has no id column?" }
        return idField.dbname
    }
}

private fun Class<*>.findDeclaredField(name: String): Field? {
    if (this == Object::class.java) return null
    val f = declaredFields.firstOrNull { it.name == "id" }
    if (f != null) return f
    return superclass.findDeclaredField(name)
}

/**
 * Returns the name of the database table backed by this entity. Defaults to [Class.getSimpleName]
 * (no conversion from `camelCase` to `hyphen_separated`) but you can annotate your class with [Table.dbname] to override
 * that.
 */
val Class<*>.databaseTableName: String get() {
    val annotatedName = getAnnotation(Table::class.java)?.dbname
    return if (annotatedName != null && annotatedName.isNotBlank()) annotatedName else simpleName
}

private inline val Field.isTransient get() = Modifier.isTransient(modifiers)

private val Field.isPersisted get() = !isTransient && !isAnnotationPresent(Ignore::class.java)

/**
 * Lists all persisted fields
 */
private val Class<*>.persistedFields: List<Field> get() = when {
    this == Object::class.java -> listOf()
    else -> declaredFields.filter { it.isPersisted } + superclass.persistedFields
}

private val persistedFieldNamesCache: ConcurrentMap<Class<*>, Set<String>> = ConcurrentHashMap<Class<*>, Set<String>>()

/**
 * The database name of given field. Defaults to [Field.name], cannot be currently changed.
 */
private val Field.dbname: String get() = name

/**
 * Returns the list of database column names in an entity.
 */
val <T : Entity<*>> Class<T>.persistedFieldNames: Set<String> get()
// thread-safety: this may compute the same value multiple times during high contention, this is OK
= persistedFieldNamesCache.getOrPut(this) { (persistedFields.map { it.dbname }).toSet() }
