package uk.co.appoly.psc_support.components

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import io.sentry.core.Sentry

import uk.co.appoly.psc_support.R

abstract class BaseFragment : Fragment() {

    protected var layoutID = R.layout.fragment_base
    protected var mBindingRoot: ViewDataBinding? = null

    lateinit var baseView: View

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        baseView = inflater.inflate(layoutID, container, false)

        Sentry.addBreadcrumb("${fragmentTag()}::onCreateView()")

        mBindingRoot = DataBindingUtil.bind(baseView)
        mBindingRoot!!.lifecycleOwner = this

        initViewModels()
        initBinding()
        initObservers()

        return baseView
    }

    protected fun navigateTo(navigationID: Int) {
        Sentry.addBreadcrumb("${fragmentTag()}::navigateTo($navigationID)")
        NavHostFragment.findNavController(this).navigate(navigationID)
    }

    protected fun navigateTo(action: NavDirections) {
        Sentry.addBreadcrumb("${fragmentTag()}::navigateTo($action)")
        NavHostFragment.findNavController(this).navigate(action)
    }

    // This method links a view model provider to a nav graph (instead of an activity) so the view model is shared among all fragments in the graph
    protected fun getViewModelProviderFor(graph: Int): ViewModelProvider {
        return ViewModelProvider(NavHostFragment.findNavController(this).getViewModelStoreOwner(graph), ViewModelProvider.AndroidViewModelFactory(activity?.application!!))
    }

    fun popBackstack(): Boolean {
        Sentry.addBreadcrumb("${fragmentTag()}::popBackstack()")
        return NavHostFragment.findNavController(this).popBackStack()
    }

    protected abstract fun initViewModels()
    protected abstract fun initBinding()
    protected abstract fun initObservers()
    protected abstract fun fragmentTag(): String

    fun makeSnack(msg: String, duration: Int) {
        Snackbar.make(baseView, msg, duration).show()
    }
}