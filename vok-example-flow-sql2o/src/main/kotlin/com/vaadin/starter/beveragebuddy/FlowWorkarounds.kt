package com.vaadin.starter.beveragebuddy

import com.github.vok.framework.sql2o.*
import com.github.vok.framework.sql2o.vaadin.EntityDataProvider
import com.github.vok.framework.sql2o.vaadin.SqlDataProvider
import com.google.gson.*
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.dom.Element
import elemental.json.Json
import elemental.json.JsonValue
import java.io.Serializable
import java.lang.reflect.Type
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * See [Entity] for doc. Workaround for https://github.com/vaadin/flow/issues/2631
 */
interface LEntity : Serializable {
    /**
     * The ID primary key. You can use the [Column] annotation to change the actual db column name.
     */
    var id: Long?

    // private since we don't want this to be exposed via e.g. Vaadin Flow.
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

/**
 * Allows you to simply create a data provider off your entity: `grid.dataProvider = Person.dataProvider`. This data provider
 * doesn't support any joins or more complex queries; to use those please use [SqlDataProvider].
 */
inline val <reified T: LEntity> Dao<T>.dataProvider: DataProvider<T, Filter<T>?> get() = EntityDataProvider(T::class.java, { it.id!! })

// workaround to not to use Flow Json converter, but a standard one.
class GsonJava8Support : JsonDeserializer<Any>, JsonSerializer<Any> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): Any {
        return when(typeOfT) {
            LocalDateTime::class.java -> ZonedDateTime.parse(json.asJsonPrimitive.asString).toLocalDateTime()
            LocalDate::class.java -> LocalDate.parse(json.asJsonPrimitive.asString)
            else -> throw RuntimeException("Unsupported $typeOfT: $json")
        }
    }

    override fun serialize(src: Any?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
        return when(typeOfSrc) {
            LocalDateTime::class.java -> JsonPrimitive((src as LocalDateTime).atZone(ZoneId.of("UTC")).toString())
            LocalDate::class.java -> JsonPrimitive((src as LocalDate).toString())
            else -> throw RuntimeException("Unsupported $typeOfSrc: $src")
        }
    }
}

fun Element.setPropertyGson(property: String, obj: Any) {
    val gson = GsonBuilder()
            .registerTypeAdapter(LocalDateTime::class.java, GsonJava8Support())
            .registerTypeAdapter(LocalDate::class.java, GsonJava8Support())
            .create()
    val tree = gson.toJsonTree(obj)
    val elementalTree: JsonValue = tree.toElementalJson()
    println("SETTING TREE: ${elementalTree.toJson()}")
    setPropertyJson(property, elementalTree)
}

private fun JsonPrimitive.toElementalJson(): JsonValue = when {
    isNumber -> Json.create(asNumber.toDouble())
    isBoolean -> Json.create(asBoolean)
    isString -> Json.create(asString)
    else -> throw RuntimeException("Unexpected value type $this")
}

private fun JsonArray.toElementalJson(): elemental.json.JsonArray {
    val target = Json.createArray()
    forEachIndexed({ index, e -> target.set(index, e.toElementalJson()) })
    return target
}

fun JsonElement.toElementalJson(): JsonValue = when (this) {
    is JsonArray -> this.toElementalJson()
    is JsonNull -> Json.createNull()
    is JsonPrimitive -> this.toElementalJson()
    is JsonObject -> this.toElementalJson()
    else -> throw RuntimeException("Unexpected json type $this")
}
private fun JsonObject.toElementalJson(): JsonValue {
    val target = Json.createObject()
    entrySet().forEach { (k, v) -> target.put(k, v.toElementalJson()) }
    return target
}
