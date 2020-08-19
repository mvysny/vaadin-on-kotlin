package eu.vaadinonkotlin.vaadin10

import com.vaadin.flow.component.login.LoginForm

@Deprecated("Use LoginForm.addLoginListener()")
public fun LoginForm.onLogin(loginBlock: (username: String, password: String) -> Unit) {
    addLoginListener { e -> loginBlock(e.username, e.password) }
}
