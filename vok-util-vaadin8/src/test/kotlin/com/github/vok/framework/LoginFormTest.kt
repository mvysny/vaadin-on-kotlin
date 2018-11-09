package com.github.vok.framework

import com.github.mvysny.dynatest.DynaTest
import com.github.mvysny.kaributesting.v8.MockVaadin
import com.github.mvysny.kaributesting.v8._click
import com.github.mvysny.kaributesting.v8._get
import com.github.mvysny.kaributesting.v8._value
import com.vaadin.ui.Button
import com.vaadin.ui.PasswordField
import com.vaadin.ui.TextField
import com.vaadin.ui.UI
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
        _get<Button> { caption = "Sign In" } ._click()
        expect(null) { username }
        expect(null) { password }
        expect("The&#32;user&#32;name&#32;is&#32;blank") { _get<TextField> { caption = "Username" }.componentError.formattedHtmlMessage }
    }

    test("blank password") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<Button> { caption = "Sign In" } ._click()
        expect(null) { username }
        expect(null) { password }
        expect("The&#32;password&#32;is&#32;blank") { _get<TextField> { caption = "Password" }.componentError.formattedHtmlMessage }
    }

    test("proper login") {
        _get<TextField> { caption = "Username" }._value = "admin"
        _get<PasswordField> { caption = "Password" }._value = "admin2"
        _get<Button> { caption = "Sign In" } ._click()
        expect("admin") { username }
        expect("admin2") { password }
    }
})
