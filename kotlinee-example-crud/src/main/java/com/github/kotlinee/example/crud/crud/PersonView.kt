package com.github.kotlinee.example.crud.crud

import com.github.kotlinee.example.crud.WelcomeView
import com.github.kotlinee.framework.Session
import com.github.kotlinee.framework.vaadin.navigateToView
import com.github.kotlinee.framework.vaadin.unshortenParam
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.Label
import com.vaadin.ui.VerticalLayout

/**
 * Demonstrates the ability to transfer complex objects through URL params.
 * @author mavi
 */
class PersonView: VerticalLayout(), View {
    override fun enter(event: ViewChangeListener.ViewChangeEvent) {
        val person = event.unshortenParam(0) as Person? ?: return navigateToView<WelcomeView>()
        removeAllComponents()
        addComponent(Label(person.toString()))
    }

    companion object {
        fun navigateTo(person: Person) {
            navigateToView<PersonView>(Session.urlParamShortener.put(person))
        }
    }
}
