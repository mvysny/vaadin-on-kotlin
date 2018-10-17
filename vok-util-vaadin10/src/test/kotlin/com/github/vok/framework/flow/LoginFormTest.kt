package com.github.vok.framework.flow

import com.github.karibu.testing.v10.*
import com.github.mvysny.dynatest.DynaTest
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import kotlin.test.expect

class LoginFormTest : DynaTest({
    var username: String? = null
    var password: String? = null
    beforeEach {
        MockVaadin.setup()
        UI.getCurrent().apply {
            loginForm("Test") {
                onLogin { u, p-> username = u; password = p }
            }
        }
    }
    afterEach { MockVaadin.tearDown() }

    test("blank username") {
        _get<Button> { caption = "Login" } ._click()
        expect(null) { username }
        expect(null) { password }
        expect("must not be blank") { _get<TextField> { caption = "Username" }.errorMessage }
    }

    test("blank password") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<Button> { caption = "Login" } ._click()
        expect(null) { username }
        expect(null) { password }
        expect(true) { _get<PasswordField> { caption = "Password" }.isInvalid }
    }

    test("proper login") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<PasswordField> { caption = "Password" }._value = "admin2"
        _get<Button> { caption = "Login" } ._click()
        expect("admin") { username }
        expect("admin2") { password }
    }
})
