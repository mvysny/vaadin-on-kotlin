package com.vaadin.starter.beveragebuddy.backend

import com.github.vokorm.*

/**
 * Represents a beverage category.
 * @property id
 * @property name the category name
 */
// must be open: https://github.com/vaadin/flow/issues/2636
open class Category(override var id: Long? = null, open var name: String = "") : Entity<Long> {

    companion object : Dao<Category> {
        fun findByName(name: String): Category? = findBy(1) { Category::name eq name } .firstOrNull()
        fun findByNameOrThrow(name: String): Category = findByName(name) ?: throw IllegalArgumentException("No category named $name")
        fun existsWithName(name: String): Boolean = findByName(name) != null
        fun deleteAll() {
            db {
                con.createQuery("update Review set category = NULL").executeUpdate()
                con.deleteAll(Category::class.java)
            }
        }
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

    override fun delete() {
        db {
            if (id != null) {
                con.createQuery("update Review set category = NULL where category=:catId")
                        .addParameter("catId", id!!)
                        .executeUpdate()
            }
            super.delete()
        }
    }
}
