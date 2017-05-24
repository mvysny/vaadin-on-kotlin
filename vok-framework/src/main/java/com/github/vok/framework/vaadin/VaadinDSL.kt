package com.github.vok.framework.vaadin

import com.vaadin.data.provider.DataProvider
import com.vaadin.server.ExternalResource
import com.vaadin.server.Resource
import com.vaadin.ui.*
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
@DslMarker
annotation class VaadinDsl

/**
 * A specialized version of [ComponentContainer], for certain special containers. The DSL's
 * [init] method will invoke [addComponent] method with the components being registered.
 *
 * For example, there may be a special container (say, a ticker) which does not attach the components as its Vaadin
 * children immediately - rather it only remembers the components added via [addComponent] in a special list and
 * Vaadin-attaches them once every 10 seconds, one at a time. This way, you can use the DSL to define all children (or
 * pages) of this special component, without having them attached immediately as Vaadin children.
 */
interface SpecialContainer : HasComponents {
    /**
     * Adds the component into this container. Called by [init] when DSL-adding children to this container.
     *
     * Note that there is no `removeComponent()` method nor any sort of support for listing of the components added via this
     * method. This is because the DSL only needs to add the components; the component list itself highly depends on the implementation
     * of the component and might cause confusion with the [HasComponents] list of attached components.
     * Therefore, I'm keeping this interface as dumb as possible.
     * @param component the component to be added.
     */
    fun addComponent(component: Component)
}

/**
 * When introducing extensions for your custom components, just call this in your method. For example:
 *
 * `fun HasComponents.shinyComponent(caption: String? = null, block: ShinyComponent.()->Unit = {}) = init(ShinyComponent(caption), block)`
 *
 * Adds [component] to receiver, see [addChild] for details.
 *
 * @param component the component to attach
 * @param block optional block to run over the component, allowing you to add children to the [component]
 */
fun <T : Component> HasComponents.init(component: T, block: T.()->Unit = {}): T {
    addChild(component)
    component.block()
    return component
}

/**
 * Adds a [child] to this component. Only concrete subclasses are supported:
 *
 * * [ComponentContainer]
 * * [SingleComponentContainer] (fails if the container already has a child)
 * * [PopupView]
 * * [AbstractSplitPanel]
 * * [SpecialContainer]
 *
 * The function will fail if the component
 * is already full (e.g. it is a split panel and it already contains two components).
 *
 * For custom containers just implement the [SpecialContainer] interface.
 */
fun HasComponents.addChild(child: Component) {
    when (this) {
        is ComponentContainer -> addComponent(child)
        is SpecialContainer -> addComponent(child)
        is SingleComponentContainer -> {
            if (componentCount >= 1) throw IllegalArgumentException("$this can only have one child")
            content = child
        }
        is PopupView -> popupComponent = child
        is AbstractSplitPanel -> when (componentCount) {
            0 -> firstComponent = child
            1 -> secondComponent = child
            else -> throw IllegalArgumentException("$this can only have 2 children")
        }
        else -> throw IllegalArgumentException("Unsupported component container $this")
    }
}

fun HasComponents.verticalLayout(block: (@VaadinDsl VerticalLayout).()->Unit = {}) = init(VerticalLayout(), block)

fun HasComponents.horizontalLayout(block: (@VaadinDsl HorizontalLayout).()->Unit = {}) = init(HorizontalLayout(), block)

fun HasComponents.formLayout(block: (@VaadinDsl FormLayout).()->Unit = {}) = init(FormLayout(), block)

fun HasComponents.absoluteLayout(block: (@VaadinDsl AbsoluteLayout).()->Unit = {}) = init(AbsoluteLayout(), block)

fun HasComponents.button(caption: String? = null, leftClickListener: ((Button.ClickEvent)->Unit)? = null, block: (@VaadinDsl Button).() -> Unit = {})
        = init(Button(caption), block).apply {
    if (leftClickListener != null) onLeftClick(leftClickListener)
}

fun <T: Any> HasComponents.grid(clazz: KClass<T>, caption: String? = null, dataProvider: DataProvider<T, *>? = null, block: (@VaadinDsl Grid<T>).() -> Unit = {}) =
    init(Grid<T>(clazz.java)) {
        this.caption = caption
        if (dataProvider != null) this.dataProvider = dataProvider
        block()
    }

fun HasComponents.image(caption: String? = null, resource: Resource? = null, block: (@VaadinDsl Image).()->Unit = {}) = init(Image(caption, resource), block)

/**
 * Creates a [TextField] and attaches it to this component.
 * @param caption optional caption
 * @param value the optional value
 */
fun HasComponents.textField(caption: String? = null, value: String? = null, block: (@VaadinDsl TextField).()->Unit = {}): TextField {
    val textField = TextField(caption, value ?: "")  // TextField no longer accepts null as a value.
    init(textField, block)
    return textField
}

/**
 * Creates a [Label]
 * @param content the label content
 * @param block use to set additional label parameters
 */
fun HasComponents.label(content: String? = null, block: (@VaadinDsl Label).()->Unit = {}) = init(Label(content), block)

fun HasComponents.accordion(block: (@VaadinDsl Accordion).()->Unit = {}) = init(Accordion(), block)

fun HasComponents.audio(caption: String? = null, resource: Resource? = null, block: (@VaadinDsl Audio).()->Unit = {}) = init(Audio(caption, resource), block)

fun HasComponents.browserFrame(url: String? = null, block: (@VaadinDsl BrowserFrame).()->Unit = {}) =
    init(BrowserFrame(null, (if (url == null) null else ExternalResource(url))), block)

fun HasComponents.checkBox(caption: String? = null, checked:Boolean? = null, block: (@VaadinDsl CheckBox).()->Unit = {}) =
        init(if (checked == null) CheckBox(caption) else CheckBox(caption, checked), block)

