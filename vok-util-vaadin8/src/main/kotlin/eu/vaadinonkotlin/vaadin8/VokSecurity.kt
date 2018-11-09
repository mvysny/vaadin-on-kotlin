package eu.vaadinonkotlin.vaadin8

import eu.vaadinonkotlin.vaadin8.VokSecurity.install
import com.github.vok.framework.VaadinOnKotlin
import eu.vaadinonkotlin.security.AccessRejectedException
import eu.vaadinonkotlin.security.AllowRoles
import eu.vaadinonkotlin.security.loggedInUserResolver
import com.vaadin.navigator.Navigator
import com.vaadin.navigator.View
import com.vaadin.navigator.ViewChangeListener
import com.vaadin.ui.*

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
