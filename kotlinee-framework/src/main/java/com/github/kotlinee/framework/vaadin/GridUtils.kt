package com.github.kotlinee.framework.vaadin

import com.vaadin.data.Container
import com.vaadin.data.Item
import com.vaadin.data.util.GeneratedPropertyContainer
import com.vaadin.data.util.PropertyValueGenerator
import com.vaadin.ui.Grid
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import java.lang.reflect.Field

/**
 * Adds a button column to a grid.
 * @param propertyId the generated column propertyId, for example "edit"
 * @param caption human-readable button caption, e.g. "Edit"
 * @param listener invoked when the button is clicked. The [RendererClickEvent.itemId] is the ID of the JPA bean.
 * @return the grid column which you can tweak further.
 */
fun Grid.addButtonColumn(propertyId: String, caption: String, listener: ClickableRenderer.RendererClickListener): Grid.Column {
    if (containerDataSource !is GeneratedPropertyContainer) containerDataSource = GeneratedPropertyContainer(containerDataSource)
    (containerDataSource as GeneratedPropertyContainer).addGeneratedProperty(propertyId, object : PropertyValueGenerator<String>() {
        override fun getValue(item: Item?, itemId: Any?, propertyId: Any?): String? = caption
        override fun getType(): Class<String>? = String::class.java
    })
    return getColumn(propertyId).apply {
        renderer = ButtonRenderer(listener)
        headerCaption = ""
    }
}

private val gridColumnGrid: Field = Grid.Column::class.java.getDeclaredField("grid").apply { isAccessible = true }

val Grid.Column.grid: Grid
    get() = gridColumnGrid.get(this) as Grid

private val propertyGenerators: Field = GeneratedPropertyContainer::class.java.getDeclaredField("propertyGenerators").apply { isAccessible = true }

fun GeneratedPropertyContainer.isGenerated(propertyId: Any?): Boolean = (propertyGenerators.get(this) as Map<Any, *>).containsKey(propertyId)

fun Container.isGenerated(propertyId: Any?): Boolean = if (this is GeneratedPropertyContainer) isGenerated(propertyId) else false

val Grid.Column.isGenerated: Boolean
    get() = grid.containerDataSource.isGenerated(getPropertyId())
