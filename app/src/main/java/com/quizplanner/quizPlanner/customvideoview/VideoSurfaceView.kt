package com.quizplanner.quizPlanner.customvideoview

import android.content.Context
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.SurfaceView
import android.widget.FrameLayout

internal class VideoSurfaceView : SurfaceView {

    private var previousHeight: Int = 0
    private var previousWidth: Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {}

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {}

    fun updateLayoutParams(videoWidth: Int, videoHeight: Int) {
        resetLayoutParams()
        previousHeight = layoutParams.height
        previousWidth = layoutParams.width
        // Get the Display Metrics
        val displayMetrics = context.resources.displayMetrics
        // Get the width of the screen
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        // Get the SurfaceView layout parameters
        val lp = layoutParams as FrameLayout.LayoutParams
        if (videoHeight.toFloat() / screenHeight > videoWidth.toFloat() / screenWidth) {
            lp.height = screenHeight
            // Set the width of the SurfaceView to match the aspect ratio of the video
            // be sure to cast these as floats otherwise the calculation will likely be 0
            lp.width = (videoWidth.toFloat() / videoHeight * screenHeight).toInt()
        } else {
            // Set the width of the SurfaceView to the width of the screen
            lp.width = screenWidth
            // Set the height of the SurfaceView to match the aspect ratio of the video
            // be sure to cast these as floats otherwise the calculation will likely be 0
            lp.height = (videoHeight.toFloat() / videoWidth * screenWidth).toInt()
        }
        // Change the gravity to center
        lp.gravity = Gravity.CENTER
        // Commit the layout parameters
        layoutParams = lp
    }

    private fun resetLayoutParams() {
        val layoutParams = layoutParams as FrameLayout.LayoutParams
        layoutParams.height = previousHeight
        layoutParams.width = previousWidth
        setLayoutParams(layoutParams)
    }
}
