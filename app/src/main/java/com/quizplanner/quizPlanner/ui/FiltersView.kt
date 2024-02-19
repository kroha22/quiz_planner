package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.CheckedTextView
import android.widget.ImageView
import android.widget.LinearLayout
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Filter
import kotlin.math.abs


/**
 *  FiltersView.kt *
 *  Created by Olga Cherepanova on 22.09.2020.
 *  Copyright © 2020 SocElectroProject. All rights reserved.
 */

class FiltersView : LinearLayout {

    var consumer: (List<Filter>) -> Unit = {}
    var onClose: () -> Unit = {}
    private val selected: MutableList<Filter> = arrayListOf()

    constructor(context: Context) : super(context)

    constructor(arg0: Context, arg1: AttributeSet) : super(arg0, arg1)

    constructor(arg0: Context, arg1: AttributeSet, arg2: Int) : super(arg0, arg1, arg2)

    init {
        LayoutInflater.from(context).inflate(R.layout.filters_view, this, true)
        clipChildren = true

        setWillNotDraw(false)

        initOnlineOffline()

        findViewById<ImageView>(R.id.filter_close).setOnClickListener {
            close()
        }

        findViewById<LinearLayout>(R.id.container).setOnTouchListener(object : OnSwipeTouchListener(context) {

            override fun onSwipeRight() {
                close()
            }

        })
    }

    fun select(filters: List<Filter>) {
        if (filters.containsAll(selected) && selected.containsAll(filters)) {
            return
        }

        if (filters.containsAll(Filter.values().asList())) {
            findViewById<CheckedTextView>(R.id.filter_all).callOnClick()
            return
        }

        //todo???

        for (f in filters) {
            getBtnForFilter(f).callOnClick()
        }
    }

    fun close() {
        consumer(selected)
        onClose()
    }

    private fun initBtn(filter: Filter) {
        initBtn(getBtnForFilter(filter), filter)
    }

    private fun getBtnForFilter(filter: Filter) = when (filter) {
        Filter.ONLINE -> findViewById<CheckedTextView>(R.id.filter_online)
        Filter.OFFLINE -> findViewById<CheckedTextView>(R.id.filter_offline)
    }

    private fun initBtn(btn: CheckedTextView, filter: Filter) {

        btn.setOnClickListener {
            if (btn.isChecked) {
                btn.isChecked = false
                selected.remove(filter)
            } else {
                btn.isChecked = true

                selected.add(filter)
            }

            consumer(selected)

        }

        btn.isChecked = false
    }

    private fun initOnlineOffline() {

        initRadioGroup(listOf(findViewById<CheckedTextView>(R.id.filter_online), findViewById<CheckedTextView>(R.id.filter_offline), findViewById<CheckedTextView>(R.id.filter_all))) {
            when (it) {
                findViewById<CheckedTextView>(R.id.filter_online) -> {
                    if (!selected.contains(Filter.ONLINE)) {
                        selected.add(Filter.ONLINE)
                    }
                    selected.remove(Filter.OFFLINE)
                }
                findViewById<CheckedTextView>(R.id.filter_offline) -> {
                    if (!selected.contains(Filter.OFFLINE)) {
                        selected.add(Filter.OFFLINE)
                    }
                    selected.remove(Filter.ONLINE)
                }
                findViewById<CheckedTextView>(R.id.filter_all) -> {
                    if (!selected.contains(Filter.ONLINE)) {
                        selected.add(Filter.ONLINE)
                    }
                    if (!selected.contains(Filter.OFFLINE)) {
                        selected.add(Filter.OFFLINE)
                    }
                }
                else -> {

                }
            }

            consumer(selected)

        }

    }

    private fun initRadioGroup(btns: List<CheckedTextView>, onSelect: (CheckedTextView) -> Unit) {
        for (btn in btns) {
            initRadioBtn(btn) {

                for (other in btns.filter { it != btn }) {
                    other.isChecked = false
                }

                onSelect(btn)
            }
        }
    }

    private fun initRadioBtn(btn: CheckedTextView, onCheck: () -> Unit) {
        btn.setOnClickListener {
            if (!btn.isChecked) {
                btn.isChecked = true

                onCheck()
            }
        }
    }
}

open class OnSwipeTouchListener(ctx: Context?) : OnTouchListener {

    companion object {
        private const val SWIPE_THRESHOLD = 100
        private const val SWIPE_VELOCITY_THRESHOLD = 100
    }

    private val gestureDetector: GestureDetector

    override fun onTouch(v: View?, event: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(event)
    }

    private inner class GestureListener : SimpleOnGestureListener() {

        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            var result = false
            try {
                val diffY = e2.y - (e1?.y ?: 0.0f)
                val diffX = e2.x - (e1?.x ?: 0.0f)
                if (abs(diffX) > abs(diffY)) {
                    if (abs(diffX) > Companion.SWIPE_THRESHOLD && abs(velocityX) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                        result = true
                    }
                } else if (abs(diffY) > Companion.SWIPE_THRESHOLD && abs(velocityY) > Companion.SWIPE_VELOCITY_THRESHOLD) {
                    if (diffY > 0) {
                        onSwipeBottom()
                    } else {
                        onSwipeTop()
                    }
                    result = true
                }
            } catch (exception: Exception) {
                exception.printStackTrace()
            }
            return result
        }
    }

    open fun onSwipeRight() {}
    open fun onSwipeLeft() {}
    open fun onSwipeTop() {}
    open fun onSwipeBottom() {}

    init {
        gestureDetector = GestureDetector(ctx, GestureListener())
    }
}