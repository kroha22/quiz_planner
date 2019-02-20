package com.quizplanner.quizPlanner

import android.text.format.Time
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

object QuizPlanner {
    //------------------------------------------------------------------------------------------------
    val myLocale = Locale("ru", "RU")
    val formatterDay = SimpleDateFormat("EEE", myLocale)
    val formatterDate = SimpleDateFormat("dd", myLocale)
    val formatterDateMonth = SimpleDateFormat("dd MMM", myLocale)
    val formatterMonth = SimpleDateFormat("LLLL", myLocale)
    val formatterTime = SimpleDateFormat("HH:mm", myLocale)
    private const val MS_ON_DAY: Long = 86400000

    //------------------------------------------------------------------------------------------------

    const val APP_NAME = "QuizPlanner"
    var DEBUG = BuildConfig.DEBUG

    fun log(tag: String, msg: String) {
        Log.d(APP_NAME, "$tag: $msg")
    }

    fun today(): Date {
        return Date(System.currentTimeMillis())
    }

    fun formatterISO(): SimpleDateFormat {//todo check timeZone
        val formatterISO = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", myLocale)
        formatterISO.timeZone = TimeZone.getTimeZone("UTC")
        return formatterISO
    }

    fun getDates(daysBefore: Int, daysAfter: Int): List<Date> {
        val list = ArrayList<Date>()
        for (i in -daysBefore..daysAfter) {
            list.add(Date(System.currentTimeMillis() + MS_ON_DAY * i))
        }
        return list
    }

    fun isOneDay(day1: Date, day2: Date): Boolean {
        val time = Time()
        time.set(day1.time)

        val thenYear = time.year
        val thenMonth = time.month
        val thenMonthDay = time.monthDay

        time.set(day2.time)

        val thenYear2 = time.year
        val thenMonth2 = time.month
        val thenMonthDay2 = time.monthDay
        return (thenYear == thenYear2
                && thenMonth == thenMonth2
                && thenMonthDay == thenMonthDay2)
    }

}