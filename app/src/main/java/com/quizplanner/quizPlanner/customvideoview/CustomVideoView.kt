package com.quizplanner.quizPlanner.customvideoview

import android.content.Context
import android.content.res.Configuration
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.R
import java.io.IOException

class CustomVideoView : FrameLayout {
    private var surfaceView: VideoSurfaceView? = null
    private var surfaceHolder: SurfaceHolder? = null
    private var progressBar: ProgressBar? = null
    private var controller: VideoControllerView? = null
    private var videoMediaPlayer: VideoMediaPlayer? = null
    private var isMediaPlayerPrepared: Boolean = false
    private var orientationHelper: OrientationHelper? = null
    private var surfaceHolderCallback: SurfaceHolder.Callback? = null
    private var isPaused: Boolean = false
    private var previousOrientation: Int = 0

    constructor(context: Context) : super(context) {}

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(attrs)
    }

    constructor(context: Context, attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(attrs)
    }

    private fun init(attrs: AttributeSet?) {
        findChildViews()
        // Skip this init rows - needed when changing FullscreenVideoView properties in XML
        if (!isInEditMode) {
            videoMediaPlayer = VideoMediaPlayer(this)
            orientationHelper = OrientationHelper(context, this)
            orientationHelper!!.enable()
        }
        setupSurfaceHolder()
        if (controller != null) {
            controller!!.init(orientationHelper!!, videoMediaPlayer!!, attrs!!)
        }
        setupProgressBarColor()
        isFocusableInTouchMode = true
        requestFocus()
        initOnBackPressedListener()
        // Setup onTouch listener
        setOnTouchListener { view, event ->
            view.performClick()
            if (controller != null) {
                controller!!.show()
            }
            false
        }
    }

    private fun setupSurfaceHolder() {
        if (surfaceView != null) {
            surfaceHolderCallback = object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    if (videoMediaPlayer != null) {
                        videoMediaPlayer!!.setDisplay(surfaceHolder)
                    }
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    if (videoMediaPlayer != null && isMediaPlayerPrepared) {
                        videoMediaPlayer!!.pause()
                    }
                }
            }
            surfaceHolder = surfaceView!!.holder
            surfaceHolder!!.addCallback(surfaceHolderCallback)
        }
    }

    private fun findChildViews() {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.fullscreen_video_view, this, true)
        surfaceView = findViewById(R.id.surface_view)
        progressBar = findViewById(R.id.progress_bar)
        controller = findViewById(R.id.video_controller)
    }

    private fun initOnBackPressedListener() {
        setOnKeyListener { v, keyCode, event ->
            (event.action == KeyEvent.ACTION_UP
                    && keyCode == KeyEvent.KEYCODE_BACK
                    && orientationHelper != null && orientationHelper!!.shouldHandleOnBackPressed())
        }
    }

    fun videoUrl(context: Context, videoUrl: Uri): Builder {
        return Builder(this, controller!!, orientationHelper!!, videoMediaPlayer!!).videoUrl(context, videoUrl)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (orientationHelper == null) {
            return
        }

        // Avoid calling onConfigurationChanged twice
        if (previousOrientation == newConfig.orientation) {
            return
        }
        previousOrientation = newConfig.orientation

        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            orientationHelper!!.activateFullscreen()
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            orientationHelper!!.exitFullscreen()
        }
    }

    override fun onDetachedFromWindow() {
        handleOnDetach()
        super.onDetachedFromWindow()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        isPaused = visibility != View.VISIBLE
    }

    private fun handleOnDetach() {
        if (controller != null) {
            controller!!.onDetach()
        }

        // Disable and null the OrientationEventListener
        if (orientationHelper != null) {
            orientationHelper!!.disable()
        }

        if (videoMediaPlayer != null) {
            videoMediaPlayer!!.onDetach()
        }

        if (surfaceHolder != null) {
            surfaceHolder!!.removeCallback(surfaceHolderCallback)
            surfaceHolder!!.surface.release()
        }

        if (surfaceView != null) {
            surfaceView!!.invalidate()
            surfaceView!!.destroyDrawingCache()
        }

        controller = null
        orientationHelper = null
        videoMediaPlayer = null
        surfaceHolder = null
        surfaceView = null
        progressBar = null

        setOnKeyListener(null)
        setOnTouchListener(null)
    }

    fun setupMediaPlayer(videoPath: String) {
        showProgress()
        try {
            if (videoMediaPlayer != null) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    videoMediaPlayer!!.setAudioAttributes(audioAttributes)
                } else {
                    videoMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }

                videoMediaPlayer!!.setDataSource(videoPath)
                videoMediaPlayer!!.setOnErrorListener { mp, what, extra ->
                    QuizPlanner.log("VideoView", "error what = $what, extra = $extra")

                    return@setOnErrorListener false
                }

                videoMediaPlayer!!.setOnPreparedListener { mediaPlayer ->
                    QuizPlanner.log("VideoView", "OnPrepared")

                    hideProgress()
                    // Get the dimensions of the video
                    val videoWidth = videoMediaPlayer!!.videoWidth
                    val videoHeight = videoMediaPlayer!!.videoHeight
                    if (surfaceView != null) {
                        surfaceView!!.updateLayoutParams(videoWidth, videoHeight)
                    }
                    if (!isPaused) {
                        isMediaPlayerPrepared = true
                        // Start media player if auto start is enabled
                        if (mediaPlayer != null && videoMediaPlayer!!.isAutoStartEnabled) {
                            mediaPlayer.start()
                        }
                    }
                }
                videoMediaPlayer!!.prepareAsync()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
    fun setupMediaPlayer(context: Context, videoUrl: Uri) {
        showProgress()
        try {
            if (videoMediaPlayer != null) {

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    val audioAttributes = AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                            .build()
                    videoMediaPlayer!!.setAudioAttributes(audioAttributes)
                } else {
                    videoMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
                }

                videoMediaPlayer!!.setDataSource(context, videoUrl)
                videoMediaPlayer!!.setOnErrorListener { mp, what, extra ->
                    QuizPlanner.log("VideoView", "error what = $what, extra = $extra")

                    return@setOnErrorListener false
                }

                videoMediaPlayer!!.setOnPreparedListener { mediaPlayer ->
                    QuizPlanner.log("VideoView", "OnPrepared")

                    hideProgress()
                    // Get the dimensions of the video
                    val videoWidth = videoMediaPlayer!!.videoWidth
                    val videoHeight = videoMediaPlayer!!.videoHeight
                    if (surfaceView != null) {
                        surfaceView!!.updateLayoutParams(videoWidth, videoHeight)
                    }
                    if (!isPaused) {
                        isMediaPlayerPrepared = true
                        // Start media player if auto start is enabled
                        if (mediaPlayer != null && videoMediaPlayer!!.isAutoStartEnabled) {
                            mediaPlayer.start()
                        }
                    }
                }
                videoMediaPlayer!!.prepareAsync()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }

    private fun setupProgressBarColor() {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime)
        if (progressBar != null) {
            progressBar!!.animate().duration = shortAnimTime.toLong()
        }
    }

    private fun hideProgress() {
        if (progressBar != null) {
            progressBar!!.visibility = View.INVISIBLE
        }
    }

    private fun showProgress() {
        if (progressBar != null) {
            progressBar!!.visibility = View.VISIBLE
        }
    }

    fun toggleFullscreen() {
        if (orientationHelper != null) {
            orientationHelper!!.toggleFullscreen()
        }
    }

    fun enableAutoStart() {
        if (videoMediaPlayer != null) {
            videoMediaPlayer!!.enableAutoStart()
        }
    }

    fun onOrientationChanged() {
        // Update the fullscreen button drawable
        if (controller != null) {
            controller!!.updateFullScreenDrawable()
        }
        if (surfaceView != null && videoMediaPlayer != null) {
            surfaceView!!.updateLayoutParams(videoMediaPlayer!!.videoWidth,
                    videoMediaPlayer!!.videoHeight)
        }
    }
}
