package com.quizplanner.quizPlanner.player.ui.menu

import androidx.annotation.DrawableRes
import android.view.View

data class MenuItem @JvmOverloads constructor(val text: String, @DrawableRes val icon: Int? = null, val onClickListener: View.OnClickListener)