package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.widget.CheckedTextView
import android.widget.LinearLayout
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Filter
import kotlinx.android.synthetic.main.filters_view.view.*

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

        filter_close.setOnClickListener {
            close()
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
        Filter.ONLINE -> filter_online
        Filter.OFFLINE -> filter_offline
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

        initRadioGroup(listOf(filter_online, filter_offline, filter_all)){
            when(it){
                filter_online -> {
                    if (!selected.contains(Filter.ONLINE)) {
                        selected.add(Filter.ONLINE)
                    }
                    selected.remove(Filter.OFFLINE)
                }
                filter_offline -> {
                    if (!selected.contains(Filter.OFFLINE)) {
                        selected.add(Filter.OFFLINE)
                    }
                    selected.remove(Filter.ONLINE)}
                filter_all -> {
                    if (!selected.contains(Filter.ONLINE)) {
                        selected.add(Filter.ONLINE)
                    }
                    if (!selected.contains(Filter.OFFLINE)) {
                        selected.add(Filter.OFFLINE)
                    }}
                else -> {

                }
            }

            consumer(selected)

        }

    }

    private fun initRadioGroup(btns: List<CheckedTextView>, onSelect: (CheckedTextView)->Unit){
        for (btn in btns){
            initRadioBtn(btn) {

                for(other in btns.filter { it != btn }){
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