package PACKAGE.NAME

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavDirections
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.*

import PACKAGE.NAME.R

abstract class BaseFragment : Fragment() {

    companion object {
        private var shouldPopFullBackstack = false
    }

    protected var layoutID = -1
    protected var mBindingRoot: ViewDataBinding? = null

    private lateinit var baseView: View

    private var imageUri : Uri? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        baseView = inflater.inflate(layoutID, container, false)

        mBindingRoot = DataBindingUtil.bind(baseView)
        mBindingRoot!!.lifecycleOwner = this

        initViewModels()
        initBinding()
        initObservers()

        return baseView
    }

    override fun onResume() {
        if (shouldPopFullBackstack) shouldPopFullBackstack = popBackstack()
        setAsCurrentActiveFragment()
        super.onResume()
    }

    protected fun navigateTo(navigationID: Int) {
        NavHostFragment.findNavController(this).navigate(navigationID)
    }

    protected fun navigateTo(action: NavDirections) {
        NavHostFragment.findNavController(this).navigate(action)
    }

    protected fun navigateToUrl(url: String?) {
        url?.let {
            val i = Intent(Intent.ACTION_VIEW)
            i.data = Uri.parse(it)
            startActivity(i)
        }
    }

    protected fun hideKeyboard() {
        (context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    // This method links a view model provider to a nav graph (instead of an activity) so the view model is shared among all fragments in the graph
    protected fun getGraphViewModelProvider(): ViewModelProvider {
        val graphID = NavHostFragment.findNavController(this).graph.id
        return ViewModelProvider(NavHostFragment.findNavController(this).getViewModelStoreOwner(graphID), ViewModelProvider.AndroidViewModelFactory(activity?.application!!))
    }

    fun popBackstack(): Boolean {
        return NavHostFragment.findNavController(this).popBackStack()
    }

    fun popFullBackstack() {
        shouldPopFullBackstack = true
        popBackstack()
    }

    private fun setAsCurrentActiveFragment() {
        val thisActivity = activity
        if (thisActivity is DashboardActivity) thisActivity.currentActiveFragment = this
    }

    protected abstract fun initViewModels()
    protected abstract fun initBinding()
    protected abstract fun initObservers()
    protected abstract fun fragmentTag(): String

    fun makeSnack(msg: String, duration: Int) {
        hideKeyboard()
        Snackbar.make(baseView, msg, duration).show()
    }

    protected fun selectImageDialog() {
        AlertDialog.Builder(context, R.style.AlertDialogTheme)
            .setMessage("How would you like to add an image?")
            .setNegativeButton("Gallery") { _, _ -> checkGalleryPermission() }
            .setPositiveButton("Camera") { _, _ -> checkCameraPermission() }
            .show()
    }

    private fun checkCameraPermission() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) != PackageManager.PERMISSION_GRANTED
            ) {requestPermissions(
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSIONS
            )
            } else selectImageFromCamera()
        }
    }

    private fun checkGalleryPermission() {
        context?.let { context ->
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    ),
                    REQUEST_READ_WRITE_PERMISSIONS
                )
            } else selectImageFromGallery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) selectImageFromCamera()
                else makeSnack("Unable to take picture without camera permissions", Snackbar.LENGTH_SHORT)
            }
            REQUEST_READ_WRITE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) selectImageFromGallery()
                else makeSnack("Unable to access gallery without file access permissions", Snackbar.LENGTH_SHORT)
            }
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GET_IMAGE_FROM_GALLERY)
    }

    private fun selectImageFromCamera() {
        context?.let { context ->
            val outputImage = File(activity?.externalCacheDir, "${UUID.randomUUID()}.jpg")
            if (outputImage.exists()) outputImage.delete()
            outputImage.createNewFile()

            imageUri = FileProvider.getUriForFile(
                context,
                BuildConfig.APPLICATION_ID + ".provider",
                outputImage
            )

            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            startActivityForResult(intent, GET_IMAGE_FROM_CAMERA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == GET_IMAGE_FROM_GALLERY) {
                receivedImageFromGallery(Uri.parse(data?.data.toString()))
            }
            if (requestCode == GET_IMAGE_FROM_CAMERA) {
                receivedImageFromCamera(Uri.parse(imageUri.toString()))
            }
        }
    }

    protected open fun receivedImageFromGallery(uri: Uri) {
        Log.e("No Function Override", "receivedImageFromGallery(uri: Uri) not overridden in ${fragmentTag()}")
    }

    protected open fun receivedImageFromCamera(uri: Uri) {
        Log.e("No Function Override", "receivedImageFromCamera(uri: Uri) not overridden in ${fragmentTag()}")
    }
}