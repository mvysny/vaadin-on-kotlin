package com.github.vok.framework

import com.github.vok.karibudsl.*
import com.vaadin.icons.VaadinIcons
import com.vaadin.server.UserError
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme

/**
 * A simple login form which shows a simple login form; calls a handler provided in the [onLogin] when user clicks the "Sign In" button.
 *
 * There are two ways to use this form. If the whole app is user-protected (an user must log in to view any view of the app, there are no views that an anonymous user may view), then
 * it is simply possible to show the form as a full-screen in the UI if no user is logged in:
 * ```
 * class MyUI : UI() {
 *   override fun init(request: VaadinRequest) {
 *     if (Session.loggedInUser == null) {
 *       content = LoginView()
 *       return;
 *     } else {
 *       content = MainLayout()  // create a main layout, populate the menu, etc
 *       navigator = Navigator(this, content as ViewProvider)
 *       navigator.addProvider(autoViewProvider)
 *       VokSecurity.install()
 *       // you need to install the error handler which would handle AccessRejectedException
 *   }
 * }
 *
 * class LoginView : VerticalLayout() {
 *   init {
 *      setSizeFull()
 *      loginForm("My App") {
 *        alignment = Alignment.MIDDLE_CENTER
 *        onLogin { username, password ->
 *          val user = User.findByUsername(username)
 *          if (user == null) {
 *            usernameField.componentError = UserError("The user does not exist")
 *          } else if (!user.passwordMatches(password)) {
 *            passwordField.componentError = UserError("Invalid password")
 *          } else {
 *            Session.loggedInUser = user
 *            Page.getCurrent().reload()  // this will cause the UI to be re-created, but the user is now logged in so the MainLayout should be instantiated etc.
 *          }
 *        }
 *      }
 *   }
 * }
 * ```
 *
 * If only parts of the app are protected, you may simply show the LoginForm class in a `Window`, when your app-specific login button is pressed.
 */
class LoginForm(appName: String) : Panel() {
    lateinit var appNameLabel: Label
        private set
    lateinit var usernameField: TextField
        private set
    lateinit var passwordField: TextField
        private set
    lateinit var loginButton: Button
        private set
    init {
        w = 500.px
        verticalLayout {
            w = fillParent
            horizontalLayout {
                w = fillParent
                label("Welcome") {
                    alignment = Alignment.BOTTOM_LEFT
                    addStyleNames(ValoTheme.LABEL_H4, ValoTheme.LABEL_COLORED)
                }
                appNameLabel = label(appName) {
                    alignment = Alignment.BOTTOM_RIGHT; styleName = ValoTheme.LABEL_H3; expandRatio = 1f
                }
            }
            horizontalLayout {
                w = fillParent
                usernameField = textField("Username") {
                    expandRatio = 1f; w = fillParent
                    icon = VaadinIcons.USER; styleName = ValoTheme.TEXTFIELD_INLINE_ICON
                }
                passwordField = passwordField("Password") {
                    expandRatio = 1f; w = fillParent
                    icon = VaadinIcons.LOCK; styleName = ValoTheme.TEXTFIELD_INLINE_ICON
                }
                loginButton = button("Sign In") {
                    alignment = Alignment.BOTTOM_RIGHT; setPrimary()
                    onLeftClick { login() }
                }
            }
        }
    }

    private fun login() {
        usernameField.componentError = null
        passwordField.componentError = null
        val user: String = usernameField.value.trim()
        if (user.isBlank()) {
            usernameField.componentError = UserError("The user name is blank")
            return
        }
        val password: String = passwordField.value.trim()
        if (password.isBlank()) {
            passwordField.componentError = UserError("The password is blank")
            return
        }
        onLoginHandler(user, password)
    }

    private var onLoginHandler: (username: String, password: String)->Unit = {_, _ -> }

    /**
     * The [loginHandler] will try to log in the user with given username and password. Both are not blank and trimmed. If such user does not exist, or the password
     * does not match, just set the appropriate [UserError] to [usernameField] or [passwordField] and bail out. Else,
     * log in the user (e.g. by storing the user into the session) and reload the page ([com.vaadin.server.Page.reload]) (so that the UI
     * is re-created and redraws the welcome page for the user, if the entire app is user-protected), or navigate to the user's welcome view.
     */
    fun onLogin(loginHandler: (username: String, password: String)->Unit) {
        onLoginHandler = loginHandler
    }
}

fun (@VaadinDsl HasComponents).loginForm(appName: String, block: LoginForm.()->Unit = {}) = init(LoginForm(appName), block)
