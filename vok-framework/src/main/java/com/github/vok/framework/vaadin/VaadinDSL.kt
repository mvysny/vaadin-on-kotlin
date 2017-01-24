package com.github.vok.framework.vaadin

import com.vaadin.data.Container
import com.vaadin.server.ExternalResource
import com.vaadin.server.Resource
import com.vaadin.ui.*

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
 * Input [component] is automatically added to the children of this [ComponentContainer], or replaces content in [SingleComponentContainer] or [PopupView].
 * For custom containers just implement the [SpecialContainer] interface.
 *
 * @param component the component to attach
 * @param block optional block to run over the component, allowing you to add children to the [component]
 */
fun <T : Component> HasComponents.init(component: T, block: T.()->Unit = {}): T {
    when (this) {
        is ComponentContainer -> addComponent(component)
        is SpecialContainer -> addComponent(component)
        is SingleComponentContainer -> content = component
        is PopupView -> popupComponent = component
        else -> throw RuntimeException("Unsupported component container $this")
    }
    component.block()
    return component
}

fun HasComponents.verticalLayout(block: VerticalLayout.()->Unit = {}) = init(VerticalLayout(), block)

fun HasComponents.horizontalLayout(block: HorizontalLayout.()->Unit = {}) = init(HorizontalLayout(), block)

fun HasComponents.formLayout(block: FormLayout.()->Unit = {}) = init(FormLayout(), block)

fun HasComponents.absoluteLayout(block: AbsoluteLayout.()->Unit = {}) = init(AbsoluteLayout(), block)

fun HasComponents.button(caption: String? = null, leftClickListener: ((Button.ClickEvent)->Unit)? = null, block: Button.() -> Unit = {})
        = init(Button(caption), block).apply {
    if (leftClickListener != null) onLeftClick(leftClickListener)
}

fun HasComponents.grid(caption: String? = null, dataSource: Container.Indexed? = null, block: Grid.() -> Unit = {}) = init(Grid(caption, dataSource), block)

fun HasComponents.image(caption: String? = null, resource: Resource? = null, block: Image.()->Unit = {}) = init(Image(caption, resource), block)

/**
 * Creates a [TextField] and attaches it to this component. [TextField.nullRepresentation] is set to an empty string.
 * @param caption optional caption
 * @param value the optional value
 */
fun HasComponents.textField(caption: String? = null, value: String? = null, block: TextField.()->Unit = {}): TextField {
    val textField = TextField(caption, value)
    textField.nullRepresentation = ""
    init(textField, block)
    return textField
}

/**
 * Creates a [Label]
 * @param content the label content
 * @param block use to set additional label parameters
 */
fun HasComponents.label(content: String? = null, block: Label.()->Unit = {}) = init(Label(content), block)

fun HasComponents.accordion(block: Accordion.()->Unit = {}) = init(Accordion(), block)

fun HasComponents.audio(caption: String? = null, resource: Resource? = null, block: Audio.()->Unit = {}) = init(Audio(caption, resource), block)

fun HasComponents.browserFrame(url: String? = null, block: BrowserFrame.()->Unit = {}) =
    init(BrowserFrame(null, (if (url == null) null else ExternalResource(url))), block)

fun HasComponents.calendar(caption: String? = null, block: Calendar.()->Unit = {}) = init(Calendar(caption), block)

fun HasComponents.checkBox(caption: String? = null, checked:Boolean? = null, block: CheckBox.()->Unit = {}) =
        init(if (checked == null) CheckBox(caption) else CheckBox(caption, checked), block)

fun HasComponents.colorPicker(popupCaption: String? = null, block: ColorPicker.()->Unit = {}) = init(ColorPicker(popupCaption), block)

fun HasComponents.comboBox(caption: String? = null, block: ComboBox.()->Unit = {}) = init(ComboBox(caption), block)

fun HasComponents.cssLayout(block: CssLayout.()->Unit = {}) = init(CssLayout(), block)

fun HasComponents.dateField(caption: String? = null, block: DateField.()->Unit = {}) = init(DateField(caption), block)

fun HasComponents.embedded(caption: String? = null, block: Embedded.()->Unit = {}) = init(Embedded(caption), block)