fun HasComponents.colorPicker(popupCaption: String? = null, block: (@VaadinDsl ColorPicker).()->Unit = {}) = init(ColorPicker(popupCaption), block)

fun <T> HasComponents.comboBox(caption: String? = null, block: (@VaadinDsl ComboBox<T>).()->Unit = {}) = init(ComboBox<T>(caption), block)

fun HasComponents.cssLayout(block: (@VaadinDsl CssLayout).()->Unit = {}) = init(CssLayout(), block)

fun HasComponents.dateField(caption: String? = null, block: (@VaadinDsl DateField).()->Unit = {}) = init(DateField(caption), block)

fun HasComponents.dateTimeField(caption: String? = null, block: (@VaadinDsl DateTimeField).()->Unit = {}) = init(DateTimeField(caption), block)

fun HasComponents.embedded(caption: String? = null, block: (@VaadinDsl Embedded).()->Unit = {}) = init(Embedded(caption), block)

fun HasComponents.gridLayout(columns: Int = 1, rows: Int = 1, block: (@VaadinDsl GridLayout).()->Unit = {}) = init(GridLayout(columns, rows), block)

fun HasComponents.inlineDateField(caption: String? = null, block: (@VaadinDsl InlineDateField).()->Unit = {}) = init(InlineDateField(caption), block)

fun HasComponents.inlineDateTimeField(caption: String? = null, block: (@VaadinDsl InlineDateTimeField).()->Unit = {}) = init(InlineDateTimeField(caption), block)

fun HasComponents.link(caption: String? = null, url: String? = null, block: (@VaadinDsl Link).()->Unit = {}) =
        init(Link(caption, if (url == null) null else ExternalResource(url)), block)

fun <T> HasComponents.listSelect(caption: String? = null, block: (@VaadinDsl ListSelect<T>).()->Unit = {}) = init(ListSelect<T>(caption), block)

fun HasComponents.menuBar(block: (@VaadinDsl MenuBar).()->Unit = {}) = init(MenuBar(), block)

fun HasComponents.nativeButton(caption: String? = null, leftClickListener: ((Button.ClickEvent)->Unit)? = null, block: (@VaadinDsl NativeButton).() -> Unit = {})
        = init(NativeButton(caption), block).apply {
    if (leftClickListener != null) onLeftClick(leftClickListener)
}

fun <T> HasComponents.nativeSelect(caption: String? = null, block: (@VaadinDsl NativeSelect<T>).()->Unit = {}) = init(NativeSelect<T>(caption), block)

fun <T> HasComponents.radioButtonGroup(caption: String? = null, block: (@VaadinDsl RadioButtonGroup<T>).()->Unit = {}) = init(RadioButtonGroup<T>(caption), block)

fun <T> HasComponents.checkBoxGroup(caption: String? = null, block: (@VaadinDsl CheckBoxGroup<T>).()->Unit = {}) = init(CheckBoxGroup<T>(caption), block)

fun HasComponents.panel(caption: String? = null, block: (@VaadinDsl Panel).()->Unit = {}) = init(Panel(caption), block)

/**
 * Creates a [PasswordField]. [TextField.nullRepresentation] is set to an empty string, and the trimming converter is automatically pre-set.
 *
 * *WARNING:* When Binding to a field, do not forget to call [Binder.BindingBuilder.trimmingConverter] to perform auto-trimming:
 * passwords generally do not have whitespaces. Pasting a password to a field in a mobile phone will also add a trailing whitespace, which
 * will cause the password to not to match, which is a source of great confusion.
 */
fun HasComponents.passwordField(caption: String? = null, block: (@VaadinDsl PasswordField).()->Unit = {}): PasswordField {
    val component = PasswordField(caption)
    init(component, block)
    return component
}

fun HasComponents.progressBar(block: (@VaadinDsl ProgressBar).()->Unit = {}) = init(ProgressBar(), block)

fun HasComponents.richTextArea(caption: String? = null, block: (@VaadinDsl RichTextArea).()->Unit = {}) = init(RichTextArea(caption), block)

fun HasComponents.slider(caption: String? = null, block: (@VaadinDsl Slider).()->Unit = {}) = init(Slider(caption), block)

fun HasComponents.tabSheet(block: (@VaadinDsl TabSheet).()->Unit = {}) = init(TabSheet(), block)

/**
 * Creates a [TextArea] and attaches it to this component.
 * @param caption optional caption
 * @param value the optional value
 */
fun HasComponents.textArea(caption: String? = null, block: (@VaadinDsl TextArea).()->Unit = {}): TextArea {
    val component = TextArea(caption)
    init(component, block)
    return component
}

fun <T: Any> HasComponents.tree(caption: String? = null, block: (@VaadinDsl Tree<T>).()->Unit = {}) = init(Tree<T>(caption), block)

fun HasComponents.upload(caption: String? = null, block: (@VaadinDsl Upload).()->Unit = {}) = init(Upload(caption, null), block)

fun HasComponents.verticalSplitPanel(block: (@VaadinDsl VerticalSplitPanel).()->Unit = {}) = init(VerticalSplitPanel(), block)

fun HasComponents.horizontalSplitPanel(block: (@VaadinDsl HorizontalSplitPanel).()->Unit = {}) = init(HorizontalSplitPanel(), block)

fun HasComponents.video(caption: String? = null, resource: Resource? = null, block: (@VaadinDsl Video).()->Unit = {}) = init(Video(caption, resource), block)

fun HasComponents.popupView(small: String? = null, block: (@VaadinDsl PopupView).()->Unit = {}): PopupView {
    val result = init(PopupView(SimpleContent.Companion.EMPTY), block)
    if (small != null) result.minimizedValueAsHTML = small
    return result
}
