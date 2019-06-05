package com.quizplanner.quizPlanner.customvideoview

import android.annotation.TargetApi
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build

internal class VideoMediaPlayer(private var customVideoView: CustomVideoView?) : MediaPlayer() {
    var isAutoStartEnabled: Boolean = false
        private set
    private var canPause = true
    private var showSeekBackwardButton = false
    private var showSeekForwardButton = false
    private var showPlaybackSpeedButton = false

    val bufferPercentage: Int
        get() = 0

    fun canPause(): Boolean {
        return canPause
    }

    fun showSeekForwardButton(): Boolean {
        return showSeekForwardButton
    }

    fun showSeekBackwardButton(): Boolean {
        return showSeekBackwardButton
    }

    fun showPlaybackSpeedButton(): Boolean {
        return showPlaybackSpeedButton
    }

    fun toggleFullScreen() {
        if (customVideoView != null) {
            customVideoView!!.toggleFullscreen()
        }
    }

    fun onPauseResume() {
        if (isPlaying) {
            pause()
        } else {
            start()
        }
    }

    fun onDetach() {
        customVideoView = null
        setOnPreparedListener(null)
        stop()
        release()
    }

    fun enableAutoStart() {
        isAutoStartEnabled = true
    }

    fun setPauseEnabled(canPause: Boolean) {
        this.canPause = canPause
    }

    fun disablePause() {
        this.canPause = false
    }

    fun addSeekForwardButton() {
        this.showSeekForwardButton = true
    }

    fun addSeekBackwardButton() {
        this.showSeekBackwardButton = true
    }

    fun addPlaybackSpeedButton() {
        this.showPlaybackSpeedButton = true
    }

    fun setCanSeekBackward(canSeekBackward: Boolean) {
        this.showSeekBackwardButton = canSeekBackward
    }

    fun setCanSeekForward(canSeekForward: Boolean) {
        this.showSeekForwardButton = canSeekForward
    }

    @TargetApi(Build.VERSION_CODES.M)
    fun changePlaybackSpeed(speed: Float) {
        val playbackParams = PlaybackParams()
        playbackParams.speed = speed
        setPlaybackParams(playbackParams)
    }
}
