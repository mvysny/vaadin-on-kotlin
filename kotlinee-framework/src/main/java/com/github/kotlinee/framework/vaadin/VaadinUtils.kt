package com.github.kotlinee.framework.vaadin

import com.github.kotlinee.framework.TreeIterator
import com.github.kotlinee.framework.extendedEntityManager
import com.github.kotlinee.framework.vaadin.ShortcutListeners
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
import com.vaadin.event.ShortcutAction.KeyCode.ENTER
import com.vaadin.event.ShortcutListener
import com.vaadin.server.*
import com.vaadin.shared.MouseEventDetails
import com.vaadin.shared.ui.label.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.renderers.ButtonRenderer
import com.vaadin.ui.renderers.ClickableRenderer
import com.vaadin.ui.themes.ValoTheme
import java.io.Serializable
import java.lang.reflect.Field
import java.util.*
import javax.servlet.http.Cookie

/**
 * Creates a container which lists all instances of given entity. To restrict the list to a particular entity only,
 * simply call [JPAContainer.addContainerFilter] on the container produced.
 *
 * Containers produced by this method have the following properties:
 * * The container's [Item] IDs are not the entity instances themselves - instead, [Item] ID contains the value of the JPA entity ID. This is important when using the container
 * together with [AbstractSelect] as the select's value is taken amongst the Item ID.
 * * [Item]'s Property IDs are [String] values - the field names of given JPA bean.
 *
 * @param entity the entity type
 * @return the new container which can be assigned to a [Grid]
 */
inline fun <reified T : Any> jpaContainer(): JPAContainer<T> = jpaContainer(T::class.java)

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
fun Component.setWidthFull() {
    w = 100.perc
}

/**
 * Sets the component height to 100%
 */
fun Component.setHeightFull() {
    h = 100.perc
}

/**
 * Triggers given listener when the text field is focused and user presses the Enter key.
 * @param enterListener the listener to invoke when the user presses the Enter key.
 */
