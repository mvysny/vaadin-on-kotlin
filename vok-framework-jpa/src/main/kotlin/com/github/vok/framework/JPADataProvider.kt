package com.github.vok.framework

import com.vaadin.data.provider.*
import com.vaadin.shared.data.sort.SortDirection
import java.io.Serializable
import java.util.stream.Stream
import javax.persistence.criteria.*
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

/**
 * Provides instances of given JPA class from the database. Currently only supports sorting, currently does not support joins.
 * @todo mavi add support for simple joins, that is, the provider will still only provide Employee but employee.department.id=23 or some
 *
 * This provider only supports listing of a single bean. If you need to list properties of two or more beans, or fields of some
 * more complex query, you'll need to write a data provider yourself. It is actually a very easy thing to do, please see sources of this
 * class for details.
 * @todo mavi maybe offer possibility to have auto-generate grid filters on custom data provider?
 */
class JPADataProvider<T: Any>(val entity: KClass<T>) : AbstractBackEndDataProvider<T, JPAFilter?>() {

    private fun QuerySortOrder.toOrder(cb: CriteriaBuilder, root: Root<T>) = if (direction == SortDirection.ASCENDING) cb.asc(root.get<T>(sorted)) else cb.desc(root.get<T>(sorted))

    override fun fetchFromBackEnd(query: Query<T, JPAFilter?>): Stream<T> = db {
        em.criteriaBuilder.let { cb ->
            val q = cb.createQuery(entity.java)
            val root = q.from(entity.java)
            q.orderBy(query.sortOrders.map { it.toOrder(cb, root) })
            if (query.filter.isPresent) q.where(query.filter.get().toPredicate(cb, root))
            val q2 = em.createQuery(q)
            q2.firstResult = query.offset
            if (query.limit < Int.MAX_VALUE) q2.maxResults = query.limit
            q2.resultList
        }.stream()
    }

    override fun sizeInBackEnd(query: Query<T, JPAFilter?>): Int = db {
        em.criteriaBuilder.let { cb ->
            val q = cb.createQuery(Long::class.java)
            val root = q.from(entity.java)
            q.select(cb.count(root))
            if (query.filter.isPresent) q.where(query.filter.get().toPredicate(cb, root))
            em.createQuery(q).singleResult.toInt()
        }
    }

    override fun getId(item: T): Any = item.dbId!!
    override fun toString() = "JPADataProvider($entity)"
}

/**
 * Utility method to create [JPADataProvider] like this: `jpaDataProvider<Person>()` instead of `JPADataProvider(Person::class)`
 */
inline fun <reified T : Any> jpaDataProvider(): JPADataProvider<T> = JPADataProvider(T::class)

/**
 * Wraps this data provider in a configurable filter, regardless of whether this data provider is already a configurable filter or not.
 * @return data provider which can be configured to always apply given filter.
 */
fun <T> DataProvider<T, JPAFilter?>.configurableFilter() : ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?> =
    withConfigurableFilter({ f1: JPAFilter?, f2: JPAFilter? -> listOf(f1, f2).filterNotNull().toSet().and() })

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param other applies this filter
 */
fun <T> DataProvider<T, JPAFilter?>.and(other: JPAFilter) : DataProvider<T, JPAFilter?> = configurableFilter().apply {
    setFilter(other)
}

/**
 * Produces a new data provider which restricts rows returned by the original data provider to given filter. Allows you to write
 * expressions like this: `jpaDataProvider<Person>().and { Person::age lt 25 }`
 * See [JPAWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will restrict the rows further.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T> DataProvider<T, JPAFilter?>.and(block: JPAWhereBuilder<T>.()-> JPAFilter) : DataProvider<T, JPAFilter?> = configurableFilter().apply {
    setFilter(block)
}

/**
 * Removes the original filter and sets the new filter. Allows you to write
 * expressions like this: `jpaDataProvider<Person>().and { Person::age lt 25 }`.
 * See [JPAWhereBuilder] for a complete list of applicable operators.
 *
 * Invoking this method multiple times will overwrite the previous filter.
 * @param block the block which allows you to build the `where` expression.
 */
fun <T> ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?>.setFilter(block: JPAWhereBuilder<T>.()-> JPAFilter) {
    setFilter(block(JPAWhereBuilder()))
}

fun Set<JPAFilter>.and(): JPAFilter? = when (size) {
    0 -> null
    1 -> iterator().next()
    else -> AndFilter(this)
}
fun Set<JPAFilter>.or(): JPAFilter? = when (size) {
    0 -> null
    1 -> iterator().next()
    else -> OrFilter(this)
}

/**
 * A JPA filter. See concrete implementation classes for further information.
 */
interface JPAFilter : Serializable {
    /**
     * Converts this filter to a JPA Criteria API Predicate.
     * @param cb the criteria builder which contains factory methods for various Predicates.
     * @param root creates Expressions for fields
     * @return a predicate which actually matches the database rows.
     */
    fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate

    /**
     * Negates this filter.
     * @return this filter negated.
     */
    operator fun not(): JPAFilter = NotFilter(this)

