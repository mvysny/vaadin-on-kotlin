package com.vaadin.starter.beveragebuddy.backend

import com.github.vok.framework.sql2o.Dao
import com.github.vok.framework.sql2o.findBy
import com.vaadin.starter.beveragebuddy.LEntity

/**
 * Represents a beverage category.
 * @property id
 * @property name the category name
 */
// must be open - Flow requires it to create ModelProxy
open class Category(override var id: Long? = null, var name: String = "") : LEntity {

    companion object : Dao<Category> {
        val UNDEFINED = Category(name = "(undefined)")
        fun findByName(name: String): Category? = findBy(1) { Category::name eq name } .firstOrNull()
        fun findByNameOrThrow(name: String): Category = findByName(name) ?: throw IllegalArgumentException("No category named $name")
    }

    override fun toString() = "Category(id=$id, name='$name')"

    fun copy() = Category(id, name)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Category
        if (id != other.id) return false
        return true
    }

    override fun hashCode(): Int = id?.hashCode() ?: 0
}
