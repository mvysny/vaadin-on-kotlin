package com.github.kotlinee.framework

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider
import com.vaadin.data.Container
import com.vaadin.data.Item
import com.vaadin.data.fieldgroup.BeanFieldGroup
import com.vaadin.data.util.GeneratedPropertyContainer
import com.vaadin.data.util.PropertyValueGenerator
import com.vaadin.data.util.converter.Converter
import com.vaadin.event.LayoutEvents
import com.vaadin.event.MouseEvents
import com.vaadin.event.ShortcutAction
import com.vaadin.event.ShortcutListener
import com.vaadin.server.*
import com.vaadin.shared.MouseEventDetails
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import java.io.Serializable
import java.lang.reflect.Field
import java.util.*
import javax.servlet.http.Cookie

/**
 * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only,
 * simply call [JPAContainer.addContainerFilter] on the container produced.
 * @param entity the entity type
 * @return the new container which can be assigned to a [Grid]
 */
inline fun <reified T: Any> jpaContainer(): JPAContainer<T> = jpaContainer(T::class.java)

fun <T> jpaContainer(entity: Class<T>): JPAContainer<T> {
    val provider = CachingBatchableLocalEntityProvider(entity, extendedEntityManager)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}

/**
 * Shows given html in this label.
 * @param html the html code to show.
 */
fun Label.html(html: String?) {
    contentMode = ContentMode.HTML
    value = html
}

/**
 * Sets the expand ratio of this component with respect to its parent layout. See [AbstractOrderedLayout.setExpandRatio] for more details.
 *
 * Fails if this component is not nested inside [AbstractOrderedLayout].
 */
var Component.expandRatio: Float
get() = (parent as AbstractOrderedLayout).getExpandRatio(this)
set(value) = (parent as AbstractOrderedLayout).setExpandRatio(this, value)

/**
 * Sets the component width to 100%
 */
fun Component.setWidthFull() = setWidth(100f, Sizeable.Unit.PERCENTAGE)

/**
 * Sets the component height to 100%
 */
fun Component.setHeightFull() = setHeight(100f, Sizeable.Unit.PERCENTAGE)

interface EnterPressedListener : Serializable {
    /**
     * Invoked when the text field is focused and the user presses Enter.
     * @param source the [TextField] which triggered the event.
     */
    fun onEnterPressed(source: AbstractTextField): Unit
}

/**
 * Triggers given listener when the text field is focused and user presses the Enter key.
 * @param enterListener the listener to invoke.
 */
fun AbstractTextField.onEnterPressed(enterListener: EnterPressedListener) {
    val enterShortCut = object : ShortcutListener("EnterOnTextAreaShorcut", null, ShortcutAction.KeyCode.ENTER) {
        override fun handleAction(sender: Any, target: Any) {
            enterListener.onEnterPressed(this@onEnterPressed)
        }
    }
    addFocusListener { addShortcutListener(enterShortCut) }
    addBlurListener { removeShortcutListener(enterShortCut) }
}

/**
 * Trims the user input string before storing it into the underlying property data source. Vital for mobile-oriented apps:
 * Android keyboard often adds whitespace to the end of the text when auto-completion occurs. Imagine storing a username ending with a space upon registration:
 * such person can no longer log in from his PC unless he explicitely types in the space.
 */
fun AbstractField<String>.trimmingConverter() {
    setConverter(object : Converter<String?, String?> {
        override fun convertToModel(value: String?, targetType: Class<out String?>?, locale: Locale?): String? = value?.trim()

        override fun convertToPresentation(value: String?, targetType: Class<out String?>?, locale: Locale?): String? = value

        override fun getPresentationType(): Class<String?>? = String::class.java as Class<String?>

        override fun getModelType(): Class<String?>? = String::class.java as Class<String?>
    })
}

private fun Component.getListenersHandling(eventType: Class<*>): List<*> =
    if (this is AbstractClientConnector) this.getListeners(eventType).toList() else listOf<Any>()

private fun Component.hasListenersHandling(eventType: Class<*>) = !getListenersHandling(eventType).isEmpty()

