package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.view.View
import androidx.appcompat.app.AlertDialog

/**
 * Created by Olga Cherepanova
 * on 20.01.2019.
 */

@Suppress("SENSELESS_COMPARISON")
class DialogBuilder {
    private var title: String? = null
    private var msg: String? = null
    private var view: View? = null
    private var positive: String? = null
    private var onPositive: () -> Unit? = {}
    private var negative: String? = null
    private var onNegative: () -> Unit? = {}
    private var isCancelable: Boolean = true

    fun title(title: String): DialogBuilder {
        this.title = title
        return this
    }

    fun msg(msg: String): DialogBuilder {
        this.msg = msg
        return this
    }

    fun view(view: View): DialogBuilder {
        this.view = view
        return this
    }

    fun positive(positive: String): DialogBuilder {
        this.positive = positive
        return this
    }

    fun onPositive(onPositive: () -> Unit): DialogBuilder {
        this.onPositive = onPositive
        return this
    }

    fun negative(negative: String): DialogBuilder {
        this.negative = negative
        return this
    }

    fun onNegative(onNegative: () -> Unit): DialogBuilder {
        this.onNegative = onNegative
        return this
    }

    fun cancelable(isCancelable: Boolean): DialogBuilder {
        this.isCancelable = isCancelable
        return this
    }

    fun show(context: Context) {
        val builder = AlertDialog.Builder(context)
        builder.
                setTitle(title).
                setMessage(msg).
                setCancelable(isCancelable)

        if (positive != null) {
            builder.setNeutralButton(
                    positive
            ) { _, _ ->
                if (onPositive != null) {
                    onPositive.invoke()
                }
            }
        }

        if (negative != null) {
            builder.setPositiveButton(
                    negative,
                    { _, _ ->
                        if (onNegative != null) {
                            onNegative.invoke()
                        }
                    })
        }

        builder.create().show()
    }


}