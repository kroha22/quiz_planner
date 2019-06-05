package com.quizplanner.quizPlanner.customvideoview

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.annotation.RequiresApi
import android.support.v4.content.ContextCompat

class Builder internal constructor(private val customVideoView: CustomVideoView,
                                   private val controller: VideoControllerView,
                                   private val orientationHelper: OrientationHelper,
                                   private val videoMediaPlayer: VideoMediaPlayer) {

    fun videoUrl(context: Context, videoUrl: Uri): Builder {
        customVideoView.setupMediaPlayer(context, videoUrl)
        return this
    }

    fun enableAutoStart(): Builder {
        customVideoView.enableAutoStart()
        return this
    }

    fun progressBarColor(progressBarColor: Int): Builder {
        controller.setProgressBarColor(progressBarColor)
        return this
    }

    fun landscapeOrientation(landscapeOrientation: LandscapeOrientation): Builder {
        orientationHelper.setLandscapeOrientation(landscapeOrientation)
        return this
    }

    fun portraitOrientation(portraitOrientation: PortraitOrientation): Builder {
        orientationHelper.setPortraitOrientation(portraitOrientation)
        return this
    }

    fun disablePause(): Builder {
        videoMediaPlayer.disablePause()
        return this
    }

    fun addSeekForwardButton(): Builder {
        videoMediaPlayer.addSeekForwardButton()
        return this
    }

    fun addSeekBackwardButton(): Builder {
        videoMediaPlayer.addSeekBackwardButton()
        return this
    }

    @RequiresApi(Build.VERSION_CODES.M)
    fun addPlaybackSpeedButton(): Builder {
        videoMediaPlayer.addPlaybackSpeedButton()
        return this
    }

    private fun getDrawable(drawableResId: Int): Drawable? {
        val context = customVideoView.context
        return ContextCompat.getDrawable(context, drawableResId)
    }

    fun playbackSpeedOptions(playbackSpeedOptions: PlaybackSpeedOptions): Builder {
        controller.setPlaybackSpeedOptions(playbackSpeedOptions)
        return this
    }
}
