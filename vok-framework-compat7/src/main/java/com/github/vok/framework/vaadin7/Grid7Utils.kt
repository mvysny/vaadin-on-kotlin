@file:Suppress("DEPRECATION")

package com.github.vok.framework.vaadin7

import com.vaadin.v7.ui.renderers.ButtonRenderer
import com.vaadin.v7.ui.renderers.ClickableRenderer
import com.vaadin.v7.data.Container
import com.vaadin.v7.data.Item
import com.vaadin.v7.data.util.GeneratedPropertyContainer
import com.vaadin.v7.data.util.PropertyValueGenerator
import com.vaadin.v7.ui.Grid
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KProperty

private val gridColumnGrid: Field = Grid.Column::class.java.getDeclaredField("grid").apply { isAccessible = true }

/**
 * Owner grid.
 */
@Deprecated("Use Vaadin8 Grid")
val Grid.Column.grid: Grid
    get() = gridColumnGrid.get(this) as Grid

private val propertyGenerators: Field = GeneratedPropertyContainer::class.java.getDeclaredField("propertyGenerators").apply { isAccessible = true }

@Suppress("UNCHECKED_CAST")
@Deprecated("Use Vaadin8 Grid")
fun GeneratedPropertyContainer.isGenerated(propertyId: Any?): Boolean = (propertyGenerators.get(this) as Map<Any, *>).containsKey(propertyId)

@Deprecated("Use Vaadin8 Grid")
fun Container.isGenerated(propertyId: Any?): Boolean = if (this is GeneratedPropertyContainer) isGenerated(propertyId) else false

/**
 * True if this column is a generated column (part of the [GeneratedPropertyContainer]).
 */
@Deprecated("Use Vaadin8 Grid")
val Grid.Column.isGenerated: Boolean
    get() = grid.containerDataSource.isGenerated(propertyId)

/**
 * Starts adding of the columns into the grid.
 */
@Deprecated("Use Vaadin8 Grid")
fun Grid.cols(block: GridColumnBuilder.()->Unit): Unit {
    val adder = GridColumnBuilder(this)
    adder.block()
    setColumns(*adder.columnProperties.toTypedArray())
}

@Deprecated("Use Vaadin8 Grid")
class GridColumnBuilder(val grid: Grid) {
    internal val columnProperties = LinkedList<Any?>()

    /**
     * Adds a column which shows JPA Bean's property values. Applicable only when the Grid is backed by [JPAContainer].
     * @param property the JPA bean property (field)
     */
    fun column(property: KProperty<*>, block: Grid.Column.()->Unit = {}) {
        column(property.name, block)
    }

    /**
     * Adds an arbitrary column to the Grid. The Container must provide property values for given propertyId.
     * @param propertyId the container property ID
     */
    fun column(propertyId: Any?, block: Grid.Column.()->Unit = {}) {
        val column = grid.getColumn(propertyId) ?: throw IllegalArgumentException("No such column $propertyId, available columns: ${grid.columns}. You need to set the data source first.")
        columnProperties += propertyId
        column.block()
    }

    /**
     * Adds a generated column. Grid's data source is automatically converted to [GeneratedPropertyContainer]
     * @param propertyId the generated column propertyId, for example "edit"
     * @param generator generates values for the cells
     */
    fun generated(propertyId: Any?, generator: PropertyValueGenerator<*>, block: Grid.Column.()->Unit = {}) {
        if (grid.containerDataSource !is GeneratedPropertyContainer) grid.containerDataSource = GeneratedPropertyContainer(grid.containerDataSource)
        (grid.containerDataSource as GeneratedPropertyContainer).addGeneratedProperty(propertyId, generator)
        column(propertyId, block)
    }

    /**
     * Adds a column which only contains a single button.
     * @param propertyId the generated column propertyId, for example "edit"
     * @param caption all buttons will have this caption
     * @param onClick invoked when the button is clicked. The [RendererClickEvent.itemId] is the ID of the JPA bean.
     */
    fun button(propertyId: Any?, caption: String, onClick: (ClickableRenderer.RendererClickEvent)->Unit, block: Grid.Column.()->Unit = {}) {
        generated(propertyId, object : PropertyValueGenerator<String>() {
            override fun getValue(item: Item?, itemId: Any?, propertyId: Any?): String? = caption
            override fun getType(): Class<String>? = String::class.java
        }) {
            renderer = ButtonRenderer(onClick)
            headerCaption = ""
            block()
        }
    }
}
