package com.github.vok.framework

import com.github.karibu.testing.MockVaadin
import com.github.karibu.testing._click
import com.github.karibu.testing._get
import com.github.karibu.testing._value
import com.github.mvysny.dynatest.DynaTest
import com.vaadin.ui.Button
import com.vaadin.ui.PasswordField
import com.vaadin.ui.TextField
import com.vaadin.ui.UI
import kotlin.test.expect

class MyLoginForm : LoginForm("Test") {
    var username: String? = null
    var password: String? = null
    override fun doLogin(username: String, password: String) {
        this.username = username
        this.password = password
    }
}

class LoginFormTest : DynaTest({
    lateinit var form: MyLoginForm
    beforeEach {
        MockVaadin.setup()
        form = MyLoginForm()
        UI.getCurrent().content = form
    }

    test("blank username") {
        _get<Button> { caption = "Sign In" } ._click()
        expect(null) {form.username }
        expect(null) {form.password }
        expect("The&#32;user&#32;name&#32;is&#32;blank") { _get<TextField> { caption = "Username" }.componentError.formattedHtmlMessage }
    }

    test("blank password") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<Button> { caption = "Sign In" } ._click()
        expect(null) {form.username }
        expect(null) {form.password }
        expect("The&#32;password&#32;is&#32;blank") { _get<TextField> { caption = "Password" }.componentError.formattedHtmlMessage }
    }

    test("proper login") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<PasswordField> { caption = "Password" }._value = "admin2"
        _get<Button> { caption = "Sign In" } ._click()
        expect("admin") {form.username }
        expect("admin2") {form.password }
    }
})
