package eu.vaadinonkotlin.vaadin10

import eu.vaadinonkotlin.FilterFactory
import com.vaadin.flow.data.provider.ConfigurableFilterDataProvider
import com.vaadin.flow.data.provider.DataProvider
import com.vaadin.flow.function.SerializablePredicate
import java.beans.Introspector
import java.io.Serializable
import java.lang.reflect.Method
import kotlin.reflect.KProperty1

/**
 * A factory which produces predicates as filters. Intended to be used with in-memory [com.vaadin.data.provider.DataProvider]s
 * such as [com.vaadin.data.provider.ListDataProvider].
 */
@Deprecated("use DataLoaderFilterFactory and PredicateFilter")
public open class PredicateFilterFactory<T: Any> : FilterFactory<SerializablePredicate<T>> {
    override fun and(filters: Set<SerializablePredicate<T>>): SerializablePredicate<T>? = when {
        filters.isEmpty() -> null
        filters.size == 1 -> filters.first()
        else -> And(filters)
    }

    public data class And<T>(val filters: Set<SerializablePredicate<T>>) : SerializablePredicate<T> {
        override fun test(t: T): Boolean = filters.all { it.test(t) }
        override fun toString(): String = filters.joinToString(" AND ", "(", ")")
    }

    override fun or(filters: Set<SerializablePredicate<T>>): SerializablePredicate<T>? = when {
        filters.isEmpty() -> null
        filters.size == 1 -> filters.first()
        else -> Or(filters)
    }

    public data class Or<T>(val filters: Set<SerializablePredicate<T>>) : SerializablePredicate<T> {
        override fun test(t: T): Boolean = filters.any { it.test(t) }
        override fun toString(): String = filters.joinToString(" OR ", "(", ")")
    }

    override fun eq(propertyName: String, value: Any?): SerializablePredicate<T> = Eq(propertyName, value)

    /**
     * Filters beans by comparing given [propertyName] to some expected [value]. Check out implementors for further details.
     */
    public abstract class BeanPropertyPredicate<T: Any> : SerializablePredicate<T> {
        public abstract val propertyName: String
        public abstract val value: Any?
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

    public data class Eq<T: Any>(override val propertyName: String, override val value: Any?) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean = getValue(t) == value
        override fun toString(): String = "$propertyName = $value"
    }

    override fun le(propertyName: String, value: Any): SerializablePredicate<T> = Le(propertyName, value)

    public data class Le<T: Any>(override val propertyName: String, override val value: Any) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            @Suppress("UNCHECKED_CAST")
            val value = getValue(t) as Comparable<Any>? ?: return false
            return value < this.value
        }
        override fun toString(): String = "$propertyName < $value"
    }

    override fun ge(propertyName: String, value: Any): SerializablePredicate<T> = Ge(propertyName, value)

    public data class Ge<T: Any>(override val propertyName: String, override val value: Any) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            @Suppress("UNCHECKED_CAST")
            val value = getValue(t) as Comparable<Any>? ?: return false
            return value > this.value
        }
        override fun toString(): String = "$propertyName > $value"
    }

    override fun ilike(propertyName: String, value: String): SerializablePredicate<T> = Ilike(propertyName, value)

    public data class Ilike<T: Any>(override val propertyName: String, override val value: String) : BeanPropertyPredicate<T>() {
        override fun test(t: T): Boolean {
            val value = getValue(t) as String? ?: return false
            return value.contains(this.value, ignoreCase = true)
        }
        override fun toString(): String = "$propertyName ILIKE %$value%"
    }
}

/**
 * Running block with this class as its receiver will allow you to write expressions like this:
 * `Person::age lt 25`. Does not support joins - just use the plain old SQL 92 where syntax for that ;)
 *
 * Containing these functions in this class will prevent polluting of the [KProperty1] interface and also makes it type-safe.
 *
 * This looks like too much Kotlin syntax magic. Promise me to use this for simple Entities and/or programmatic WHERE clause creation only ;)
 */
@Deprecated("Use DataLoader API with PredicateFilter")
public class PredicateFilterBuilder<T: Any> {
    private val ff = PredicateFilterFactory<T>()