fun AbstractTextField.onEnterPressed(enterListener: (AbstractTextField) -> Unit) {
    val enterShortCut = object : ShortcutListener("EnterOnTextAreaShorcut", null, ENTER) {
        override fun handleAction(sender: Any, target: Any) {
            enterListener(this@onEnterPressed)
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
 * Sets a click listener to a layout. The click listener will be called when the layout and any descendant component is clicked,
 * except for descendants which have their own click listeners attached.
 *
 * Removes any previously attached layout click listeners
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.onChildClick(listener: (LayoutEvents.LayoutClickEvent)->Unit) {
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
fun LayoutEvents.LayoutClickNotifier.addChildClickListener(listener: (LayoutEvents.LayoutClickEvent) -> Unit) {
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
fun Button.onLeftClick(listener: (Button.ClickEvent)->Unit) {
    getListeners(Button.ClickEvent::class.java).toList().forEach { removeClickListener(it as Button.ClickListener) }
    // Button only fires left-click events.
    addClickListener(listener)
}

/**
 * Configures this button as primary. Beware - all primary buttons attached to the current UI or Window will be pressed on Enter key press.
 */
fun Button.setPrimary() {
    styleName += " ${ValoTheme.BUTTON_PRIMARY}"
    setClickShortcut(ENTER)
}

/**
 * Replaces any click listeners with this one.
 * @param listener the listener to set. Only called on left-click.
 */
fun Image.onLeftClick(listener: (MouseEvents.ClickEvent)->Unit): Unit {
    getListeners(MouseEvents.ClickEvent::class.java).toList().forEach { removeClickListener(it as MouseEvents.ClickListener) }
    addClickListener {
        if (it.button == MouseEventDetails.MouseButton.LEFT) listener(it)
    }
}

/**
 * Represents a Vaadin component width or height.
 * @param size the size
 * @param units states the size units.
 */
data class Size(val size: Float, val units: Sizeable.Unit) : Serializable {
    /**
     * true if this size is set to 100% and the component fills its parent in this dimension.
     */
    val isFillParent = size >= 100 && units == Sizeable.Unit.PERCENTAGE
    val isFull = isFillParent
    /**
     * true if this component wraps its content in this dimension (size is -1px).
     */
    val isWrapContent = size < 0
    val isUndefined = isWrapContent

    override fun toString() = "$size${units.symbol}"
}

val Int.px: Size
    get() = toFloat().px
val Float.px: Size
    get() = Size(this, Sizeable.Unit.PIXELS)
val Int.perc: Size
    get() = toFloat().perc
val Float.perc: Size
    get() = Size(this, Sizeable.Unit.PERCENTAGE)
var Component.w: Size
    get() = Size(width, widthUnits)
    set(value) {
        setWidth(value.size, value.units)
    }
var Component.h: Size
    get() = Size(height, heightUnits)
    set(value) = setHeight(value.size, value.units)

/**
 * true if both the component width and height is set to 100%
 */
val Component.isSizeFull: Boolean
    get() = w.isFillParent && h.isFillParent

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
    get() = absolutePosition.zIndex
    set(value) {
        absolutePosition.zIndex = value
    }

/**
 * Returns the [AbsoluteLayout.ComponentPosition] of this component. Fails if this component is not nested inside [AbsoluteLayout]
 */
val Component.absolutePosition: AbsoluteLayout.ComponentPosition
    get() = (parent as AbsoluteLayout).getPosition(this)
var AbsoluteLayout.ComponentPosition.top: Size
    get() = Size(topValue, topUnits)
    set(value) {
        topValue = value.size; topUnits = value.units
    }
var AbsoluteLayout.ComponentPosition.bottom: Size
    get() = Size(bottomValue, bottomUnits)
    set(value) {
        bottomValue = value.size; bottomUnits = value.units
    }
var AbsoluteLayout.ComponentPosition.left: Size
    get() = Size(leftValue, leftUnits)
    set(value) {
        leftValue = value.size; leftUnits = value.units
    }
var AbsoluteLayout.ComponentPosition.right: Size
    get() = Size(rightValue, rightUnits)
    set(value) {
        rightValue = value.size; rightUnits = value.units
    }

/**
 * An utility method which adds an item and sets item's caption.
 * @param the Identification of the item to be created.
 * @param caption the new caption
 * @return the newly created item ID.
 */
fun AbstractSelect.addItem(itemId: Any?, caption: String) = addItem(itemId).apply { setItemCaption(itemId, caption) }!!



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
 * Directs the browser to go back.
 */
fun goBack() = Page.getCurrent().javaScript.execute("window.history.back();")

/**
 * Allows you to create [BeanFieldGroup] like this: `BeanFieldGroup<Person>()` instead of `BeanFieldGroup<Person>(Person::class.java)`
 */
inline fun <reified T : Any> BeanFieldGroup(): BeanFieldGroup<T> = BeanFieldGroup(T::class.java)

data class SimpleContent(val small: String, val large: Component) : PopupView.Content {
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

enum class ModifierKey(val value: Int) {
    Shift(ShortcutAction.ModifierKey.SHIFT),
    Ctrl(ShortcutAction.ModifierKey.CTRL),
    Alt(ShortcutAction.ModifierKey.ALT),
    Meta(ShortcutAction.ModifierKey.META);

    infix operator fun plus(other: ModifierKey) = setOf(this, other)
    infix operator fun plus(key: Int) = KeyShortcut(key, setOf(this))
}

infix operator fun Set<ModifierKey>.plus(key: Int) = KeyShortcut(key, this)

/**
 * Denotes a keyboard shortcut, such as [ModifierKey.Ctrl]+[ModifierKey.Alt]+[ShortcutAction.KeyCode.C]`. When properly imported, this
 * becomes `Ctrl+Alt+C` ;)
 * @property keyCode one of the ShortcutAction.KeyCode.* constants.
 */
data class KeyShortcut(val keyCode: Int, val modifierKeys: Set<ModifierKey> = setOf()) {
    val vaadinModifiers: IntArray = modifierKeys.map { it.value }.toIntArray()
}

fun shortcutListener(shortcut: KeyShortcut, block: () -> Unit): ShortcutListener =
        ShortcutListeners.listener(shortcut.keyCode, shortcut.vaadinModifiers, block)

fun shortcutListener(shortcut: Int, block: () -> Unit) = shortcutListener(KeyShortcut(shortcut), block)

/**
 * Adds global shortcut listener. The listener is not added directly for this component - instead it is global, up to the nearest parent
 * Panel, UI or Window.
 * @param shortcut the shortcut, e.g. `Ctrl + Alt + C`
 */
fun Component.addGlobalShortcutListener(shortcut: KeyShortcut, action: () -> Unit): ShortcutListener {
    val listener = shortcutListener(shortcut.keyCode, action)
    (this as AbstractComponent).addShortcutListener(listener)
    return listener
}

/**
 * Adds global shortcut listener. The listener is not added directly for this component - instead it is global, up to the nearest parent
 * Panel, UI or Window.
 * @param keyCode the key code, e.g. [ShortcutAction.KeyCode.C]
 */
fun Component.addGlobalShortcutListener(keyCode: Int, action: () -> Unit) = addGlobalShortcutListener(KeyShortcut(keyCode), action)

/**
 * Makes it possible to invoke a click on this button by pressing the given
 * {@link KeyCode} and (optional) {@link ModifierKey}s.
 * The shortcut is global (bound to the containing Window).
 * @param shortcut the shortcut, e.g. `Ctrl + Alt + C`
 */
var Button.clickShortcut: KeyShortcut
    get() = throw RuntimeException("Property is write-only")
    set(shortcut) = setClickShortcut(shortcut.keyCode, *shortcut.vaadinModifiers)

/**
 * Sets all four margins to given value.
 */
var AbstractOrderedLayout.isMargin: Boolean
get() = margin.hasAll()
set(value) { setMargin(value) }