private fun Component.hasClickListeners() = hasListenersHandling(MouseEvents.ClickEvent::class.java) || hasListenersHandling(Button.ClickEvent::class.java)

private fun Component.getAscendantLayoutWithLayoutClickNotifier(): LayoutEvents.LayoutClickNotifier? {
    var component = this
    while (component != null) {
        if (component is LayoutEvents.LayoutClickNotifier && component.hasListenersHandling(LayoutEvents.LayoutClickEvent::class.java)) {
            return component
        }
        component = component.parent
    }
    return null
}

/**
 * Adds a click listener to a layout. The click listener will be called when the layout and any descendant component is clicked,
 * except for descendants which have their own click listeners attached.
 *
 * Removes any previously attached layout click listeners
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.setChildClickListener(listener: (LayoutEvents.LayoutClickEvent)->Unit) {
    (this as AbstractClientConnector).getListeners(LayoutEvents.LayoutClickEvent::class.java).toList().forEach {
        removeLayoutClickListener(it as LayoutEvents.LayoutClickListener)
    }
    addChildClickListener(listener)
}

/**
 * Adds a click listener to a layout. The click listener will be called when the layout and any descendant component is clicked,
 * except for descendants which have their own click listeners attached.
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.addChildClickListener(listener: (LayoutEvents.LayoutClickEvent)->Unit) {
    (this as Component).addStyleName("clickable")
    addLayoutClickListener({ event ->
        if (event.button != MouseEventDetails.MouseButton.LEFT) {
            // only handle left mouse clicks
        } else if (event.clickedComponent != null && event.clickedComponent.hasClickListeners()) {
            // the component has its own click listeners, do nothing
        } else
        // what if some child layout listens for the layout click event as well?
        if (event.clickedComponent != null && event.clickedComponent.getAscendantLayoutWithLayoutClickNotifier() !== this@addChildClickListener) {
            // do nothing
        } else {
            listener(event)
        }
    })
}

/**
 * Replaces any click listeners with this one.
 * @param listener the listener to set. Only called on left-click.
 */
fun Button.setLeftClickListener(listener: (Button.ClickEvent)->Unit): Unit {
    getListeners(Button.ClickEvent::class.java).toList().forEach { removeClickListener(it as Button.ClickListener) }
    // Button only fires left-click events.
    addClickListener(listener)
}

/**
 * Replaces any click listeners with this one.
 * @param listener the listener to set. Only called on left-click.
 */
fun Image.setLeftClickListener(listener: (MouseEvents.ClickEvent)->Unit): Unit {
    getListeners(MouseEvents.ClickEvent::class.java).toList().forEach { removeClickListener(it as MouseEvents.ClickListener) }
    addClickListener {
        if (it.button == MouseEventDetails.MouseButton.LEFT) listener(it)
    }
}

/**
 * true if this component width is set to 100%
 */
val Component.widthIsFillParent: Boolean
    get() = width >= 100 && widthUnits == Sizeable.Unit.PERCENTAGE

/**
 * true if this component height is set to 100%
 */
val Component.heightIsFillParent: Boolean
    get() = width >= 100 && widthUnits == Sizeable.Unit.PERCENTAGE

/**
 * true if both the component width and height is set to 100%
 */
val Component.isSizeFull: Boolean
    get() = widthIsFillParent && heightIsFillParent

/**
 * Sets or gets alignment for this component with respect to its parent layout. Use
 * predefined alignments from Alignment class. Fails if the component is not nested inside [AbstractOrderedLayout]
 */
var Component.alignment: Alignment
    get() = (parent as AbstractOrderedLayout).getComponentAlignment(this)
    set(value) = (parent as AbstractOrderedLayout).setComponentAlignment(this, value)

/**
 * Sets [AbsoluteLayout.ComponentPosition.zIndex]. Fails if this component is not nested inside [AbsoluteLayout]
 */
var Component.zIndex: Int
    get() = (parent as AbsoluteLayout).getPosition(this).zIndex
    set(value) { (parent as AbsoluteLayout).getPosition(this).zIndex = value }

