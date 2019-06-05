package com.quizplanner.quizPlanner.customvideoview

/*
 * Copyright (C) 2006 The Android Open Source Project
 * Modifications Copyright (C) 2017 Dev Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.TypedArray
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Handler
import android.os.Message
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MotionEvent
import android.view.View
import android.view.View.OnClickListener
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import com.quizplanner.quizPlanner.R
import java.lang.ref.WeakReference
import java.util.*

class PlaybackSpeedOptions {

    private val speeds = ArrayList(listOf(1f))

    fun addSpeeds(speeds: ArrayList<Float>): PlaybackSpeedOptions {
        this.speeds.addAll(speeds)
        this.speeds.sort()

        if (containsIllegalNumbers()) {
            throw IllegalArgumentException("The speeds array must contain only numbers between 0 and 4!")
        }
        return this
    }

    private fun containsIllegalNumbers(): Boolean {
        val size = speeds.size
        for (i in 0 until size) {
            if (speeds[i] < 0 || speeds[i] > 4) {
                return true
            }
        }
        return false
    }

    fun getSpeeds(): ArrayList<Float> {
        return ArrayList(HashSet(speeds))
    }
}

internal class PlaybackSpeedPopupMenu(context: Context, anchor: View) : android.support.v7.widget.PopupMenu(context, anchor) {

    private var values = ArrayList(Arrays.asList(0.25f, 0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f))

    init {
        addMenuButtons()
    }

    fun setOnSpeedSelectedListener(listener: OnSpeedSelectedListener) {
        setOnMenuItemClickListener { item ->
            val speed = values[item.itemId]
            val text = String.format(Locale.getDefault(), "%.2f", speed) + "x"
            listener.onSpeedSelected(speed, text)
            true
        }
    }

    fun setPlaybackSpeedOptions(playbackSpeedOptions: PlaybackSpeedOptions) {
        values.clear()
        values = playbackSpeedOptions.getSpeeds()
        removeMenuButtons()
        addMenuButtons()
    }

    private fun removeMenuButtons() {
        menu.removeGroup(0)
    }

    private fun addMenuButtons() {
        val size = values.size
        var id = -1
        for (i in 0 until size) {
            id++
            val title = String.format(Locale.getDefault(), "%.2f", values[i]) + "x"
            menu.add(0, id, Menu.NONE, title)
        }
    }

    internal interface OnSpeedSelectedListener {
        fun onSpeedSelected(speed: Float, text: String)
    }
}

internal object Constants {
    const val ONE_HOUR_SECONDS = 3600
    const val ONE_MINUTE_SECONDS = 60
    const val ONE_SECOND_MILLISECONDS = 1000
    const val ONE_HOUR_MILLISECONDS = 3600000
    const val FAST_FORWARD_DURATION = 15000
    const val REWIND_DURATION = 5000
    const val ONE_MILLISECOND = 1000L
}

internal class VideoControllerView : FrameLayout {

    companion object {
        private const val DEFAULT_TIMEOUT = 3000
        private const val FADE_OUT = 1
        private const val SHOW_PROGRESS = 2

        private fun stringForTime(timeMs: Int): CharSequence {
            val totalSeconds = timeMs / Constants.ONE_SECOND_MILLISECONDS
            val seconds = totalSeconds % Constants.ONE_MINUTE_SECONDS
            val minutes = totalSeconds / Constants.ONE_MINUTE_SECONDS % Constants.ONE_MINUTE_SECONDS
            val hours = totalSeconds / Constants.ONE_HOUR_SECONDS
            return String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        }
    }

    private var videoMediaPlayer: VideoMediaPlayer? = null
    private var endTime: TextView? = null
    private var currentTime: TextView? = null
    private var isDragging: Boolean = false
    private var popupMenu: PlaybackSpeedPopupMenu? = null
    private var msgHandler: Handler? = MessageHandler(this)
    private var progress: SeekBar? = null
    private var startPauseButton: ImageButton? = null
    private var ffwdButton: ImageButton? = null
    private var rewButton: ImageButton? = null
    private var playbackSpeedButton: TextView? = null
    private var pauseListener: OnClickListener? = OnClickListener {
        doPauseResume()
        show(DEFAULT_TIMEOUT)
    }
    private var fullscreenListener: OnClickListener? = OnClickListener {
        doToggleFullscreen()
        show(DEFAULT_TIMEOUT)
    }
    // There are two scenarios that can trigger the SeekBar listener to trigger:
    //
    // The first is the user using the TouchPad to adjust the position of the
    // SeekBar's thumb. In this case onStartTrackingTouch is called followed by
    // a number of onProgressChanged notifications, concluded by onStopTrackingTouch.
    // We're setting the field "isDragging" to true for the duration of the dragging
    // session to avoid jumps in the position in case of ongoing playback.
    //
    // The second scenario involves the user operating the scroll ball, in this
    // case there WON'T BE onStartTrackingTouch/onStopTrackingTouch notifications,
    // we will simply apply the updated position without suspending regular updates.
    private var seekListener: SeekBar.OnSeekBarChangeListener? = OnSeekChangeListener()
    private var rewListener: OnClickListener? = OnClickListener {
        if (videoMediaPlayer == null) {
            return@OnClickListener
        }

        var pos = videoMediaPlayer!!.currentPosition
        pos -= rewindDuration // milliseconds
        videoMediaPlayer!!.seekTo(pos)
        setProgress()

        show(DEFAULT_TIMEOUT)
    }
    private var ffwdListener: OnClickListener? = OnClickListener {
        if (videoMediaPlayer == null) {
            return@OnClickListener
        }

        var pos = videoMediaPlayer!!.currentPosition
        pos += fastForwardDuration // milliseconds
        videoMediaPlayer!!.seekTo(pos)
        setProgress()

        show(DEFAULT_TIMEOUT)
    }
    private var playbackSpeedListener: OnClickListener? = OnClickListener {
        // Inflate the PopupMenu
        //            popupMenu.getMenuInflater()
        //                    .inflate(R.menu.playback_speed_popup_menu, popupMenu.getMenu());

        popupMenu!!.setOnSpeedSelectedListener(object : PlaybackSpeedPopupMenu.OnSpeedSelectedListener {
            override fun onSpeedSelected(speed: Float, text: String) {
                // Update the Playback Speed Drawable according to the clicked menu item
                buttonHelper!!.updatePlaybackSpeedText(text)
                // Change the Playback Speed of the VideoMediaPlayer
                videoMediaPlayer!!.changePlaybackSpeed(speed)
                // Hide the VideoControllerView
                hide()
            }
        })

        popupMenu!!.setOnDismissListener { show() }

        // Show the PopupMenu
        popupMenu!!.show()

        // Show the VideoControllerView and until hide is called
        show(0)
    }

    private var buttonHelper: ButtonHelper? = null
    private var progressBarColor = Color.WHITE

    private var fastForwardDuration = Constants.FAST_FORWARD_DURATION
    private var rewindDuration = Constants.REWIND_DURATION

    private val isShowing: Boolean
        get() = visibility == View.VISIBLE

    constructor(context: Context) : super(context) {
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.video_controller, this, true)
        initControllerView()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {

        val layoutInflater = LayoutInflater.from(getContext())
        layoutInflater.inflate(R.layout.video_controller, this, true)
        initControllerView()
        setupXmlAttributes(attrs)
    }

    private fun setupXmlAttributes(attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs,
                R.styleable.VideoControllerView, 0, 0)
        buttonHelper!!.setupDrawables(typedArray)
        setupProgressBar(typedArray)
        // Recycle the TypedArray
        typedArray.recycle()
    }

    private fun setupProgressBar(a: TypedArray) {
        val color = a.getColor(R.styleable.VideoControllerView_progress_color, 0)
        if (color != 0) {
            // Set the default color
            progressBarColor = color
        }
        progress!!.progressDrawable.setColorFilter(progressBarColor, PorterDuff.Mode.SRC_IN)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            progress!!.thumb.setColorFilter(progressBarColor, PorterDuff.Mode.SRC_IN)
        }
    }

    private fun initControllerView() {
        if (!isInEditMode) {
            visibility = View.INVISIBLE
        }

        startPauseButton = findViewById(R.id.start_pause_media_button)
        if (startPauseButton != null) {
            startPauseButton!!.requestFocus()
            startPauseButton!!.setOnClickListener(pauseListener)
        }

        val fullscreenButton = findViewById<ImageButton>(R.id.fullscreen_media_button)
        if (fullscreenButton != null) {
            fullscreenButton.requestFocus()
            fullscreenButton.setOnClickListener(fullscreenListener)
        }

        ffwdButton = findViewById(R.id.forward_media_button)
        if (ffwdButton != null) {
            ffwdButton!!.setOnClickListener(ffwdListener)
        }

        rewButton = findViewById(R.id.rewind_media_button)
        if (rewButton != null) {
            rewButton!!.setOnClickListener(rewListener)
        }

        playbackSpeedButton = findViewById(R.id.playback_speed_button)
        if (playbackSpeedButton != null) {
            playbackSpeedButton!!.setOnClickListener(playbackSpeedListener)
        }

        buttonHelper = ButtonHelper(context, startPauseButton, ffwdButton, rewButton, fullscreenButton, playbackSpeedButton)

        progress = findViewById(R.id.progress_seek_bar)
        if (progress != null) {
            progress!!.progressDrawable.setColorFilter(progressBarColor, PorterDuff.Mode.SRC_IN)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                progress!!.thumb.setColorFilter(progressBarColor, PorterDuff.Mode.SRC_IN)
            }
            progress!!.setOnSeekBarChangeListener(seekListener)
            progress!!.max = 1000
        }

        endTime = findViewById(R.id.time)
        currentTime = findViewById(R.id.time_current)
    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 3 seconds of inactivity.
     */
    fun show() {
        show(DEFAULT_TIMEOUT)
    }

    /**
     * Change the buttons visibility according to the flags in [.videoMediaPlayer].
     */
    private fun setupButtonsVisibility() {
        if (videoMediaPlayer == null) {
            return
        }

        try {
            if (startPauseButton != null && !videoMediaPlayer!!.canPause()) {
                startPauseButton!!.isEnabled = false
                startPauseButton!!.visibility = View.INVISIBLE
            }
            if (rewButton != null && !videoMediaPlayer!!.showSeekBackwardButton()) {
                rewButton!!.isEnabled = false
                rewButton!!.visibility = View.INVISIBLE
            }
            if (ffwdButton != null && !videoMediaPlayer!!.showSeekForwardButton()) {
                ffwdButton!!.isEnabled = false
                ffwdButton!!.visibility = View.INVISIBLE
            }
            if (playbackSpeedButton != null && !videoMediaPlayer!!.showPlaybackSpeedButton()) {
                playbackSpeedButton!!.isEnabled = false
                playbackSpeedButton!!.visibility = View.INVISIBLE
            }
        } catch (ex: IncompatibleClassChangeError) {
            // We were given an old version of the interface, that doesn't have
            // the canPause/canSeekXYZ methods. This is OK, it just means we
            // assume the media can be paused and seeked, and so we don't disable
            // the buttons.
            ex.printStackTrace()
        }

    }

    /**
     * Show the controller on screen. It will go away
     * automatically after 'timeout' milliseconds of inactivity.
     *
     * @param timeout The timeout in milliseconds. Use 0 to show
     * the controller until hide() is called.
     */
    private fun show(timeout: Int) {
        if (!isShowing) {
            setProgress()
            if (startPauseButton != null) {
                startPauseButton!!.requestFocus()
            }
            setupButtonsVisibility()
            visibility = View.VISIBLE
        }

        buttonHelper!!.updatePausePlay()
        buttonHelper!!.updateFullScreenDrawable()

        // Cause the progress bar to be updated even if it's showing.
        // This happens, for example, if we're
        // paused with the progress bar showing the user hits play.
        if (msgHandler == null) {
            return
        }

        msgHandler!!.sendEmptyMessage(SHOW_PROGRESS)

        val msg = msgHandler!!.obtainMessage(FADE_OUT)
        if (timeout != 0) {
            msgHandler!!.removeMessages(FADE_OUT)
            msgHandler!!.sendMessageDelayed(msg, timeout.toLong())
        } else {
            msgHandler!!.removeMessages(FADE_OUT)
        }
    }

    fun updateFullScreenDrawable() {
        buttonHelper!!.updateFullScreenDrawable()
    }

    /**
     * Remove the controller from the screen.
     */
    private fun hide() {
        try {
            visibility = View.INVISIBLE
            if (msgHandler != null) {
                msgHandler!!.removeMessages(SHOW_PROGRESS)
            }
        } catch (ignored: IllegalArgumentException) {
            Log.w("MediaController", "already removed")
        }

    }

    private fun setProgress(): Int {
        if (videoMediaPlayer == null || isDragging) {
            return 0
        }

        val position = videoMediaPlayer!!.currentPosition
        val duration = videoMediaPlayer!!.duration
        if (progress != null) {
            if (duration > 0) {
                // Use long to avoid overflow
                val pos = Constants.ONE_MILLISECOND * position / duration
                progress!!.progress = pos.toInt()
            }
            val percent = videoMediaPlayer!!.bufferPercentage
            progress!!.secondaryProgress = percent * 10
        }

        if (endTime != null) {
            endTime!!.text = stringForTime(duration)
        }

        if (currentTime != null) {
            currentTime!!.text = stringForTime(position)
        }

        return position
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        performClick()
        show(DEFAULT_TIMEOUT)
        return true
    }

    private fun doPauseResume() {
        if (videoMediaPlayer == null) {
            return
        }
        videoMediaPlayer!!.onPauseResume()
        buttonHelper!!.updatePausePlay()
    }

    private fun doToggleFullscreen() {
        if (videoMediaPlayer == null) {
            return
        }

        videoMediaPlayer!!.toggleFullScreen()
    }

    override fun setEnabled(enabled: Boolean) {
        if (startPauseButton != null) {
            startPauseButton!!.isEnabled = enabled
        }
        if (ffwdButton != null) {
            ffwdButton!!.isEnabled = enabled
        }
        if (rewButton != null) {
            rewButton!!.isEnabled = enabled
        }
        if (playbackSpeedButton != null) {
            playbackSpeedButton!!.isEnabled = enabled
        }
        if (progress != null) {
            progress!!.isEnabled = enabled
        }
        setupButtonsVisibility()
        super.setEnabled(enabled)
    }

    fun onDetach() {
        ffwdListener = null
        fullscreenListener = null
        pauseListener = null
        rewListener = null
        seekListener = null
        playbackSpeedListener = null
        msgHandler = null
        videoMediaPlayer = null
    }

    fun setEnterFullscreenDrawable(enterFullscreenDrawable: Drawable) {
        buttonHelper!!.setEnterFullscreenDrawable(enterFullscreenDrawable)
    }

    fun setExitFullscreenDrawable(exitFullscreenDrawable: Drawable) {
        buttonHelper!!.setExitFullscreenDrawable(exitFullscreenDrawable)
    }

    fun setProgressBarColor(progressBarColor: Int) {
        this.progressBarColor = ContextCompat.getColor(context, progressBarColor)
    }

    fun setPlayDrawable(playDrawable: Drawable) {
        buttonHelper!!.setPlayDrawable(playDrawable)
    }

    fun setPauseDrawable(pauseDrawable: Drawable) {
        buttonHelper!!.setPauseDrawable(pauseDrawable)
    }

    fun setFastForwardDuration(fastForwardDuration: Int) {
        this.fastForwardDuration = fastForwardDuration * 1000
    }

    fun setRewindDuration(rewindDuration: Int) {
        this.rewindDuration = rewindDuration * 1000
    }

    fun setFastForwardDrawable(fastForwardDrawable: Drawable) {
        buttonHelper!!.setFastForwardDrawable(fastForwardDrawable)
    }

    fun setRewindDrawable(rewindDrawable: Drawable) {
        buttonHelper!!.setRewindDrawable(rewindDrawable)
    }

    fun setPlaybackSpeedOptions(playbackSpeedOptions: PlaybackSpeedOptions) {
        popupMenu!!.setPlaybackSpeedOptions(playbackSpeedOptions)
    }

    fun init(orientationHelper: OrientationHelper, videoMediaPlayer: VideoMediaPlayer, attrs: AttributeSet) {
        setupXmlAttributes(attrs)
        this.videoMediaPlayer = videoMediaPlayer

        buttonHelper!!.setOrientationHelper(orientationHelper)
        buttonHelper!!.setVideoMediaPlayer(videoMediaPlayer)
        buttonHelper!!.updatePausePlay()
        buttonHelper!!.updateFullScreenDrawable()
        buttonHelper!!.updateFastForwardDrawable()
        buttonHelper!!.updateRewindDrawable()

        // Initialize the PopupMenu
        popupMenu = PlaybackSpeedPopupMenu(context, playbackSpeedButton!!)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            viewTreeObserver.addOnWindowFocusChangeListener {
                if (orientationHelper.isLandscape) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        (context as Activity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    } else {
                        (context as Activity).window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
                    }
                }
            }
        }
    }

    private class MessageHandler internal constructor(view: VideoControllerView) : Handler() {
        private val view: WeakReference<VideoControllerView> = WeakReference(view)

        override fun handleMessage(msg: Message) {
            val view = this.view.get()
            if (view?.videoMediaPlayer == null) {
                return
            }

            if (msg.what == FADE_OUT) {
                view.hide()
            } else { // SHOW_PROGRESS
                val position = view.setProgress()
                if (!view.isDragging && view.isShowing && view.videoMediaPlayer!!.isPlaying) {
                    val message = obtainMessage(SHOW_PROGRESS)
                    sendMessageDelayed(message, (1000 - position % 1000).toLong())
                }
            }
        }
    }

    private inner class OnSeekChangeListener : SeekBar.OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            show(Constants.ONE_HOUR_MILLISECONDS)

            isDragging = true

            // By removing these pending progress messages we make sure
            // that a) we won't update the progress while the user adjusts
            // the seekbar and b) once the user is done dragging the thumb
            // we will post one of these messages to the queue again and
            // this ensures that there will be exactly one message queued up.
            if (msgHandler != null) {
                msgHandler!!.removeMessages(SHOW_PROGRESS)
            }
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (videoMediaPlayer == null) {
                return
            }

            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return
            }

            val duration = videoMediaPlayer!!.duration.toLong()
            val newPosition = duration * progress / Constants.ONE_MILLISECOND
            videoMediaPlayer!!.seekTo(newPosition.toInt())
            if (currentTime != null) {
                currentTime!!.text = stringForTime(newPosition.toInt())
            }
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            isDragging = false
            setProgress()
            buttonHelper!!.updatePausePlay()
            show(DEFAULT_TIMEOUT)

            // Ensure that progress is properly updated in the future,
            // the call to show() does not guarantee this because it is a
            // no-op if we are already showing.
            if (msgHandler != null) {
                msgHandler!!.sendEmptyMessage(SHOW_PROGRESS)
            }
        }
    }
}