    public infix fun <R: Serializable?> KProperty1<T, R>.eq(value: R): SerializablePredicate<T> = ff.eq(name, value)
    @Suppress("UNCHECKED_CAST")
    public infix fun <R> KProperty1<T, R?>.le(value: R): SerializablePredicate<T> = ff.le(name, value as Comparable<Any>)
    @Suppress("UNCHECKED_CAST")
    public infix fun <R> KProperty1<T, R?>.ge(value: R): SerializablePredicate<T> = ff.ge(name, value as Comparable<Any>)
    @Suppress("UNCHECKED_CAST")

    /**
     * An ILIKE filter, performs case-insensitive matching. It performs the 'starts-with' matching which tends to perform quite well on indexed columns. If you need a substring
     * matching, then you actually need to employ full text search
     * capabilities of your database. For example [PostgreSQL full-text search](https://www.postgresql.org/docs/9.5/static/textsearch.html).
     *
     * There is no point in supporting substring matching: it performs a full table scan when used, regardless of whether the column contains
     * the index or not. If you really wish for substring matching, you probably want a full-text search instead which is implemented using
     * a different keywords.
     * @param value the substring, automatically appended with `%` when the SQL query is constructed. The substring is matched
     * case-insensitive.
     */
    public infix fun KProperty1<T, String?>.ilike(value: String): SerializablePredicate<T> = ff.ilike(name, value)
    /**
     * Matches only values contained in given range.
     * @param range the range
     */
    public infix fun <R> KProperty1<T, R?>.between(range: ClosedRange<R>): SerializablePredicate<T> where R: Number, R: Comparable<R> =
        this.ge(range.start as Number).and(this.le(range.endInclusive as Number))

    public val KProperty1<T, Boolean?>.isTrue: SerializablePredicate<T> get() = eq(true)
    public val KProperty1<T, Boolean?>.isFalse: SerializablePredicate<T> get() = eq(false)
}

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return a new data provider which can be outfitted with a custom filter.
 */
@Deprecated("Use DataLoader API with PredicateFilter")
public fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withConfigurableFilter2() : ConfigurableFilterDataProvider<T, SerializablePredicate<T>, SerializablePredicate<T>> =
    withConfigurableFilter({ f1: SerializablePredicate<T>?, f2: SerializablePredicate<T>? -> when {
        f1 == null && f2 == null -> null
        f1 == null -> f2
        f2 == null -> f1
        else -> f1.and(f2)
    } })

/**
 * Produces a new data provider which always applies given [other] filter and restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will chain the data providers and restrict the rows further.
 * @param other applies this filter
 * @return a [VokDataProvider]; setting the [ConfigurableFilterDataProvider.setFilter] won't overwrite the filter specified in this method.
 */
@Deprecated("Use DataLoader API with PredicateFilter")
public fun <T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(other: SerializablePredicate<T>) : ConfigurableFilterDataProvider<T, SerializablePredicate<T>, SerializablePredicate<T>> =
    withConfigurableFilter2().apply {
        // wrap the current DP so that we won't change the filter
        setFilter(other)
        // wrap the DP again so that nobody will change our filter.
    }.withConfigurableFilter2()

/**
 * Produces a new data provider with unremovable filter which restricts rows returned by the receiver data provider.
 * Allows you to write
 * expressions like this: `Person.dataProvider.withFilter { Person::age lt 25 }`
 * See [com.github.mvysny.vokdataloader.FilterBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 * @return a [VokDataProvider]; setting the [ConfigurableFilterDataProvider.setFilter] won't overwrite the filter specified in this method.
 */
@Deprecated("Use DataLoader API with PredicateFilter")
public inline fun <reified T: Any> DataProvider<T, in SerializablePredicate<T>?>.withFilter(block: PredicateFilterBuilder<T>.()-> SerializablePredicate<T>) : ConfigurableFilterDataProvider<T, SerializablePredicate<T>, SerializablePredicate<T>> =
    withFilter(block(PredicateFilterBuilder()))
