package com.github.vok.framework.flow

import com.github.vok.framework.FilterFactory
import com.vaadin.flow.function.SerializablePredicate
import java.beans.Introspector
import java.lang.reflect.Method

/**
 * A factory which produces predicates as filters. Intended to be used with in-memory [com.vaadin.data.provider.DataProvider]s
 * such as [com.vaadin.data.provider.ListDataProvider].
 */
class PredicateFilterFactory<T: Any> : FilterFactory<SerializablePredicate<T>> {
    override fun and(filters: Set<SerializablePredicate<T>>): SerializablePredicate<T>? = when {
        filters.isEmpty() -> null
        filters.size == 1 -> filters.first()
        else -> And(filters)
    }

    data class And<T>(val filters: Set<SerializablePredicate<T>>) : SerializablePredicate<T> {
        override fun test(t: T): Boolean = filters.all { it.test(t) }
        override fun toString(): String = filters.joinToString(" AND ", "(", ")")
    }

    override fun or(filters: Set<SerializablePredicate<T>>): SerializablePredicate<T>? = when {
        filters.isEmpty() -> null
        filters.size == 1 -> filters.first()
        else -> Or(filters)
    }

    data class Or<T>(val filters: Set<SerializablePredicate<T>>) : SerializablePredicate<T> {
        override fun test(t: T): Boolean = filters.any { it.test(t) }
        override fun toString(): String = filters.joinToString(" OR ", "(", ")")
    }

    override fun eq(propertyName: String, value: Any): SerializablePredicate<T> = Eq(propertyName, value)

    /**
     * Filters beans by comparing given [propertyName] to some expected [value]. Check out implementors for further details.
     */
    abstract class BeanPropertyPredicate<T: Any> : SerializablePredicate<T> {
        abstract val propertyName: String
        abstract val value: Any?
        @Transient
        private var readMethod: Method? = null
        private fun getGetter(item: T): Method {
            if (readMethod == null) {
                val propertyDescriptor = Introspector.getBeanInfo(item.javaClass).propertyDescriptors.first { it.name == propertyName }
                        ?: throw IllegalStateException("Bean ${item.javaClass} has no property $propertyName")
                readMethod = propertyDescriptor.readMethod ?: throw IllegalStateException("Bean ${item.javaClass} has no readMethod for property $propertyDescriptor")
            }
            return readMethod!!
        }
        protected fun getValue(item: T): Any? = getGetter(item).invoke(item)
    }

    data class Eq<T: Any>(override val propertyName: String, override val value: Any) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean = getValue(t) == value
        override fun toString(): String = "$propertyName = $value"
    }

    override fun le(propertyName: String, value: Any): SerializablePredicate<T> = Le(propertyName, value)

    data class Le<T: Any>(override val propertyName: String, override val value: Any) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            val value = getValue(t) as Comparable<Any>? ?: return false
            return value < this.value
        }
        override fun toString(): String = "$propertyName < $value"
    }

    override fun ge(propertyName: String, value: Any): SerializablePredicate<T> = Ge(propertyName, value)

    data class Ge<T: Any>(override val propertyName: String, override val value: Any) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            val value = getValue(t) as Comparable<Any>? ?: return false
            return value > this.value
        }
        override fun toString(): String = "$propertyName > $value"
    }

    override fun ilike(propertyName: String, value: String): SerializablePredicate<T> = Ilike(propertyName, value)

    data class Ilike<T: Any>(override val propertyName: String, override val value: String) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            val value = getValue(t) as String? ?: return false
            return value.contains(this.value, ignoreCase = true)
        }
        override fun toString(): String = "$propertyName ILIKE %$value%"
    }
}
