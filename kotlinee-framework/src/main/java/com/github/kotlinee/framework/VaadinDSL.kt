package com.github.kotlinee.framework

import com.vaadin.data.Container
import com.vaadin.server.ExternalResource
import com.vaadin.server.Resource
import com.vaadin.ui.*

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

fun HasComponents.absoluteLayout(block: AbsoluteLayout.()->Unit = {}) = init(AbsoluteLayout(), block)

fun HasComponents.button(caption: String? = null, leftClickListener: ((Button.ClickEvent)->Unit)? = null, block: Button.() -> Unit = {})
        = init(Button(caption), block).apply {
    if (leftClickListener != null) setLeftClickListener(leftClickListener)
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
    if (leftClickListener != null) setLeftClickListener(leftClickListener)
}

fun HasComponents.nativeSelect(caption: String? = null, block: NativeSelect.()->Unit = {}) = init(NativeSelect(), block)

fun HasComponents.optionGroup(caption: String? = null, block: OptionGroup.()->Unit = {}) = init(OptionGroup(), block)

fun HasComponents.panel(caption: String? = null, block: Panel.()->Unit = {}) = init(Panel(), block)

fun HasComponents.popupDateField(caption: String? = null, block: PopupDateField.()->Unit = {}) = init(PopupDateField(caption), block)

fun HasComponents.passwordField(caption: String? = null, block: PasswordField.()->Unit = {}) = init(PasswordField(caption), block)

fun HasComponents.progressBar(block: ProgressBar.()->Unit = {}) = init(ProgressBar(), block)

fun HasComponents.richTextArea(caption: String? = null, block: RichTextArea.()->Unit = {}) = init(RichTextArea(caption), block)

fun HasComponents.slider(caption: String? = null, block: Slider.()->Unit = {}) = init(Slider(caption), block)

fun HasComponents.table(caption: String? = null, dataSource: Container? = null, block: Table.()->Unit = {}) =
        init(Table(caption, dataSource), block)

fun HasComponents.tabSheet(block: TabSheet.()->Unit = {}) = init(TabSheet(), block)

fun HasComponents.textArea(caption: String? = null, block: TextArea.()->Unit = {}) =
        init(TextArea(), block).apply { nullRepresentation = "" }

fun HasComponents.tree(caption: String? = null, block: Tree.()->Unit = {}) = init(Tree(), block)

fun HasComponents.treeTable(caption: String? = null, dataSource: Container? = null, block: TreeTable.()->Unit = {}) =
        init(TreeTable(caption, dataSource), block)

fun HasComponents.upload(caption: String? = null, block: Upload.()->Unit = {}) = init(Upload(caption, null), block)

fun HasComponents.verticalSplitPanel(block: VerticalSplitPanel.()->Unit = {}) = init(VerticalSplitPanel(), block)

fun HasComponents.horizontalSplitPanel(block: HorizontalSplitPanel.()->Unit = {}) = init(HorizontalSplitPanel(), block)

fun HasComponents.video(caption: String? = null, resource: Resource? = null, block: Video.()->Unit = {}) = init(Video(caption, resource), block)
