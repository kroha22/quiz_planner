package com.quizplanner.quizPlanner.ui

import android.content.Context
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Quiz
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


@InjectViewState
class MainPresenter : MvpPresenter<MainView>() {
    //----------------------------------------------------------------------------------------------
    companion object {

        private val TAG: String = MainPresenter::class.java.name
        private fun log(msg: String) {
            QuizPlanner.log(TAG, msg)
        }

        //-------------------------------------------------------------------------------------------
        private val LOAD_ERROR_MESSAGE = "Во время загрузки произошла ошибка."
        private val NO_DATA_MESSAGE = "Отсутствуют данные для отображения."
        private val RELOAD_REQUEST = "Повторить загруку?"
        //-------------------------------------------------------------------------------------------

        private val MS_ON_DAY: Long = 86400000
        private val DAYS_FROM: Int = 20
        private val DAYS_TO: Int = 7

        //-------------------------------------------------------------------------------------------
        private enum class State {
            EMPTY, MESSAGE, LOAD, GAMES_LIST, GAME
        }
        //-------------------------------------------------------------------------------------------

    }
    //----------------------------------------------------------------------------------------------

    private var dataLoader: RetrofitService? = null
    //   private var dao: Db.DAO? = null

    private lateinit var escapeHandler: () -> Unit
    private var isStarted: Boolean = false
    private val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
    private var selectedQuiz: Quiz? = null

    private var subscription: Subscription? = null
    private var currentState: State? = null

    private var from = Date(System.currentTimeMillis() - 86400000 * 3)
    private var to = Date(System.currentTimeMillis() + 86400000 * 7)

    fun init(context: Context) {

        updateDates()

        dataLoader = RetrofitService.getInstance(context)
        // this.dao = dao
        this.currentState = State.EMPTY
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

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

        subscription = dataLoader!!.getQuizData(formatterISO.format(from), formatterISO.format(to))
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { games -> /*todo save*/ }
                .map { games ->
                    val gamesByDate = LinkedHashMap<Date, MutableList<Quiz>>()
                    for (game in games) {
                        val date = formatterISO.parse(game.date!!)
                        var list = gamesByDate[date]
                        if (list == null) {
                            list = ArrayList()
                            gamesByDate[date] = list
                        }
                        list.add(game.getQuiz())
                    }
                    return@map gamesByDate
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.setForecasts(it) },
                        { this.onLoadError(it) },
                        { this.showGames() })
    }

    private fun setForecasts(gamesByDate: LinkedHashMap<Date, MutableList<Quiz>>) {
        this.gamesByDate.clear()
        this.gamesByDate.putAll(gamesByDate)
    }

    private fun onLoadError(err: Throwable) {
        err.printStackTrace()
        showReloadMsg(LOAD_ERROR_MESSAGE)
    }

    private fun showGames() {
        viewState.setContent(gamesByDate)
        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }
    }

    private fun showReloadMsg(errMsg: String) {
        viewState.showDialog(DialogBuilder()
                .msg("$errMsg $RELOAD_REQUEST")
                .onPositive { this.startLoad() })
        setCurrentState(State.MESSAGE)
    }

    private fun setCurrentState(currentState: State) {
        this.currentState = currentState
    }

    private fun updateDates() {
        from = Date(System.currentTimeMillis() - MS_ON_DAY * DAYS_FROM)
        to = Date(System.currentTimeMillis() + MS_ON_DAY * DAYS_TO)
    }


}
