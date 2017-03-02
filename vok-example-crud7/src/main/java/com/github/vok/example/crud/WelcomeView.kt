package com.github.vok.example.crud

import com.github.vok.framework.vaadin.*
import com.github.vok.framework.vaadin7.html
import com.github.vok.framework.vaadin7.label7
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.shared.Version
import com.vaadin.ui.VerticalLayout

/**
 * This is the root (or main) view. MyUI initially shows view whose name is "" (an empty string).
 * @author mvy
 */
@ViewName("")
class WelcomeView: VerticalLayout(), View {

    companion object {
        fun navigateTo() = navigateToView<WelcomeView>()
    }

    init {
        setSizeFull(); isMargin = true
        label7 {
            html("""<h3>VaadinOnKotlin</h3>Welcome to the VaadinOnKotlin demo. VaadinOnKotlin provides means to creating rich Vaadin apps:
            <ul><li>Provides Vaadin DSL builder support</li>
            <li>Provides simple database access via db {} function</li></ul>
            And more.""")
        }
        label7("Vaadin version ${Version.getFullVersion()}")
    }

    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}

