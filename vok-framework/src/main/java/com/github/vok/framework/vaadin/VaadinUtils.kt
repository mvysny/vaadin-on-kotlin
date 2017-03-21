package com.github.vok.framework.vaadin

import com.github.vok.framework.TreeIterator
import com.github.vok.framework.filterNotBlank
import com.vaadin.data.*
import com.vaadin.data.converter.*
import com.vaadin.event.LayoutEvents
import com.vaadin.event.MouseEvents
import com.vaadin.event.ShortcutAction
import com.vaadin.event.ShortcutAction.KeyCode.ENTER
import com.vaadin.event.ShortcutListener
import com.vaadin.server.AbstractClientConnector
import com.vaadin.server.Page
import com.vaadin.server.Sizeable
import com.vaadin.shared.MouseEventDetails
import com.vaadin.shared.Registration
import com.vaadin.shared.ui.ContentMode
import com.vaadin.ui.*
import com.vaadin.ui.renderers.TextRenderer
import com.vaadin.ui.themes.ValoTheme
import elemental.json.Json
import elemental.json.JsonValue
import org.intellij.lang.annotations.Language
import java.io.Serializable
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.reflect.KProperty1

/**
 * Shows given html in this label.
 * @param html the html code to show.
 */
fun Label.html(@Language("HTML") html: String?) {
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
 * Triggers given listener when the text field is focused and user presses the Enter key.
 * @param enterListener the listener to invoke when the user presses the Enter key.
 */
fun AbstractTextField.onEnterPressed(enterListener: (AbstractTextField) -> Unit) {
    val enterShortCut = object : ShortcutListener("EnterOnTextAreaShorcut", null, ENTER) {
        override fun handleAction(sender: Any, target: Any) {
            enterListener(this@onEnterPressed)
        }
    }
    var r: Registration? = null
    addFocusListener { r?.remove(); r = addShortcutListener(enterShortCut) }
    addBlurListener { r?.remove() }
}

/**
 * Trims the user input string before storing it into the underlying property data source. Vital for mobile-oriented apps:
 * Android keyboard often adds whitespace to the end of the text when auto-completion occurs. Imagine storing a username ending with a space upon registration:
 * such person can no longer log in from his PC unless he explicitely types in the space.
 */
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.trimmingConverter(): Binder.BindingBuilder<BEAN, String?> =
        withConverter(object : Converter<String?, String?> {
    override fun convertToModel(value: String?, context: ValueContext?): Result<String?> =
        Result.ok(value?.trim())
    override fun convertToPresentation(value: String?, context: ValueContext?): String? = value
})
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toInt(): Binder.BindingBuilder<BEAN, Int?> =
        withConverter(StringToIntegerConverter("Can't convert to integer"))
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toDouble(): Binder.BindingBuilder<BEAN, Double?> =
        withConverter(StringToDoubleConverter("Can't convert to decimal number"))
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toLong(): Binder.BindingBuilder<BEAN, Long?> =
        withConverter(StringToLongConverter("Can't convert to integer"))
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toBigDecimal(): Binder.BindingBuilder<BEAN, BigDecimal?> =
        withConverter(StringToBigDecimalConverter("Can't convert to decimal number"))
fun <BEAN> Binder.BindingBuilder<BEAN, String?>.toBigInteger(): Binder.BindingBuilder<BEAN, BigInteger?> =
        withConverter(StringToBigIntegerConverter("Can't convert to integer"))
fun <BEAN> Binder.BindingBuilder<BEAN, LocalDate?>.toDate(): Binder.BindingBuilder<BEAN, Date?> =
        withConverter(LocalDateToDateConverter(ZoneOffset.ofTotalSeconds(Page.getCurrent().webBrowser.timezoneOffset / 1000)))
@JvmName("localDateTimeToDate")
fun <BEAN> Binder.BindingBuilder<BEAN, LocalDateTime?>.toDate(): Binder.BindingBuilder<BEAN, Date?> =
        withConverter(LocalDateTimeToDateConverter(ZoneOffset.ofTotalSeconds(Page.getCurrent().webBrowser.timezoneOffset / 1000)))

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
 * except for descendants which have their own click listeners attached. Note that Vaadin does not fire this event e.g. when clicking
 * on children's captions, so this is not 100% perfect.
 *
 * Removes any previously attached layout click listeners
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.onLayoutClick(listener: (LayoutEvents.LayoutClickEvent)->Unit) {
    (this as AbstractClientConnector).getListeners(LayoutEvents.LayoutClickEvent::class.java).toList().forEach {
        @Suppress("DEPRECATION")
        removeLayoutClickListener(it as LayoutEvents.LayoutClickListener)
    }
    addChildClickListener(listener)
}

/**
 * Adds a click listener to a layout. The click listener will be called when the layout and any descendant component is clicked,
 * except for descendants which have their own click listeners attached.
 *
 * Only left mouse button clicks are reported; double-clicks are ignored.
 * @param listener the click listener.
 */
fun LayoutEvents.LayoutClickNotifier.addChildClickListener(listener: (LayoutEvents.LayoutClickEvent) -> Unit) {
    (this as Component).addStyleName("clickable")
    addLayoutClickListener({ event ->
        if (event.button != MouseEventDetails.MouseButton.LEFT) {
            // only handle left mouse clicks
        } else if (event.isDoubleClick) {
            // ignore double-clicks
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
    getListeners(Button.ClickEvent::class.java).toList().forEach {
        @Suppress("DEPRECATION")
        removeClickListener(it as Button.ClickListener)
    }
    // Button only fires left-click events.
    addClickListener(listener)
}

/**
 * Adds or removes given [style] from the component, depending on the value of the [isPresent] parameter.
 */
fun Component.toggleStyleName(style: String, isPresent: Boolean) {
    if (isPresent) addStyleName(style) else removeStyleName(style)
}

/**
 * Returns a set of styles currently present on the component.
 */
val Component.styleNames: Set<String> get() = styleName.split(' ').filterNotBlank().toSet()

/**
 * Checks whether the component has given [style].
 * @param style if contains a space, this is considered to be a list of styles. In such case, all styles must be present on the component.
 */
fun Component.hasStyleName(style: String): Boolean {
    if (style.contains(' ')) return style.split(' ').filterNotBlank().all { hasStyleName(style) }
    return styleNames.contains(style)
}

/**
 * Adds multiple [styles]. Individual items in the styles array may contain spaces.
 */
fun Component.addStyleNames(vararg styles: String) = styles.forEach { addStyleName(it) }

/**
 * Configures this button as primary. Beware - all buttons marked primary using this function, attached to the current UI
 * or Window will be pressed on Enter key press.
 */
fun Button.setPrimary() {
    addStyleName(ValoTheme.BUTTON_PRIMARY)
    setClickShortcut(ENTER)
}

/**
 * Replaces any click listeners with this one.
 * @param listener the listener to set. Only called on left-click.
 */
fun Image.onLeftClick(listener: (MouseEvents.ClickEvent)->Unit): Unit {
    // warning, here we may receive listeners for ContextClickEvents!
    getListeners(MouseEvents.ClickEvent::class.java).filterIsInstance<MouseEvents.ClickListener>().forEach {
        @Suppress("DEPRECATION")
        removeClickListener(it)
    }
    addClickListener {
        if (it.button == MouseEventDetails.MouseButton.LEFT) listener(it)
    }
}

/**
 * Represents a Vaadin component width or height.
 * @param size the size, may be negative for undefined/wrapContent size.
 * @param units states the size units.
 */
data class Size(val size: Float, val units: Sizeable.Unit) : Serializable {
    /**
     * true if this size is set to 100% and the component fills its parent in this dimension.
     */
    val isFillParent: Boolean
            get() = size >= 100 && units == Sizeable.Unit.PERCENTAGE
    /**
     * Same as [isFillParent], it's here just to keep in sync with Vaadin terminology ([Component.setSizeFull]).
     */
    val isFull: Boolean
            get() = isFillParent
    /**
     * true if this component wraps its content in this dimension (size is -1px).
     */
    val isWrapContent: Boolean
            get() = size < 0
    /**
     * Same as [isWrapContent], it's here just to keep in sync with Vaadin terminology ([Component.setSizeUndefined]).
     */
    val isUndefined: Boolean
            get() = isWrapContent

    override fun toString() = "$size${units.symbol}"

    val isPixels: Boolean get() = units == Sizeable.Unit.PIXELS

    val isPercent: Boolean get() = units == Sizeable.Unit.PERCENTAGE

    /**
     * True if the component is of fixed size, e.g. 48px, 20em etc. When the size is fixed,
     * it cannot be [isWrapContent] nor [isFillParent]
     */
    val isFixed: Boolean get() = !isPercent && size >= 0
}

/**
 * Tells the component to wrap-content in particular direction. Typing `w = wrapContent` is equal to calling [Sizeable.setWidthUndefined]
 * or setWidth(null) or setWidth(-1, Sizeable.Unit.PIXELS).
 */
val wrapContent = (-1).px
/**
 * Tells the component to fill-parent in particular direction. Typing `w = fillParent` is equal to calling setWidth("100%").
 */
val fillParent = 100.perc

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
 * Walks over this component and all descendants of this component, breadth-first.
 * @return iterable which iteratively walks over this component and all of its descendants.
 */
fun HasComponents.walk(): Iterable<Component> = Iterable {
    TreeIterator<Component>(this, { component -> component as? HasComponents ?: listOf() })
}

/**
 * Directs the browser to go back.
 */
fun goBack() = Page.getCurrent().javaScript.execute("window.history.back();")

fun navigateBack() = goBack()

/**
 * Allows you to create [Binder] like this: `Binder<Person>()` instead of `Binder(Person::class.java)`
 */
inline fun <reified T : Any> BeanValidationBinder(): BeanValidationBinder<T> = BeanValidationBinder(T::class.java)

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
    val listener = shortcutListener(shortcut, action)
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
 * [ShortcutAction.KeyCode] and (optional) [ModifierKey]s.
 * The shortcut is global (bound to the containing Window).
 *
 * Example of shortcut expression: `Ctrl + Alt + C`
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

/**
 * Allows you to bind the component directly in the component's definition. E.g.
 * ```
 * textField("Name:") {
 *   bind(binder).trimmingConverter().bind(Person::name)
 * }
 * ```
 */
fun <BEAN, FIELDVALUE> HasValue<FIELDVALUE>.bind(binder: Binder<BEAN>): Binder.BindingBuilder<BEAN, FIELDVALUE> {
    var builder = binder.forField(this)
    // workaround for https://github.com/vaadin/framework/issues/8664
    @Suppress("UNCHECKED_CAST")
    if (this is AbstractTextField) builder = builder.withNullRepresentation("" as FIELDVALUE)
    return builder
}

/**
 * A type-safe binding which binds only to a property of given type, found on given bean.
 * @param prop the bean property
 */
fun <BEAN, FIELDVALUE> Binder.BindingBuilder<BEAN, FIELDVALUE>.bind(prop: KProperty1<BEAN, FIELDVALUE?>): Binder.Binding<BEAN, FIELDVALUE> =
        bind(prop.name)

/**
 * Causes the Grid to only show given set of columns, and in given order.
 * @param ids show only this properties.
 */
fun <T> Grid<T>.showColumns(vararg ids: KProperty1<T, *>) = setColumns(*ids.map { it.name }.toTypedArray())

/**
 * Allows you to configure a particular column in a Grid. E.g.:
 * ```
 * grid(...) {
 *   showColumns(Person::name, Person::age)
 *   column(Person::age) { isSortable = false }
 * }
 * ```
 * @param prop the bean property for which to retrieve the column
 * @param block run this block with the column as a receiver
 */
@Suppress("UNCHECKED_CAST")
fun <T, V> Grid<T>.column(prop: KProperty1<T, V>, block: Grid.Column<T, V>.() -> Unit = {}): Grid.Column<T, V> =
    (getColumn(prop.name) as Grid.Column<T, V>).apply { block() }

@Suppress("UNCHECKED_CAST")
class ConvertingRenderer<V>(private val convertor: (V)->String) : TextRenderer() {
    override fun encode(value: Any?): JsonValue {
        return if (value == null) super.encode(value) else Json.create(convertor(value as V))
    }
}
fun <T, V> Grid<T>.removeColumn(prop: KProperty1<T, V>) = removeColumn(prop.name)
@Suppress("UNCHECKED_CAST")
fun <T, V> Grid<T>.addColumn(prop: KProperty1<T, V>, renderer: (V)->String, block: Grid.Column<T, V>.() -> Unit = {}): Grid.Column<T, V> =
        (addColumn(prop.name, ConvertingRenderer<V>(renderer)) as Grid.Column<T, V>).apply { block() }

fun Grid<*>.removeAllColumns() = columns.map { it.id }.forEach { removeColumn(it) }

/**
 * Returns component at given [index]. Optimized for [CssLayout] and [AbstractOrderedLayout]s, but works with any
 * [ComponentContainer].
 * @throws IndexOutOfBoundsException If the index is out of range.
 */
fun ComponentContainer.getComponentAt(index: Int): Component = when (this) {
    is CssLayout -> this.getComponent(index)
    is AbstractOrderedLayout -> this.getComponent(index)
    else -> toList()[index]
}

/**
 * Removes a component at given [index] from this container. Optimized for [CssLayout] and [AbstractOrderedLayout]s, but works with any
 * [ComponentContainer].
 * @throws IndexOutOfBoundsException If the index is out of range.
 */
fun ComponentContainer.removeComponentAt(index: Int) {
    removeComponent(getComponentAt(index))
}

/**
 * Returns an [IntRange] of the valid component indices for this container.
 */
val ComponentContainer.indices: IntRange get() = 0..componentCount - 1

/**
 * Removes components with given indices.
 */
fun ComponentContainer.removeComponentsAt(indexRange: IntRange) {
    if (indexRange.isEmpty()) {
    } else if (indexRange == indices) {
        removeAllComponents()
    } else if (this is CssLayout || this is AbstractOrderedLayout) {
        indexRange.reversed().forEach { removeComponentAt(it) }
    } else {
        removeAll(filterIndexed { index, _ -> index in indexRange })
    }
}

fun ComponentContainer.removeAll(components: Iterable<Component>) = components.forEach { removeComponent(it) }
