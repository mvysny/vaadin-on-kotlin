package com.github.vok.framework.vaadin7

import com.github.vok.framework.vaadin.init
import com.vaadin.ui.HasComponents
import com.vaadin.v7.data.Container
import com.vaadin.v7.ui.*

@Deprecated("Use VerticalLayout from Vaadin 8")
fun HasComponents.verticalLayout7(block: VerticalLayout.()->Unit = {}) = init(VerticalLayout(), block)

@Deprecated("Use HorizontalLayout from Vaadin 8")
fun HasComponents.horizontalLayout7(block: HorizontalLayout.()->Unit = {}) = init(HorizontalLayout(), block)

@Deprecated("Use Grid from Vaadin 8")
fun HasComponents.grid7(caption: String? = null, dataSource: Container.Indexed? = null, block: Grid.() -> Unit = {}) = init(Grid(caption, dataSource), block)

/**
 * Creates a [TextField] and attaches it to this component. [TextField.nullRepresentation] is set to an empty string.
 * @param caption optional caption
 * @param value the optional value
 */
@Deprecated("Use TextField from Vaadin 8")
fun HasComponents.textField7(caption: String? = null, value: String? = null, block: TextField.()->Unit = {}): TextField {
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
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.label7(content: String? = null, block: Label.()->Unit = {}) = init(Label(content), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.calendar(caption: String? = null, block: Calendar.()->Unit = {}) = init(Calendar(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.checkBox7(caption: String? = null, checked:Boolean? = null, block: CheckBox.()->Unit = {}) =
        init(if (checked == null) CheckBox(caption) else CheckBox(caption, checked), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.colorPicker7(popupCaption: String? = null, block: ColorPicker.()->Unit = {}) = init(ColorPicker(popupCaption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.comboBox7(caption: String? = null, block: ComboBox.()->Unit = {}) = init(ComboBox(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.dateField7(caption: String? = null, block: DateField.()->Unit = {}) = init(DateField(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.inlineDateField7(caption: String? = null, block: InlineDateField.()->Unit = {}) = init(InlineDateField(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.listSelect7(caption: String? = null, block: ListSelect.()->Unit = {}) = init(ListSelect(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.nativeSelect7(caption: String? = null, block: NativeSelect.()->Unit = {}) = init(NativeSelect(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.optionGroup7(caption: String? = null, block: OptionGroup.()->Unit = {}) = init(OptionGroup(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.popupDateField7(caption: String? = null, block: PopupDateField.()->Unit = {}) = init(PopupDateField(caption), block)

/**
 * Creates a [PasswordField]. [TextField.nullRepresentation] is set to an empty string, and the trimming converter is automatically pre-set.
 *
 * Auto-trimming: passwords generally do not have whitespaces. Pasting a password to a field in a mobile phone will also add a trailing whitespace, which
 * will cause the password to not to match, which is a source of great confusion.
 */
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.passwordField7(caption: String? = null, block: PasswordField.()->Unit = {}): PasswordField {
    val component = PasswordField(caption)
    component.trimmingConverter()
    component.nullRepresentation = ""
    init(component, block)
    return component
}

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.progressBar7(block: ProgressBar.()->Unit = {}) = init(ProgressBar(), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.richTextArea7(caption: String? = null, block: RichTextArea.()->Unit = {}) = init(RichTextArea(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.slider7(caption: String? = null, block: Slider.()->Unit = {}) = init(Slider(caption), block)

@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.table(caption: String? = null, dataSource: Container? = null, block: Table.()->Unit = {}) =
        init(Table(caption, dataSource), block)

/**
 * Creates a [TextArea] and attaches it to this component. [TextField.nullRepresentation] is set to an empty string.
 * @param caption optional caption
 * @param value the optional value
 */
@Deprecated("Deprecated in Vaadin 8")
fun HasComponents.textArea7(caption: String? = null, block: TextArea.()->Unit = {}): TextArea {
    val component = TextArea(caption)
    component.nullRepresentation = ""
    init(component, block)
    return component
}

@Deprecated("Replacement planned in Vaadin 8.1")
fun HasComponents.tree(caption: String? = null, block: Tree.()->Unit = {}) = init(Tree(caption), block)

@Deprecated("Replacement planned in Vaadin 8.1")
fun HasComponents.treeTable(caption: String? = null, dataSource: Container? = null, block: TreeTable.()->Unit = {}) =
        init(TreeTable(caption, dataSource), block)

@Deprecated("Use Upload from Vaadin 8 but beware of it being immediate by default")
fun HasComponents.upload7(caption: String? = null, block: Upload.()->Unit = {}) = init(Upload(caption, null), block)