internal class ButtonHelper(context: Context,
                            startPauseButton: ImageButton?,
                            ffwdButton: ImageButton?,
                            rewButton: ImageButton?,
                            fullscreenButton: ImageButton,
                            playbackSpeedButton: TextView?) {

    // Drawables for the buttons of the controller
    private var exitFullscreenDrawable: Drawable? = null
    private var enterFullscreenDrawable: Drawable? = null
    private var playDrawable: Drawable? = null
    private var pauseDrawable: Drawable? = null
    private var fastForwardDrawable: Drawable? = null
    private var rewindDrawable: Drawable? = null

    // Buttons
    private val startPauseButton: WeakReference<ImageButton?>
    private val ffwdButton: WeakReference<ImageButton?>
    private val rewButton: WeakReference<ImageButton?>
    private val fullscreenButton: WeakReference<ImageButton?>
    private val playbackSpeedButton: WeakReference<TextView?>

    private var orientationHelper: WeakReference<OrientationHelper>? = null
    private var videoMediaPlayer: WeakReference<VideoMediaPlayer>? = null

    init {
        this.exitFullscreenDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fullscreen_exit_white_48dp)
        this.enterFullscreenDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fullscreen_white_48dp)
        this.playDrawable = ContextCompat.getDrawable(context, R.drawable.ic_play_arrow_white_48dp)
        this.pauseDrawable = ContextCompat.getDrawable(context, R.drawable.ic_pause_white_48dp)
        this.fastForwardDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fast_forward_white_48dp)
        this.rewindDrawable = ContextCompat.getDrawable(context, R.drawable.ic_fast_rewind_white_48dp)

        this.startPauseButton = WeakReference(startPauseButton)
        this.ffwdButton = WeakReference(ffwdButton)
        this.rewButton = WeakReference(rewButton)
        this.fullscreenButton = WeakReference(fullscreenButton)
        this.playbackSpeedButton = WeakReference(playbackSpeedButton)
    }

    fun setupDrawables(typedArray: TypedArray) {
        setupPlayPauseButton(typedArray)
        setupFullscreenButton(typedArray)
        setupRewindButton(typedArray)
        setupFastForwardButton(typedArray)
    }

    private fun setupFastForwardButton(a: TypedArray) {
        val drawable = a.getDrawable(R.styleable.VideoControllerView_ffwd_drawable)
        if (drawable != null) {
            fastForwardDrawable = drawable
        }
        ffwdButton.get()!!.setImageDrawable(fastForwardDrawable)
    }

    private fun setupRewindButton(a: TypedArray) {
        val drawable = a.getDrawable(R.styleable.VideoControllerView_rew_drawable)
        if (drawable != null) {
            rewindDrawable = drawable
        }
        rewButton.get()!!.setImageDrawable(rewindDrawable)
    }

    private fun setupFullscreenButton(a: TypedArray) {
        val enterDrawable = a.getDrawable(R.styleable.VideoControllerView_enter_fullscreen_drawable)
        if (enterDrawable != null) {
            enterFullscreenDrawable = enterDrawable
        }
        fullscreenButton.get()!!.setImageDrawable(enterFullscreenDrawable)

        val exitDrawable = a.getDrawable(
                R.styleable.VideoControllerView_exit_fullscreen_drawable)
        if (exitDrawable != null) {
            setExitFullscreenDrawable(exitDrawable)
        }
    }

    private fun setupPlayPauseButton(a: TypedArray) {
        val drawable = a.getDrawable(R.styleable.VideoControllerView_play_drawable)
        if (drawable != null) {
            playDrawable = drawable
        }
        startPauseButton.get()!!.setImageDrawable(playDrawable)

        val drawable1 = a.getDrawable(R.styleable.VideoControllerView_pause_drawable)
        if (drawable1 != null) {
            pauseDrawable = drawable1
        }
    }

    fun setEnterFullscreenDrawable(enterFullscreenDrawable: Drawable?) {
        if (enterFullscreenDrawable != null) {
            this.enterFullscreenDrawable = enterFullscreenDrawable
        }
    }

    fun setExitFullscreenDrawable(exitFullscreenDrawable: Drawable?) {
        if (exitFullscreenDrawable != null) {
            this.exitFullscreenDrawable = exitFullscreenDrawable
        }
    }

    fun setPlayDrawable(playDrawable: Drawable?) {
        if (playDrawable != null) {
            this.playDrawable = playDrawable
        }
    }

    fun setPauseDrawable(pauseDrawable: Drawable?) {
        if (pauseDrawable != null) {
            this.pauseDrawable = pauseDrawable
        }
    }

    fun setFastForwardDrawable(fastForwardDrawable: Drawable) {
        this.fastForwardDrawable = fastForwardDrawable
    }

    fun setRewindDrawable(rewindDrawable: Drawable) {
        this.rewindDrawable = rewindDrawable
    }

    fun updateRewindDrawable() {
        if (rewButton.get() == null || videoMediaPlayer!!.get() == null) {
            return
        }

        rewButton.get()!!.setImageDrawable(rewindDrawable)
    }

    fun updateFastForwardDrawable() {
        if (ffwdButton.get() == null || videoMediaPlayer!!.get() == null) {
            return
        }

        ffwdButton.get()!!.setImageDrawable(fastForwardDrawable)
    }

    fun updatePausePlay() {
        if (startPauseButton.get() == null || videoMediaPlayer!!.get() == null) {
            return
        }

        if (videoMediaPlayer!!.get()!!.isPlaying) {
            startPauseButton.get()!!.setImageDrawable(pauseDrawable)
        } else {
            startPauseButton.get()!!.setImageDrawable(playDrawable)
        }
    }

    fun updateFullScreenDrawable() {
        if (fullscreenButton.get() == null || orientationHelper!!.get() == null) {
            return
        }

        if (orientationHelper!!.get()!!.isLandscape) {
            fullscreenButton.get()!!.setImageDrawable(exitFullscreenDrawable)
        } else {
            fullscreenButton.get()!!.setImageDrawable(enterFullscreenDrawable)
        }
    }

    fun setOrientationHelper(orientationHelper: OrientationHelper) {
        this.orientationHelper = WeakReference(orientationHelper)
    }

    fun setVideoMediaPlayer(videoMediaPlayer: VideoMediaPlayer) {
        this.videoMediaPlayer = WeakReference(videoMediaPlayer)
    }

    fun updatePlaybackSpeedText(text: String) {
        playbackSpeedButton.get()!!.text = text
    }
}