fun HasComponents.gridLayout(columns: Int = 1, rows: Int = 1, block: GridLayout.()->Unit = {}) = init(GridLayout(columns, rows), block)

fun HasComponents.inlineDateField(caption: String? = null, block: InlineDateField.()->Unit = {}) = init(InlineDateField(caption), block)

fun HasComponents.link(caption: String? = null, url: String? = null, block: Link.()->Unit = {}) =
        init(Link(caption, if (url == null) null else ExternalResource(url)), block)

fun HasComponents.listSelect(caption: String? = null, block: ListSelect.()->Unit = {}) = init(ListSelect(caption), block)

fun HasComponents.menuBar(block: MenuBar.()->Unit = {}) = init(MenuBar(), block)

fun HasComponents.nativeButton(caption: String? = null, leftClickListener: ((Button.ClickEvent)->Unit)? = null, block: NativeButton.() -> Unit = {})
        = init(NativeButton(caption), block).apply {
    if (leftClickListener != null) onLeftClick(leftClickListener)
}

fun HasComponents.nativeSelect(caption: String? = null, block: NativeSelect.()->Unit = {}) = init(NativeSelect(caption), block)

fun HasComponents.optionGroup(caption: String? = null, block: OptionGroup.()->Unit = {}) = init(OptionGroup(caption), block)

fun HasComponents.panel(caption: String? = null, block: Panel.()->Unit = {}) = init(Panel(caption), block)

fun HasComponents.popupDateField(caption: String? = null, block: PopupDateField.()->Unit = {}) = init(PopupDateField(caption), block)

/**
 * Creates a [PasswordField]. [TextField.nullRepresentation] is set to an empty string, and the trimming converter is automatically pre-set.
 *
 * Auto-trimming: passwords generally do not have whitespaces. Pasting a password to a field in a mobile phone will also add a trailing whitespace, which
 * will cause the password to not to match, which is a source of great confusion.
 */
fun HasComponents.passwordField(caption: String? = null, block: PasswordField.()->Unit = {}): PasswordField {
    val component = PasswordField(caption)
    component.trimmingConverter()
    component.nullRepresentation = ""
    init(component, block)
    return component
}

fun HasComponents.progressBar(block: ProgressBar.()->Unit = {}) = init(ProgressBar(), block)

fun HasComponents.richTextArea(caption: String? = null, block: RichTextArea.()->Unit = {}) = init(RichTextArea(caption), block)

fun HasComponents.slider(caption: String? = null, block: Slider.()->Unit = {}) = init(Slider(caption), block)

fun HasComponents.table(caption: String? = null, dataSource: Container? = null, block: Table.()->Unit = {}) =
        init(Table(caption, dataSource), block)

fun HasComponents.tabSheet(block: TabSheet.()->Unit = {}) = init(TabSheet(), block)

/**
 * Creates a [TextArea] and attaches it to this component. [TextField.nullRepresentation] is set to an empty string.
 * @param caption optional caption
 * @param value the optional value
 */
fun HasComponents.textArea(caption: String? = null, block: TextArea.()->Unit = {}): TextArea {
    val component = TextArea(caption)
    component.nullRepresentation = ""
    init(component, block)
    return component
}

fun HasComponents.tree(caption: String? = null, block: Tree.()->Unit = {}) = init(Tree(caption), block)

fun HasComponents.treeTable(caption: String? = null, dataSource: Container? = null, block: TreeTable.()->Unit = {}) =
        init(TreeTable(caption, dataSource), block)

fun HasComponents.upload(caption: String? = null, block: Upload.()->Unit = {}) = init(Upload(caption, null), block)

fun HasComponents.verticalSplitPanel(block: VerticalSplitPanel.()->Unit = {}) = init(VerticalSplitPanel(), block)

fun HasComponents.horizontalSplitPanel(block: HorizontalSplitPanel.()->Unit = {}) = init(HorizontalSplitPanel(), block)

fun HasComponents.video(caption: String? = null, resource: Resource? = null, block: Video.()->Unit = {}) = init(Video(caption, resource), block)

fun HasComponents.popupView(small: String? = null, block: PopupView.()->Unit = {}): PopupView {
    val result = init(PopupView(SimpleContent.Companion.EMPTY), block)
    if (small != null) result.minimizedValueAsHTML = small
    return result
}
