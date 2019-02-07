package com.quizplanner.quizPlanner

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

object QuizPlanner{
    //------------------------------------------------------------------------------------------------
    val myLocale = Locale("ru", "RU")
    val formatterDay = SimpleDateFormat("EEE", myLocale)
    val formatterDate = SimpleDateFormat("dd", myLocale)
    val formatterDateMonth = SimpleDateFormat("dd.mm", myLocale)
    val formatterMonth = SimpleDateFormat("LLLL", myLocale)
    val formatterISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", myLocale)
    //------------------------------------------------------------------------------------------------

    const val APP_NAME = "QuizPlanner"
    var DEBUG = BuildConfig.DEBUG

    fun log(tag: String, msg: String) {
        Log.d(APP_NAME, "$tag: $msg")
    }

 }