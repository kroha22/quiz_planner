package com.quizplanner.quizPlanner

import android.util.Log

object QuizPlanner{

    const val APP_NAME = "QuizPlanner"
    var DEBUG = BuildConfig.DEBUG

    fun log(tag: String, msg: String) {
        Log.d(APP_NAME, "$tag: $msg")
    }

 }