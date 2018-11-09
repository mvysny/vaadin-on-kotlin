package com.github.vok.framework.flow

import com.github.mvysny.karibudsl.v10.*
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import javax.validation.constraints.NotBlank

/**
 * A simple login form which shows a simple login form; calls a handler provided in the [onLogin] when user clicks the "Login" button.
 *
 * There are two ways to use this form. If the whole app is user-protected (an user must log in to view any view of the app, there are no views that an anonymous user may view), then
 * it is simply possible to show the form as a full-screen in the UI if no user is logged in:
 *
 * ```
 * @BodySize(width = "100vw", height = "100vh")
 * @HtmlImport("frontend://styles.html")
 * @Viewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes")
 * @Theme(Lumo::class)
 * class MainLayout : AppHeaderLayout(), RouterLayout, BeforeEnterObserver {
 *   override fun beforeEnter(event: BeforeEnterEvent) {
 *     if (!Session.loginManager.isLoggedIn) {
 *       event.rerouteTo(LoginView::class.java)
 *     } else {
 *       VaadinOnKotlin.loggedInUserResolver!!.checkPermissionsOnClass(event.navigationTarget)
 *     }
 *   }
 *   ...
 * }
 *
 * @BodySize(width = "100vw", height = "100vh")
 * @HtmlImport("frontend://styles.html")
 * @Viewport("width=device-width, minimum-scale=1, initial-scale=1, user-scalable=yes")
 * @Theme(Lumo::class)
 * @Route("login")
 * class LoginView : VerticalLayout() {
 *   private val loginForm: LoginForm
 *   init {
 *     setSizeFull(); isPadding = false; content { center() }
 *
 *     loginForm = loginForm("VoK Security Demo") {
 *       text("Log in as user/user or admin/admin")
 *       onLogin { username, password ->
 *         val user = User.findByUsername(username)
 *         if (user == null) {
 *           usernameField.isInvalid = true
 *           usernameField.errorMessage = "No such user"
 *         } else if (!user.passwordMatches(password)) {
 *           passwordField.isInvalid = true
 *           passwordField.errorMessage = "Incorrect password"
 *         } else {
 *           Session.loginManager.login(user)
 *         }
 *       }
 *     }
 *   }
 * }
 * ```
 *
 * If only parts of the app are protected, you may simply show the LoginForm class in a `Window`, when your app-specific login button is pressed.
 */
class LoginForm(appName: String) : VerticalLayout() {
    data class UsernamePassword(@field:NotBlank var username: String = "", @field:NotBlank var password: String = "")
    private val binder = beanValidationBinder<UsernamePassword>()
    lateinit var usernameField: TextField
        private set
    lateinit var passwordField: PasswordField
        private set
    private var loginHandler: (username: String, password: String)->Unit = { _, _ -> }

    fun onLogin(loginBlock: (username: String, password: String)->Unit) {
        this.loginHandler = loginBlock
    }

    init {
        width = "500px"; isSpacing = false
        binder.bean = UsernamePassword()

        horizontalLayout {
            width = "100%"
            // the trick to this layout is to use the "between" layout mode, which adds as much space as possible between two elements, thus pushing
            // first child to the left (the "Welcome" label), while pushing second child to the right (the AppName label).
            // yet in order for this to work, it's important to disable spacing: https://github.com/vaadin/vaadin-ordered-layout-flow/issues/54
            isSpacing = false
            content { align(between, baseline) }

            h3("Welcome")
            h4(appName)
        }
        horizontalLayout {
            width = "100%"
            usernameField = textField("Username") {
                isExpand = true; minWidth = "0px"
                prefixComponent = Icon(VaadinIcon.USER)
                bind(binder).asRequired().trimmingConverter().bind(UsernamePassword::username)
            }
            passwordField = passwordField("Password") {
                isExpand = true; minWidth = "0px"
                prefixComponent = Icon(VaadinIcon.LOCK)
                bind(binder).asRequired().trimmingConverter().bind(UsernamePassword::password)
            }
            button("Login") {
                setPrimary()
                onLeftClick { login() }
            }
        }
    }

    private fun login() {
        if (binder.validate().isOk) {
            loginHandler(binder.bean.username, binder.bean.password)
        }
    }
}

fun (@VaadinDsl HasComponents).loginForm(appName: String, block: (@VaadinDsl LoginForm).() -> Unit = {}) = init(LoginForm(appName), block)
