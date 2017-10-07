package com.vaadin.starter.beveragebuddy

import com.fasterxml.jackson.annotation.JsonIgnore
import com.github.vok.framework.sql2o.EntityMeta
import com.github.vok.framework.sql2o.db
import java.io.Serializable

/**
 * See [Entity] for doc. Workaround for https://github.com/vaadin/flow/issues/2631
 */
interface LEntity : Serializable {
    /**
     * The ID primary key. You can use the [Column] annotation to change the actual db column name.
     */
    var id: Long?

    // private since we don't want this to be exposed via e.g. Vaadin Flow.
    @get:JsonIgnore
    private val meta get() = EntityMeta(javaClass)

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
                        .bind(this@LEntity)
                        .executeUpdate()
                id = con.key as Long
            } else {
                val fields = meta.persistedFieldDbNames - meta.idDbname
                con.createQuery("update ${meta.databaseTableName} set ${fields.map { "$it = :$it" }.joinToString()} where ${meta.idDbname} = :${meta.idDbname}")
                        .bind(this@LEntity)
                        .executeUpdate()
            }
        }
    }

    /**
     * Deletes this entity from the database. Fails if [id] is null, since it is expected that the entity is already in the database.
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
