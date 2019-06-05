package com.quizplanner.quizPlanner.customvideoview

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
import android.os.Build
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.DisplayMetrics
import android.view.OrientationEventListener
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import java.util.ArrayList

enum class LandscapeOrientation(val value: Int) {
    SENSOR(SCREEN_ORIENTATION_SENSOR_LANDSCAPE),
    DEFAULT(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE),
    REVERSE(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE)
}

enum class PortraitOrientation(val value: Int) {
    SENSOR(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT),
    DEFAULT(SCREEN_ORIENTATION_PORTRAIT),
    REVERSE(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT)
}

/*
 * Handles orientation changes. Updates the VideoView layout params. Hides/shows the toolbar.
 */
class OrientationHelper(context: Context, private val videoView: CustomVideoView) : OrientationEventListener(context) {

    companion object {
        private const val LEFT_LANDSCAPE = 90
        private const val RIGHT_LANDSCAPE = 270
        private const val PORTRAIT = 0
        private const val ROTATE_THRESHOLD = 10
    }

    private var originalWidth: Int = 0
    private var originalHeight: Int = 0
    var isLandscape: Boolean = false
        private set
    private val contentResolver: ContentResolver = context.contentResolver
    // Orientation
    private var landscapeOrientation = LandscapeOrientation.SENSOR
    private var portraitOrientation = PortraitOrientation.DEFAULT
    private var shouldEnterPortrait: Boolean = false

    private val parent: ViewGroup
        get() {
            val window = (videoView.context as Activity).window
            val decorView = window.decorView as ViewGroup
            return decorView.findViewById(android.R.id.content)
        }

    fun activateFullscreen() {
        // Update isLandscape flag
        isLandscape = true

        // Fullscreen active
        videoView.onOrientationChanged()

        // Change the screen orientation to SENSOR_LANDSCAPE
        setOrientation(landscapeOrientation.value)

        UiUtils.hideOtherViews(parent)

        // Save the video player original width and height
        originalWidth = videoView.width
        originalHeight = videoView.height
        updateLayoutParams()

        // Hide the supportToolbar
        toggleToolbarVisibility(false)

        // Hide status bar
        toggleSystemUiVisibility()
    }

    private fun updateLayoutParams() {
        val params = videoView.layoutParams
        val context = videoView.context
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = windowManager.defaultDisplay

        val realMetrics = DisplayMetrics()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            display.getRealMetrics(realMetrics)
        } else {
            display.getMetrics(realMetrics)
        }

