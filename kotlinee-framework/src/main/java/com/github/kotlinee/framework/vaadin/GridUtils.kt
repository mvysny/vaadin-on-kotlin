package com.github.kotlinee.framework.vaadin

import com.vaadin.data.Container
import com.vaadin.data.Item
import com.vaadin.data.util.GeneratedPropertyContainer
import com.vaadin.data.util.PropertyValueGenerator
import com.vaadin.ui.Grid
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import java.lang.reflect.Field
import java.util.*
import kotlin.reflect.KProperty

private val gridColumnGrid: Field = Grid.Column::class.java.getDeclaredField("grid").apply { isAccessible = true }

/**
 * Owner grid.
 */
val Grid.Column.grid: Grid
    get() = gridColumnGrid.get(this) as Grid

private val propertyGenerators: Field = GeneratedPropertyContainer::class.java.getDeclaredField("propertyGenerators").apply { isAccessible = true }

fun GeneratedPropertyContainer.isGenerated(propertyId: Any?): Boolean = (propertyGenerators.get(this) as Map<Any, *>).containsKey(propertyId)

fun Container.isGenerated(propertyId: Any?): Boolean = if (this is GeneratedPropertyContainer) isGenerated(propertyId) else false

/**
 * True if this column is a generated column (part of the [GeneratedPropertyContainer]).
 */
val Grid.Column.isGenerated: Boolean
    get() = grid.containerDataSource.isGenerated(propertyId)

/**
 * Starts adding of the columns into the grid.
 */
fun Grid.cols(block: GridColumnBuilder.()->Unit): Unit {
    val adder = GridColumnBuilder(this)
    adder.block()
    setColumns(*adder.columnProperties.toTypedArray())
}

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
        val column = grid.getColumn(propertyId) ?: throw IllegalArgumentException("No such property $propertyId, available properties: ${grid.columns}")
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
