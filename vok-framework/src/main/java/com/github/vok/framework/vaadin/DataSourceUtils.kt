package com.github.vok.framework.vaadin

import com.github.vok.framework.db
import com.github.vok.framework.dbId
import com.vaadin.data.provider.*
import com.vaadin.shared.data.sort.SortDirection
import kotlinx.support.jdk8.collections.stream
import java.io.Serializable
import java.util.stream.Stream
import javax.persistence.criteria.*
import kotlin.reflect.KClass

/**
 * Provides instances of given JPA class from the database. Currently only supports sorting, currently does not support joins.
 */
class JPADataSource<T: Any>(val entity: KClass<T>) : AbstractBackEndDataProvider<T, JPAFilter?>() {

    private fun QuerySortOrder.toOrder(cb: CriteriaBuilder, root: Root<T>) = if (direction == SortDirection.ASCENDING) cb.asc(root.get<T>(sorted)) else cb.desc(root.get<T>(sorted))

    override fun fetchFromBackEnd(query: Query<T, JPAFilter?>): Stream<T> = db {
        em.criteriaBuilder.let { cb ->
            val q = cb.createQuery(entity.java)
            val root = q.from(entity.java)
            q.orderBy(query.sortOrders.map { it.toOrder(cb, root) })
            if (query.filter.isPresent) q.where(query.filter.get()!!.toPredicate(cb, root))
            em.createQuery(q).resultList
        }.stream()
    }

    override fun sizeInBackEnd(query: Query<T, JPAFilter?>): Int = db {
        em.criteriaBuilder.let { cb ->
            val q = cb.createQuery(Long::class.java)
            val root = q.from(entity.java)
            q.select(cb.count(root))
            if (query.filter.isPresent) q.where(query.filter.get()!!.toPredicate(cb, root))
            em.createQuery(q).singleResult.toInt()
        }
    }

    override fun getId(item: T): Any = item.dbId!!
}

fun <T> DataProvider<T, JPAFilter?>.configurableFilter() : ConfigurableFilterDataProvider<T, JPAFilter?, JPAFilter?> =
        withConfigurableFilter({ f1: JPAFilter?, f2: JPAFilter? -> listOf(f1, f2).filterNotNull().and() })

fun <T> DataProvider<T, JPAFilter?>.and(other: JPAFilter) : DataProvider<T, JPAFilter?> = configurableFilter().apply {
    setFilter(other)
}

fun Collection<JPAFilter>.and(): JPAFilter? = when (size) {
    0 -> null
    1 -> iterator().next()
    else -> AndFilter(toList())
}

interface JPAFilter : Serializable {
    fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate
}

class AndFilter(val filters: List<JPAFilter>) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.and(*filters.map { it.toPredicate(cb, root) }.toTypedArray())
}

class OrFilter(val filters: List<JPAFilter>) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.or(*filters.map { it.toPredicate(cb, root) }.toTypedArray())
}

class EqFilter(val field: String, val value: Serializable) : JPAFilter {
    override fun toPredicate(cb: CriteriaBuilder, root: Root<*>): Predicate = cb.equal(root.get<Any>(field), value)
}
