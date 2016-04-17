package com.example.pokusy.kotlinee

import com.vaadin.addon.jpacontainer.JPAContainer
import com.vaadin.addon.jpacontainer.provider.CachingBatchableLocalEntityProvider
import com.vaadin.data.Container
import com.vaadin.data.Item
import com.vaadin.data.util.GeneratedPropertyContainer
import com.vaadin.data.util.PropertyValueGenerator
import com.vaadin.data.util.converter.Converter
import com.vaadin.event.LayoutEvents
import com.vaadin.event.MouseEvents
import com.vaadin.event.ShortcutAction
import com.vaadin.event.ShortcutListener
import com.vaadin.server.AbstractClientConnector
import com.vaadin.server.Sizeable
import com.vaadin.shared.MouseEventDetails
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import java.io.Serializable
import java.util.*

/**
 * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only,
 * simply call [JPAContainer.addContainerFilter] on the container produced.
 * @param entity the entity type
 * @return the new container which can be assigned to a [Grid]
 */
fun <T> createContainer(entity: Class<T>): JPAContainer<T> {
    val provider = CachingBatchableLocalEntityProvider(entity, extendedEntityManager)
    val container = JPAContainer(entity)
    container.entityProvider = provider
    return container
}

/**
 * When introducing extensions for your custom components, just call this in your method. For example:
 *
 * `fun HasComponents.shinyComponent(caption: String? = null, block: ShinyComponent.()->Unit = {}) = init(ShinyComponent(caption), block)`
 *
 * Input [component] is automatically added to the children of this [ComponentContainer], or replaces content in [SingleComponentContainer].
 * @param component the component to attach
 * @param block optional block to run over the component, allowing you to add children to the [component]
 */
fun <T : Component> HasComponents.init(component: T, block: T.()->Unit = {}): T {
    when (this) {
        is ComponentContainer -> addComponent(component)
        is SingleComponentContainer -> content = component
        else -> throw RuntimeException("Unsupported component container $this")
    }
    component.block()
    return component
}

fun HasComponents.verticalLayout(block: VerticalLayout.()->Unit = {}) = init(VerticalLayout(), block)

fun HasComponents.horizontalLayout(block: HorizontalLayout.()->Unit = {}) = init(HorizontalLayout(), block)

fun HasComponents.formLayout(block: FormLayout.()->Unit = {}) = init(FormLayout(), block)

fun HasComponents.button(caption: String? = null, block: Button.() -> Unit = {}) = init(Button(caption), block)

fun HasComponents.grid(caption: String? = null, dataSource: Container.Indexed? = null, block: Grid.() -> Unit = {}) = init(Grid(caption, dataSource), block)

/**
 * Creates a [TextField] and attaches it to this component. [TextField.nullRepresentation] is set to an empty string.
 * @param caption optional caption
 * @param value the optional value
 */
fun HasComponents.textField(caption: String? = null, value: String? = null, block: TextField.()->Unit = {}): TextField {
    val textField = TextField(caption, value)
    init(textField, block)
    textField.nullRepresentation = ""
    return textField
}

/**
 * Creates a [Label]
 * @param content the label content
 * @param block use to set additional label parameters
 */
fun HasComponents.label(content: String? = null, block: Label.()->Unit = {}) = init(Label(content), block)

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
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.addChildClickListener(listener: LayoutEvents.LayoutClickListener) {
    (this as Component).addStyleName("clickable")
    addLayoutClickListener(LayoutEvents.LayoutClickListener { event ->
        if (event.button != MouseEventDetails.MouseButton.LEFT) {
            // only handle left mouse clicks
        } else if (event.clickedComponent != null && event.clickedComponent.hasClickListeners()) {
            // the component has its own click listeners, do nothing
        } else
        // what if some child layout listens for the layout click event as well?
        if (event.clickedComponent != null && event.clickedComponent.getAscendantLayoutWithLayoutClickNotifier() !== this@addChildClickListener) {
            // do nothing
        } else {
            listener.layoutClick(event)
        }
    })
}

/**
 * Replaces any click listeners with this one.
 * @param listener the listener to set. Only called on left-click.
 */
fun Button.setLeftClickListener(listener: Button.ClickListener): Unit {
    getListeners(Button.ClickEvent::class.java).toList().forEach { removeClickListener(it as Button.ClickListener) }
    // Button only fires left-click events.
    addClickListener(listener)
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
