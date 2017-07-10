package com.github.vok.framework.sql2o.vaadin

import com.vaadin.server.SerializablePredicate
import java.beans.Introspector
import java.lang.reflect.Method
import java.util.function.BiPredicate

/**
 * A generic filter which filters items of type [T]. Implementors must define how exactly the items are filtered. The filter is retrofitted as a serializable
 * predicate so that in-memory filtering is also supported.
 *
 * Implementor detail: equals/hashCode/toString must be implemented properly, so that the filter type, the property name and the value which we compare against must be
 * taken into consideration.
 */
interface Filter<T: Any> : SerializablePredicate<T> {
    infix fun and(other: Filter<in T>): Filter<T> = AndFilter(setOf(this, other))
    infix fun or(other: Filter<in T>): Filter<T> = OrFilter(setOf(this, other))
    /**
     * Attempts to convert this filter into a SQL 92 WHERE-clause representation. There are two types of filters:
     * * Filters which do not match column to a value, for example [AndFilter] which produces something like `(filter1 and filter2)`
     * * Filters which do match column to a value, for example [LikeFilter] which produces things like `name LIKE :name`. All [BeanFilter]s are expected
     * to match a database column to a value; that value is automatically prefilled into the JDBC query string under the [BeanFilter.propertyName].
     */
    fun toSQL92(): String = throw IllegalStateException("$this cannot be converted to sql92 filter")
    fun getSQL92Parameters(): Map<String, Any?> = throw IllegalStateException("$this cannot be converted to sql92 filter")
}

/**
 * Filters beans by comparing given [propertyName] to some expected [value]. Check out implementors for further details.
 */
abstract class BeanFilter<T: Any> : Filter<T> {
    abstract val propertyName: String
    abstract val value: Any?
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
    /**
     * A simple way on how to make parameter names unique and tie them to filter instances ;)
     */
    val parameterName get() = "p${System.identityHashCode(this).toString(36)}"
    override fun getSQL92Parameters(): Map<String, Any?> = mapOf(parameterName to value)
}

/**
 * A filter which tests for value equality. Allows nulls.
 */
data class EqFilter<T: Any>(override val propertyName: String, override val value: Any?) : BeanFilter<T>() {
    override fun test(t: T) = getValue(t) == value
    override fun toString() = "$propertyName = $value"
    override fun toSQL92() = "$propertyName = :$parameterName"
}

enum class CompareOperator(val sql92Operator: String) : BiPredicate<Comparable<Any>?, Comparable<Any>> {
    eq("=") { override fun test(t: Comparable<Any>?, u: Comparable<Any>) = t == u },
    lt("<") { override fun test(t: Comparable<Any>?, u: Comparable<Any>) = t != null && t < u },
    le("<=") { override fun test(t: Comparable<Any>?, u: Comparable<Any>) = t != null && t <= u },
    gt(">") { override fun test(t: Comparable<Any>?, u: Comparable<Any>) = t != null && t > u },
    ge(">=") { override fun test(t: Comparable<Any>?, u: Comparable<Any>) = t != null && t >= u },
}

/**
 * A filter which supports less than, less or equals than, etc. Filters out null values.
 */
data class OpFilter<T: Any>(override val propertyName: String, override val value: Comparable<Any>, val operator: CompareOperator) : BeanFilter<T>() {
    @Suppress("UNCHECKED_CAST")
    override fun test(t: T) = operator.test(getValue(t) as Comparable<Any>?, value)
    override fun toString() = "$propertyName ${operator.sql92Operator} $value"
    override fun toSQL92() = "$propertyName ${operator.sql92Operator} :$parameterName"
}

data class IsNullFilter<T: Any>(override val propertyName: String) : BeanFilter<T>() {
    override val value: Any? = null
    override fun test(t: T) = getValue(t) == null
    override fun toSQL92() = "$propertyName is null"
}

data class IsNotNullFilter<T: Any>(override val propertyName: String) : BeanFilter<T>() {
    override val value: Any? = null
    override fun test(t: T) = getValue(t) != null
    override fun toSQL92() = "$propertyName is not null"
}

/**
 * A LIKE filter. Since it does substring matching, it performs quite badly in the databases. You should use full text search
 * capabilities of your database. For example [PostgreSQL full-text search](https://www.postgresql.org/docs/9.5/static/textsearch.html).
 */
class LikeFilter<T: Any>(override val propertyName: String, substring: String) : BeanFilter<T>() {
    private val substring = substring.trim()
    override val value = "%${substring.trim()}%"
    override fun test(t: T) = (getValue(t) as? String)?.contains(substring, ignoreCase = true) ?: false
    override fun toString() = """$propertyName LIKE "$value""""
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as LikeFilter<*>
        if (propertyName != other.propertyName) return false
        if (value != other.value) return false
        return true
    }
    override fun hashCode(): Int {
        var result = value.hashCode()
        result = 31 * result + value.hashCode()
        return result
    }
    override fun toSQL92() = "$propertyName LIKE :$parameterName"
}

class AndFilter<T: Any>(children: Set<Filter<in T>>) : Filter<T> {
    val children: Set<Filter<in T>> = children.flatMap { if (it is AndFilter) it.children else listOf(it) }.toSet()
    override fun test(t: T) = children.all { it.test(t) }
    override fun toString() = children.joinToString(" and ", "(", ")")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as AndFilter<*>
        if (children != other.children) return false
        return true
    }

    override fun hashCode() = children.hashCode()
    override fun toSQL92() = children.joinToString(" and ", "(", ")") { it.toSQL92() }
    override fun getSQL92Parameters(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        children.forEach { map.putAll(it.getSQL92Parameters()) }
        return map
    }
}

class OrFilter<T: Any>(children: Set<Filter<in T>>) : Filter<T> {
    val children: Set<Filter<in T>> = children.flatMap { if (it is OrFilter) it.children else listOf(it) }.toSet()
    override fun test(t: T) = children.any { it.test(t) }
    override fun toString() = children.joinToString(" or ", "(", ")")
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false
        other as OrFilter<*>
        if (children != other.children) return false
        return true
    }
    override fun hashCode() = children.hashCode()
    override fun toSQL92() = children.joinToString(" or ", "(", ")") { it.toSQL92() }
    override fun getSQL92Parameters(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        children.forEach { map.putAll(it.getSQL92Parameters()) }
        return map
    }
}