    /**
     * Produces a filter which only matches rows which are matched both by this and the other filter.
     * @param other rows must match also this filter.
     * @return AND of `this` and `other`
     */
    infix fun and(other: JPAFilter): JPAFilter = setOf(this, other).and()!!
    /**
     * Produces a filter which matches rows which are matched by either this or the other filter.
     * @param other rows may match also this filter.
     * @return OR of `this` and `other`
     */
    infix fun or(other: JPAFilter): JPAFilter = setOf(this, other).or()!!
}

data class AndFilter(val filters: Set<JPAFilter>) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.and(*filters.map { it.toPredicate(cb, root) }.toTypedArray())
    override fun toString() = filters.joinToString(" and ", transform = { it -> "($it)" })
}
data class OrFilter(val filters: Set<JPAFilter>) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.or(*filters.map { it.toPredicate(cb, root) }.toTypedArray())
    override fun toString() = filters.joinToString(" or ", transform = { it -> "($it)" })
}
data class NotFilter(val filter: JPAFilter) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.not(filter.toPredicate(cb, root))
    override fun toString() = "!$filter"
}
data class EqFilter(val field: String, val value: Serializable?) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.equal(root.get<Any>(field), value)
    override fun toString() = "$field == $value"
}
data class LeFilter(val field: String, val value: Number) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.le(root.get<Number>(field), value)
    override fun toString() = "$field <= $value"
}
data class Le2Filter<V: Comparable<V>>(val field: String, val value: V) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.lessThanOrEqualTo(root.get<V>(field), value)
    override fun toString() = "$field <= $value"
}
data class LtFilter(val field: String, val value: Number) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.lt(root.get<Number>(field), value)
    override fun toString() = "$field < $value"
}
data class Lt2Filter<V: Comparable<V>>(val field: String, val value: V) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.lessThan(root.get<V>(field), value)
    override fun toString() = "$field < $value"
}
data class GeFilter(val field: String, val value: Number) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.ge(root.get<Number>(field), value)
    override fun toString() = "$field >= $value"
}
data class Ge2Filter<V: Comparable<V>>(val field: String, val value: V) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.greaterThanOrEqualTo(root.get<V>(field), value)
    override fun toString() = "$field >= $value"
}
data class GtFilter(val field: String, val value: Number) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.gt(root.get<Number>(field), value)
    override fun toString() = "$field > $value"
}
data class Gt2Filter<V: Comparable<V>>(val field: String, val value: V) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.greaterThan(root.get<V>(field), value)
    override fun toString() = "$field > $value"
}
data class IsNullFilter(val field: String) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.isNull(root.get<Any>(field))
    override fun toString() = "$field is null"
}
data class IsNotNullFilter(val field: String) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.isNotNull(root.get<Any>(field))
    override fun toString() = "$field is not null"
}
data class IsTrueFilter(val field: String) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.isTrue(root.get<Boolean>(field))
    override fun toString() = field
}
data class LikeFilter(val field: String, val value: String) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.like(root.get<String>(field), value)
    override fun toString() = "$field like $value"
}

/**
 * Running block with this class as its receiver will allow you to write expressions like this:
 * `Person::age lt 25`.
 *
 * Containing these functions in this class will prevent polluting of the KProperty1 interface and also makes it type-safe?
 * @todo mavi what about joins?
 */
class JPAWhereBuilder<T> {
    infix fun <R: Serializable?> KProperty1<T, R>.eq(value: R): JPAFilter = EqFilter(name, value)
    infix fun <R: Number> KProperty1<T, R?>.le(value: R): JPAFilter = LeFilter(name, value)
    infix fun <R: Comparable<R>> KProperty1<T, R?>.le(value: R): JPAFilter = Le2Filter(name, value)
    infix fun <R: Number> KProperty1<T, R?>.lt(value: R): JPAFilter = LtFilter(name, value)
    infix fun <R: Comparable<R>> KProperty1<T, R?>.lt(value: R): JPAFilter = Lt2Filter(name, value)
    infix fun <R: Number> KProperty1<T, R?>.ge(value: R): JPAFilter = GeFilter(name, value)
    infix fun <R: Comparable<R>> KProperty1<T, R?>.ge(value: R): JPAFilter = Ge2Filter(name, value)
    infix fun <R: Number> KProperty1<T, R?>.gt(value: R): JPAFilter = GtFilter(name, value)
    infix fun <R: Comparable<R>> KProperty1<T, R?>.gt(value: R): JPAFilter = Gt2Filter(name, value)
    infix fun KProperty1<T, String?>.like(value: String): JPAFilter = LikeFilter(name, value)
    /**
     * Matches only values contained in given range.
     * @param range the range
     */
    infix fun <R> KProperty1<T, R?>.between(range: ClosedRange<R>): JPAFilter where R: Number, R: Comparable<R> =
            GeFilter(name, range.start) and LeFilter(name, range.endInclusive)
    val KProperty1<T, *>.isNull: JPAFilter get() = IsNullFilter(name)
    val KProperty1<T, *>.isNotNull: JPAFilter get() = IsNotNullFilter(name)
    val KProperty1<T, Boolean?>.isTrue: JPAFilter get() = IsTrueFilter(name)
    val KProperty1<T, Boolean?>.isFalse: JPAFilter get() = !IsTrueFilter(name)
}
