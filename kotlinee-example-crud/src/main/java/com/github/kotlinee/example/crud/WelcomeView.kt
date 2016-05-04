package com.github.kotlinee.example.crud

import com.github.kotlinee.framework.vaadin.ViewName
import com.github.kotlinee.framework.vaadin.html
import com.github.kotlinee.framework.vaadin.label
import com.github.kotlinee.framework.vaadin.navigateToView
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
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
        setSizeFull()
        setMargin(true)
        label {
            html("""<h3>Kotlinee</h3>Welcome to the Kotlinee demo. Kotlinee provides means to creating rich Vaadin apps:
            <ul><li>Provides Vaadin DSL builder support</li>
            <li>Provides simple database access via db {} function</li></ul>
            And more.""")
        }
    }

    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}