/**
 * An utility method which adds an item and sets item's caption.
 * @param the Identification of the item to be created.
 * @param caption the new caption
 */
fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!

/**
 * Adds a button column to a grid.
 * @param propertyId the generated column propertyId, for example "edit"
 * @param caption human-readable button caption, e.g. "Edit"
 * @param listener invoked when the button is clicked. The [RendererClickEvent.itemId] is the ID of the JPA bean.
 * @return the grid column which you can tweak further.
 */
fun Grid.addButtonColumn(propertyId: String, caption: String, listener: ClickableRenderer.RendererClickListener): Grid.Column {
    if (containerDataSource !is GeneratedPropertyContainer) containerDataSource = GeneratedPropertyContainer(containerDataSource)
    (containerDataSource as GeneratedPropertyContainer).addGeneratedProperty(propertyId, object: PropertyValueGenerator<String>() {
        override fun getValue(item: Item?, itemId: Any?, propertyId: Any?): String? = caption
        override fun getType(): Class<String>? = String::class.java
    })
    return getColumn(propertyId).apply {
        renderer = ButtonRenderer(listener)
        headerCaption = ""
    }
}

private val gridColumnGrid: java.lang.reflect.Field = Grid.Column::class.java.getDeclaredField("grid").apply { isAccessible = true }

val Grid.Column.grid: Grid
    get() = gridColumnGrid.get(this) as Grid

private val propertyGenerators: Field = GeneratedPropertyContainer::class.java.getDeclaredField("propertyGenerators").apply { isAccessible = true }

fun GeneratedPropertyContainer.isGenerated(propertyId: Any?) = (propertyGenerators.get(this) as Map<Any, *>).containsKey(propertyId)

fun Container.isGenerated(propertyId: Any?) = if (this is GeneratedPropertyContainer) isGenerated(propertyId) else false

val Grid.Column.isGenerated: Boolean
    get() = grid.containerDataSource.isGenerated(getPropertyId())

/**
 * Walks over this component and all descendants of this component, breadth-first.
 * @return iterable which iteratively walks over this component and all of its descendants.
 */
fun HasComponents.walk(): Iterable<Component> = Iterable {
    TreeIterator<Component>(this, { component ->
        if (component is HasComponents) component else listOf()
    })
}

/**
 * Finds a cookie by name.
 * @param name cookie name
 * @return cookie or null if there is no such cookie.
 */
fun getCookie(name: String): Cookie? = VaadinService.getCurrentRequest().cookies?.firstOrNull { it.name == name }

/**
 * Directs the browser to go back.
 */
fun goBack() = Page.getCurrent().javaScript.execute("window.history.back();")

/**
 * Allows you to create [BeanFieldGroup] like this: `BeanFieldGroup<Person>()` instead of `BeanFieldGroup<Person>(Person::class.java)`
 */
inline fun <reified T: Any> BeanFieldGroup(): BeanFieldGroup<T> = com.vaadin.data.fieldgroup.BeanFieldGroup(T::class.java)

data class SimpleContent(val small: String, val large: Component): PopupView.Content {
    companion object {
        val EMPTY = SimpleContent("", Label(""))
    }
    constructor(content: PopupView.Content) : this(content.minimizedValueAsHTML, content.popupComponent)

    override fun getPopupComponent(): Component? = large

    override fun getMinimizedValueAsHTML() = small
}

private var PopupView.simpleContent: SimpleContent
get() {
    val content = content
    return if (content is SimpleContent) content else SimpleContent(content)
}
set(value) {
    content = value
}

/**
 * Allows you to set the popup component directly, without changing [minimizedValueAsHTML]
 */
var PopupView.popupComponent: Component
get() = content.popupComponent
set(value) {
    content = simpleContent.copy(large = value)
}

/**
 * Allows you to set the minimized text directly, without changing [popupComponent]
 */
var PopupView.minimizedValueAsHTML: String
get() = content.minimizedValueAsHTML
set(value) {
    content = simpleContent.copy(small = value)
}
