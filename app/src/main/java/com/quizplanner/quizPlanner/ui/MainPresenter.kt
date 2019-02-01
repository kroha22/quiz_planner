package com.quizplanner.quizPlanner.ui

import android.content.Context
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import com.quizplanner.quizPlanner.model.QuizData
import rx.Observable
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
        private const val LOAD_ERROR_MESSAGE = "Во время загрузки произошла ошибка."
        private const val NO_DATA_MESSAGE = "Отсутствуют данные для отображения."
        private const val RELOAD_REQUEST = "Повторить загруку?"
        //-------------------------------------------------------------------------------------------

        private const val MS_ON_DAY: Long = 86400000
        private const val DAYS_FROM: Int = 20
        private const val DAYS_TO: Int = 20

        //-------------------------------------------------------------------------------------------
        private enum class State {
            EMPTY, MESSAGE, LOAD, GAMES_LIST, GAME
        }
        //-------------------------------------------------------------------------------------------

    }
    //----------------------------------------------------------------------------------------------

    private var dataLoader: RetrofitService? = null
    private var dao: Db.DAO? = null

    private lateinit var escapeHandler: () -> Unit
    private var isInitialized: Boolean = false
    private val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
    private var selectedQuiz: Quiz? = null

    private var subscription: Subscription? = null
    private var currentState: State? = null

    private var from = Date(System.currentTimeMillis() - 86400000 * 3)
    private var to = Date(System.currentTimeMillis() + 86400000 * 7)

    fun init(context: Context) {
        this.dataLoader = RetrofitService.getInstance(context)
        this.dao = Db.DAO(context)
        this.currentState = State.EMPTY

        updateDates()

        isInitialized = true
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

    fun start() {
        if (currentState == State.EMPTY) {

            getForecasts {
                if (gamesByDate.isEmpty()) {
                    startLoad()
                } else {
                    showGames()
                }
            }
        } else if (currentState == State.GAMES_LIST){
            showGames()
        }
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun setEscapeHandler(escapeHandler: () -> Unit) {
        this.escapeHandler = escapeHandler
    }

    fun onRefreshClick() {
        startLoad()
    }

    fun onQuizSelected(quiz: Quiz) {
        selectedQuiz = quiz

        viewState.showQuizView(quiz)
        setCurrentState(State.GAME)
    }

    private fun getForecasts(onComplete: ()->Unit) {
        showLoadProgress()

        subscription = loadForecastsFromDb()
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Load from bd games count ${it.size}") }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        { this.setForecasts(it) },
                        { this.onLoadError(it) },
                        { onComplete.invoke() })
    }

    private fun showLoadProgress() {
        viewState.showLoadProgress()
        setCurrentState(State.LOAD)
    }

    private fun loadForecastsFromDb(): Observable<List<QuizData>> {
        return Observable.create<List<QuizData>> { subscriber ->
            subscriber.onNext(dao!!.getGames())
            subscriber.onCompleted()
        }
    }

    private fun startLoad() {
        showLoadProgress()

        subscription = dataLoader!!.getQuizData(formatterISO.format(from), formatterISO.format(to))
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.size}") }
                .doOnNext { dao!!.saveGames(it) }
                .doOnNext { log("Save games count ${it.size}") }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ this.setForecasts(it) },
                        { this.onLoadError(it) },
                        { this.showGames() })
    }

    private fun getGamesByDate(games: List<QuizData>): LinkedHashMap<Date, MutableList<Quiz>> {
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
        return gamesByDate
    }

    private fun setForecasts(gamesByDate: LinkedHashMap<Date, MutableList<Quiz>>) {
        this.gamesByDate.clear()
        this.gamesByDate.putAll(gamesByDate)
    }

    private fun onLoadError(err: Throwable) {
        log("Load games error: ${err.message}")
        showReloadMsg(LOAD_ERROR_MESSAGE)
    }

    private fun showGames() {
        viewState.hideLoadProgress()
        viewState.setContent(gamesByDate)

        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }

        setCurrentState(State.GAMES_LIST)
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
