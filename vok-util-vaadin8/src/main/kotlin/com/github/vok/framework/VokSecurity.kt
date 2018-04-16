package com.github.vok.framework

import com.github.vok.framework.VokSecurity.install
import com.github.vok.karibudsl.*
import com.github.vok.security.AccessRejectedException
import com.github.vok.security.AllowRoles
import com.github.vok.security.loggedInUserResolver
import com.vaadin.icons.VaadinIcons
import com.vaadin.navigator.Navigator
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.server.UserError
import com.vaadin.ui.*
import com.vaadin.ui.themes.ValoTheme

/**
 * The security provider. Don't forget to call [install] from your UI!
 */
object VokSecurity {
    private val viewChangeListener = object : ViewChangeListener {
        override fun beforeViewChange(event: ViewChangeListener.ViewChangeEvent): Boolean {
            checkViewAccessible(event.newView.javaClass)
            return true
        }

        override fun afterViewChange(event: ViewChangeListener.ViewChangeEvent) {
            checkViewAccessible(event.newView.javaClass)
        }
    }

    private fun checkViewAccessible(viewClass: Class<out View>) {
        // if this is not an UI thread, then we can't retrieve the current session and we can't accurately check for user's roles
        // just a safety precaution since we should be called from Navigator only, which always runs in the UI thread.
        checkUIThread()
        VaadinOnKotlin.loggedInUserResolver!!.checkPermissionsOnClass(viewClass)
    }

    /**
     * Call this from your `UI.init()` function after the [Navigator] has been set. Hooks will be installed into the [Navigator]
     * which will check for [AllowRoles] annotations on views.
     *
     * Do not forget to install an error handler (by the means of [UI.setErrorHandler]) and handle the [AccessRejectedException] there appropriately.
     * Usually showing an error notification, or an "access denied" view is the best approach.
     */
    fun install() {
        checkUIThread()
        check(VaadinOnKotlin.loggedInUserResolver != null) { "The VaadinOnKotlin.resolver has not been set. You need to set it before the @AllowRoles annotation can be checked on views" }
        val navigator: Navigator = checkNotNull(UI.getCurrent().navigator) { "The UI.getCurrent().navigator returns null - there is no Navigator set. You must set a navigator prior calling this function" }
        navigator.addViewChangeListener(viewChangeListener)
    }
}

/**
 * A simple full-screen login form which shows a simple login form; calls [login] on login.
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
 *       // install the error handler which would handle AccessRejectedException
 *   }
 * }
 *
 * class LoginView : VerticalLayout() {
 *   init {
 *      setSizeFull()
 *      val loginForm = object : LoginForm("My App") {
 *        override fun doLogin(username: String, password: String) {
 *          val user = User.findByUsername(username)
 *          if (user == null) {
 *            this.username.componentError = UserError("The user does not exist")
 *            return
 *          }
 *          if (!user.passwordMatches(password)) {
 *              this.password.componentError = UserError("Invalid password")
 *              return
 *          }
 *          Session.loggedInUser = user
 *          Page.getCurrent().reload()  // this will cause the UI to be re-created, but the user is now logged in so the MainLayout should be instantiated etc.
 *        }
 *      }
 *      addComponent(loginForm)
 *      loginForm.alignment = Alignment.MIDDLE_CENTER
 *   }
 * }
 * ```
 *
 * If only parts of the app are protected, you may simply show the LoginForm class in a `Window`, when your app-specific login button is pressed.
 */
abstract class LoginForm(appName: String) : Panel() {
    protected lateinit var appNameLabel: Label
    protected lateinit var username: TextField
    protected lateinit var password: TextField
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
                username = textField("Username") {
                    expandRatio = 1f; w = fillParent
                    icon = VaadinIcons.USER; styleName = ValoTheme.TEXTFIELD_INLINE_ICON
                }
                password = passwordField("Password") {
                    expandRatio = 1f; w = fillParent
                    icon = VaadinIcons.LOCK; styleName = ValoTheme.TEXTFIELD_INLINE_ICON
                }
                button("Sign In") {
                    alignment = Alignment.BOTTOM_RIGHT; setPrimary()
                    onLeftClick { login() }
                }
            }
        }
    }

    protected fun login() {
        username.componentError = null
        password.componentError = null
        val user: String = username.value.trim()
        if (user.isBlank()) {
            username.componentError = UserError("The user name is blank")
            return
        }
        val password: String = password.value.trim()
        if (password.isBlank()) {
            this.password.componentError = UserError("The password is blank")
            return
        }
        doLogin(user, password)
    }

    /**
     * Tries to log in the user with given [username] and [password]. Both are not blank. If such user does not exist, or the password
     * does not match, just set the appropriate [UserError] to [username] or [password] and bail out. Else,
     * log in the user (e.g. by storing the user into the session) and reload the page ([com.vaadin.server.Page.reload]) (so that the UI
     * is re-created and redraws the welcome page for the user, if the entire app is user-protected), or navigate to the user's welcome view.
     */
    protected abstract fun doLogin(username: String, password: String)
}
