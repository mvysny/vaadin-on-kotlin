package com.github.vok.example.crud

import com.github.mvysny.karibudsl.v8.*
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.Version
import com.vaadin.ui.VerticalLayout
import com.vaadin.ui.themes.ValoTheme

/**
 * This is the root (or main) view. MyUI initially shows view whose name is "" (an empty string).
 * @author mvy
 */
@AutoView("")
class WelcomeView: VerticalLayout(), View {

    companion object {
        fun navigateTo() = navigateToView<WelcomeView>()
    }

    init {
        isMargin = false
        label("Vaadin On Kotlin") {
            w = fillParent
            addStyleNames(ValoTheme.LABEL_H1, ValoTheme.LABEL_COLORED)
        }
        label {
            w = fillParent
            html("""Welcome to the Vaadin-On-Kotlin demo. Vaadin-On-Kotlin provides means to creating rich Vaadin apps:
<ul><li>Provides Vaadin DSL builder support</li>
<li>Provides simple database access via db {} function</li></ul>
And more.<br/>
To learn more, please visit <a href="http://www.vaadinonkotlin.eu/">Vaadin-on-Kotlin</a> home page.""")
        }
        label {
            w = fillParent
            html("<strong>Vaadin version:</strong> ${Version.getFullVersion()}<br/><strong>Kotlin version:</strong> ${KotlinVersion.CURRENT}")
        }
    }

    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}
