package com.github.vok.example.crudflow.person

import com.github.vok.karibudsl.flow.h2
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.page.BodySize
import com.vaadin.flow.component.page.Viewport
import com.vaadin.flow.router.Route
import com.vaadin.flow.theme.Theme
import com.vaadin.flow.theme.lumo.Lumo

/**
 * The main view contains a button and a template element.
 */
@BodySize(width = "100vw", height = "100vh")
@Route("")
@Viewport("width=device-width, minimum-scale=1.0, initial-scale=1.0, user-scalable=yes")
@Theme(Lumo::class)
class PersonListView : VerticalLayout() {
    init {
        setSizeFull()
        h2("It works!")
    }
}
