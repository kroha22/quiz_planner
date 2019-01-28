package com.quizplanner.quizPlanner.ui

import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.dummy.DummyContent
import com.quizplanner.quizPlanner.model.Quiz
import java.util.*


@InjectViewState
class MainPresenter : MvpPresenter<MainView>() {
    //----------------------------------------------------------------------------------------------
    companion object {

        private val TAG: String = MainPresenter::class.java.name
        private fun log(msg: String) {
            QuizPlanner.log(TAG, msg)
        }
    }
    //----------------------------------------------------------------------------------------------

    private lateinit var escapeHandler: () -> Unit
    private var isStarted: Boolean = false

    fun start() {
        isStarted = true

        startLoad()
    }

    fun isStarted(): Boolean {
        return isStarted
    }

    fun setEscapeHandler(escapeHandler: () -> Unit) {
        this.escapeHandler = escapeHandler
    }

    fun onRefreshClick() {
        startLoad()
    }

    fun onQuizSelected(quiz: Quiz) {
        viewState.showQuizView(quiz)
    }

    private fun startLoad() {
        val mTabItems = LinkedHashMap<Date, List<Quiz>>()//todo load
        mTabItems[Date(System.currentTimeMillis() - 86400000 * 2)] = DummyContent.ITEMS
        mTabItems[Date(System.currentTimeMillis() - 86400000 * 1)] = DummyContent.ITEMS
        mTabItems[Date(System.currentTimeMillis())] = DummyContent.ITEMS
        mTabItems[Date(System.currentTimeMillis() + 86400000)] = DummyContent.ITEMS
        mTabItems[Date(System.currentTimeMillis() + 86400000 * 2)] = DummyContent.ITEMS
        viewState.setContent(mTabItems)
    }
}