        params.width = realMetrics.widthPixels
        params.height = realMetrics.heightPixels
        videoView.layoutParams = params
    }

    fun exitFullscreen() {
        // Update isLandscape flag
        isLandscape = false

        // Update the fullscreen button drawable
        videoView.onOrientationChanged()

        // Change the screen orientation to PORTRAIT
        setOrientation(portraitOrientation.value)

        UiUtils.showOtherViews(parent)

        val params = videoView.layoutParams
        params.width = originalWidth
        params.height = originalHeight
        videoView.layoutParams = params

        toggleToolbarVisibility(true)
        toggleSystemUiVisibility()
    }

    private fun toggleSystemUiVisibility() {
        val activityWindow = (videoView.context as Activity).window
        val decorView = activityWindow.decorView
        var newUiOptions = decorView.systemUiVisibility
        newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_FULLSCREEN
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            newUiOptions = newUiOptions xor View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
        decorView.systemUiVisibility = newUiOptions
    }

    private fun toggleToolbarVisibility(visible: Boolean) {
        if (videoView.context is AppCompatActivity) {
            toggleSupportActionBarVisibility(visible)
        }
        if (videoView.context is Activity) {
            toggleActionBarVisibility(visible)
        }
    }

    private fun toggleActionBarVisibility(visible: Boolean) {
        // Activity action bar
        val actionBar = (videoView.context as Activity).actionBar
        if (actionBar != null) {
            if (visible) {
                actionBar.show()
            } else {
                actionBar.hide()
            }
        }
    }

    private fun toggleSupportActionBarVisibility(visible: Boolean) {
        // AppCompatActivity support action bar
        val supportActionBar = (videoView.context as AppCompatActivity)
                .supportActionBar
        if (supportActionBar != null) {
            if (visible) {
                supportActionBar.show()
            } else {
                supportActionBar.hide()
            }
        }
    }

    private fun setOrientation(orientation: Int) {
        (videoView.context as Activity).requestedOrientation = orientation
    }

    fun shouldHandleOnBackPressed(): Boolean {
        if (isLandscape) {
            // Locks the screen orientation to portrait
            setOrientation(portraitOrientation.value)
            videoView.onOrientationChanged()
            return true
        }

        return false
    }

    fun toggleFullscreen() {
        isLandscape = !isLandscape
        var newOrientation = portraitOrientation.value
        if (isLandscape) {
            newOrientation = landscapeOrientation.value
        }
        setOrientation(newOrientation)
    }

    fun setLandscapeOrientation(landscapeOrientation: LandscapeOrientation) {
        this.landscapeOrientation = landscapeOrientation
    }

    fun setPortraitOrientation(portraitOrientation: PortraitOrientation) {
        this.portraitOrientation = portraitOrientation
    }

    private fun shouldChangeOrientation(a: Int, b: Int): Boolean {
        return a > b - ROTATE_THRESHOLD && a < b + ROTATE_THRESHOLD
    }

    override fun onOrientationChanged(orientation: Int) {
        // If the device's rotation is not enabled do not proceed further with the logic
        if (!isRotationEnabled(contentResolver)) {
            return
        }

        if ((shouldChangeOrientation(orientation, LEFT_LANDSCAPE) || shouldChangeOrientation(orientation, RIGHT_LANDSCAPE)) && !shouldEnterPortrait) {
            shouldEnterPortrait = true
            setOrientation(SCREEN_ORIENTATION_SENSOR_LANDSCAPE)
        }

        if (shouldChangeOrientation(orientation, PORTRAIT) && shouldEnterPortrait) {
            shouldEnterPortrait = false
            setOrientation(SCREEN_ORIENTATION_PORTRAIT)
        }
    }

    /**
     * Check if the device's rotation is enabled
     *
     * @param contentResolver from the app's context
     * @return true or false according to whether the rotation is enabled or disabled
     */
    private fun isRotationEnabled(contentResolver: ContentResolver): Boolean {
        return Settings.System.getInt(contentResolver, Settings.System.ACCELEROMETER_ROTATION,
                0) == 1
    }
}


object UiUtils {

    /**
     * Shows all views except the parent layout
     *
     * @param parentLayout the top layout in the XML file
     */
    fun showOtherViews(parentLayout: ViewGroup) {
        val views = getAllChildViews(parentLayout)
        val size = views.size
        for (i in 0 until size) {
            val view = views[i]
            view.visibility = View.VISIBLE
        }
    }

    /**
     * Hides all views except the parent layout
     *
     * @param parentLayout the top layout in the XML file
     */
    fun hideOtherViews(parentLayout: ViewGroup) {
        val views = getAllChildViews(parentLayout)
        val size = views.size
        for (i in 0 until size) {
            val view = views[i]
            view.visibility = View.GONE
        }
    }

    /**
     * Search recursively through all children of the parent layout and checks their class.
     * If they are ViewGroup classes, continues the recursion,
     * if they are View classes, terminates the recursion
     *
     *
     * Used in [.hideOtherViews] to get all the Views that should be hidden
     * Used in [.showOtherViews] to get all the Views that should be shown
     *
     * @param parentLayout the top layout in XML file
     * @return a list of all non-ViewGroup views from the parent layout except the VideoView,
     * but including Toolbar
     */
    private fun getAllChildViews(parentLayout: View): List<View> {
        if (!shouldCheckChildren(parentLayout)) {
            return listOf(parentLayout)
        }

        val childCount = (parentLayout as ViewGroup).childCount
        val children = ArrayList<View>(childCount)
        for (i in 0 until childCount) {
            val view = parentLayout.getChildAt(i)
            if (shouldCheckChildren(view)) {
                children.addAll(getAllChildViews(view))
            } else {
                if (view !is CustomVideoView) {
                    children.add(view)
                }
            }
        }
        return children
    }

    /**
     * Check if a view has children to iterate
     *
     *
     * Used in [.getAllChildViews] as a terminating case
     *
     * @param view the [View] that should be checked
     * @return true if the View is a ViewGroup, but not FullscreenVideoView or Toolbar
     */
    private fun shouldCheckChildren(view: View): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            view is ViewGroup &&
                    view !is Toolbar &&
                    view !is android.widget.Toolbar &&
                    view !is CustomVideoView
        } else {
            view is ViewGroup &&
                    view !is Toolbar &&
                    view !is CustomVideoView
        }
    }
}