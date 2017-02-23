package com.github.vok.framework.vaadin

import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.framework.db
import com.github.vok.framework.dbId
import com.vaadin.data.provider.AbstractBackEndDataProvider
import com.vaadin.data.provider.Query
import com.vaadin.data.provider.QuerySortOrder
import com.vaadin.shared.data.sort.SortDirection
//import com.vaadin.v7.data.Container
//import com.vaadin.v7.data.Item
//import com.vaadin.v7.data.util.GeneratedPropertyContainer
//import com.vaadin.v7.data.util.PropertyValueGenerator
//import com.vaadin.v7.ui.Grid
//import com.vaadin.v7.ui.renderers.ButtonRenderer
//import com.vaadin.v7.ui.renderers.ClickableRenderer
import kotlinx.support.jdk8.collections.stream
import java.lang.reflect.Field
import java.util.*
import java.util.stream.Stream
import java.util.stream.StreamSupport
import javax.persistence.EntityManager
import javax.persistence.TypedQuery
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

//private val gridColumnGrid: Field = Grid.Column::class.java.getDeclaredField("grid").apply { isAccessible = true }
//
///**
// * Owner grid.
// */
//val Grid.Column.grid: Grid
//    get() = gridColumnGrid.get(this) as Grid
//
//private val propertyGenerators: Field = GeneratedPropertyContainer::class.java.getDeclaredField("propertyGenerators").apply { isAccessible = true }
//
//fun GeneratedPropertyContainer.isGenerated(propertyId: Any?): Boolean = (propertyGenerators.get(this) as Map<Any, *>).containsKey(propertyId)
//
//fun Container.isGenerated(propertyId: Any?): Boolean = if (this is GeneratedPropertyContainer) isGenerated(propertyId) else false
//
///**
// * True if this column is a generated column (part of the [GeneratedPropertyContainer]).
// */
//val Grid.Column.isGenerated: Boolean
//    get() = grid.containerDataSource.isGenerated(propertyId)
//
///**
// * Starts adding of the columns into the grid.
// */
//fun Grid.cols(block: GridColumnBuilder.()->Unit): Unit {
//    val adder = GridColumnBuilder(this)
//    adder.block()
//    setColumns(*adder.columnProperties.toTypedArray())
//}
//
//class GridColumnBuilder(val grid: Grid) {
//    internal val columnProperties = LinkedList<Any?>()
//
//    /**
//     * Adds a column which shows JPA Bean's property values. Applicable only when the Grid is backed by [JPAContainer].
//     * @param property the JPA bean property (field)
//     */
//    fun column(property: KProperty<*>, block: Grid.Column.()->Unit = {}) {
//        column(property.name, block)
//    }
//
//    /**
//     * Adds an arbitrary column to the Grid. The Container must provide property values for given propertyId.
//     * @param propertyId the container property ID
//     */
//    fun column(propertyId: Any?, block: Grid.Column.()->Unit = {}) {
//        val column = grid.getColumn(propertyId) ?: throw IllegalArgumentException("No such column $propertyId, available columns: ${grid.columns}. You need to set the data source first.")
//        columnProperties += propertyId
//        column.block()
//    }
//
//    /**
//     * Adds a generated column. Grid's data source is automatically converted to [GeneratedPropertyContainer]
//     * @param propertyId the generated column propertyId, for example "edit"
//     * @param generator generates values for the cells
//     */
//    fun generated(propertyId: Any?, generator: PropertyValueGenerator<*>, block: Grid.Column.()->Unit = {}) {
//        if (grid.containerDataSource !is GeneratedPropertyContainer) grid.containerDataSource = GeneratedPropertyContainer(grid.containerDataSource)
//        (grid.containerDataSource as GeneratedPropertyContainer).addGeneratedProperty(propertyId, generator)
//        column(propertyId, block)
//    }
//
//    /**
//     * Adds a column which only contains a single button.
//     * @param propertyId the generated column propertyId, for example "edit"
//     * @param caption all buttons will have this caption
//     * @param onClick invoked when the button is clicked. The [RendererClickEvent.itemId] is the ID of the JPA bean.
//     */
//    fun button(propertyId: Any?, caption: String, onClick: (ClickableRenderer.RendererClickEvent)->Unit, block: Grid.Column.()->Unit = {}) {
//        generated(propertyId, object : PropertyValueGenerator<String>() {
//            override fun getValue(item: Item?, itemId: Any?, propertyId: Any?): String? = caption
//            override fun getType(): Class<String>? = String::class.java
//        }) {
//            renderer = ButtonRenderer(onClick)
//            headerCaption = ""
//            block()
//        }
//    }
//}



/**
 * Provides instances of given JPA class from the database. Currently only supports sorting, currently does not support joins.
 * @todo mavi add support for filters
 */
class JPADataSource<T: Any>(val entity: KClass<T>) : AbstractBackEndDataProvider<T, Unit>() {

    private fun Query<T, Unit>.toQlString(): String = buildString {
        append("from ${entity.java.simpleName} a")
        if (!sortOrders.isEmpty()) {
            append(" order by ")
            sortOrders.joinTo(this, transform = { "a.${it.toQlOrderByClause()}" })
        }
    }

    private fun QuerySortOrder.toQlOrderByClause() = sorted + if (direction != SortDirection.ASCENDING) " desc" else ""

    override fun fetchFromBackEnd(query: Query<T, Unit>): Stream<T> = db {
        em.createQuery(query.toQlString(), entity.java).apply {
            firstResult = query.offset
            if (query.limit < Integer.MAX_VALUE) maxResults = query.limit
        }.resultList.stream()
    }

    override fun sizeInBackEnd(query: Query<T, Unit>): Int = db {
        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        em.createQuery("select count(a) ${query.toQlString()}", java.lang.Long::class.java).singleResult.toInt()
    }

    override fun getId(item: T): Any = item.dbId!!
}
