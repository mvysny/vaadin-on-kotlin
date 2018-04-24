package com.github.vok.framework.flow

import com.github.vok.framework.VaadinOnKotlin
import com.github.vok.karibudsl.flow.*
import com.github.vok.security.loggedInUserResolver
import com.vaadin.flow.component.Component
import com.vaadin.flow.component.HasComponents
import com.vaadin.flow.component.UI
import com.vaadin.flow.component.icon.Icon
import com.vaadin.flow.component.icon.VaadinIcons
import com.vaadin.flow.component.orderedlayout.VerticalLayout
import com.vaadin.flow.component.textfield.PasswordField
import com.vaadin.flow.component.textfield.TextField
import com.vaadin.flow.router.Route
import javax.validation.constraints.NotBlank

/**
 * The security provider. Since Vaadin 10 applications typically don't define their own UIs but use the approach of setting [Route.layout],
 * we will provide support for that as well.
 *
 * Don't forget to install a proper [VaadinOnKotlin.loggedInUserResolver] for your project.
 *
 * ## If you have your own UI class
 *
 * Simply call [install] - the function will add the [UI.addBeforeEnterListener] which will check permissions on all views.
 *
 * ## If you do not have your own UI class
 *
 * Then you typically have a main layout to which all your [Route]s reference by the means of [Route.layout]. You just need to call the [checkPermissionsOfView]
 * function before the View is displayed.
 *
 * Make your `MainLayout` class implement the `BeforeEnterObserver`, then implement the `beforeEnter()` function as follows:
 * ```
 * override fun beforeEnter(event: BeforeEnterEvent) {
 *   if (!Session.loginManager.isLoggedIn) {
 *     event.rerouteTo(LoginView::class.java)
 *   } else {
 *     VokSecurity.checkPermissionsOfView(event.navigationTarget)
 *   }
 * }
 * ```
 */
object VokSecurity {
    /**
     * Checks the permissions of given view. If the parent layout is also annotated with [Route], we'll recursively check that as well.
     * @param viewClass the Vaadin 10 view. Must be a [Component] annotated with [Route].
     */
    fun checkPermissionsOfView(viewClass: Class<*>) {
        require(Component::class.java.isAssignableFrom(viewClass)) { "$viewClass is not a subclass of Component" }
        val route = requireNotNull(viewClass.getAnnotation(Route::class.java)) { "The view $viewClass is not annotated with @Route" }
        val user = checkNotNull(VaadinOnKotlin.loggedInUserResolver) { "The VaadinOnKotlin.loggedInUserResolver has not been set" }
        user.checkPermissionsOnClass(viewClass)
        if (route.layout != UI::class && route.layout.java.getAnnotation(Route::class.java) != null) {
            // recursively check for permissions on the parent layout
            checkPermissionsOfView(route.layout.java)
        }
    }

    /**
     * If you have your own custom UI, then simply call this function from your [UI.init] method. It will install [UI.addBeforeEnterListener]
     * and will check all views.
     */
    fun install() {
        UI.getCurrent().addBeforeEnterListener({ e -> checkPermissionsOfView(e.navigationTarget) })
    }
}

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
                isExpand = true
                prefixComponent = Icon(VaadinIcons.USER)
                bind(binder).asRequired().trimmingConverter().bind(UsernamePassword::username)
            }
            passwordField = passwordField("Password") {
                isExpand = true
                prefixComponent = Icon(VaadinIcons.LOCK)
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
