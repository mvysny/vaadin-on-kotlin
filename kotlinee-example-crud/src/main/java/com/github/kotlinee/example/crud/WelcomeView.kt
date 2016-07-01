package com.github.kotlinee.example.crud

import com.github.kotlinee.framework.vaadin.*
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
        setSizeFull(); isMargin = true
        label {
            //language=HTML
            html("""<h3>Kotlinee</h3>Welcome to the Kotlinee demo. Kotlinee provides means to creating rich Vaadin apps:
            <ul><li>Provides Vaadin DSL builder support</li>
            <li>Provides simple database access via db {} function</li></ul>
            And more.""")
        }
    }

    override fun enter(event: ViewChangeListener.ViewChangeEvent?) {
    }
}

