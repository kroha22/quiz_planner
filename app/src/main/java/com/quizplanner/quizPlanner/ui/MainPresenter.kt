package com.quizplanner.quizPlanner.ui

import android.content.Context
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO
import com.quizplanner.quizPlanner.QuizPlanner.getDates
import com.quizplanner.quizPlanner.QuizPlanner.isOneDay
import com.quizplanner.quizPlanner.QuizPlanner.today
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
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
        private const val DAYS_FROM: Int = 3
        private const val DAYS_TO: Int = 7

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
    private var selectedDate: Date = today()
    private var selectedQuiz: Quiz? = null

    private var subscription: Subscription? = null
    private var currentState: State = State.EMPTY

    private var dates: MutableList<Date> = ArrayList()

    fun init(context: Context) {
        this.dataLoader = RetrofitService.getInstance(context)
        this.dao = Db.DAO(context)

        updateDates()

        isInitialized = true
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

    fun start() {
        if (currentState == State.EMPTY) {
            getGamesFromDb()
        }
    }

    private fun hasGames(gamesByDate: Map<Date, List<Quiz>>): Boolean {
        for (games in gamesByDate.entries) {
            if (!games.value.isEmpty()) {
                return true
            }
        }
        return false

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

    fun onGameSelected(quiz: Quiz) {
        selectedQuiz = quiz

        viewState.showQuizView(quiz)
        setCurrentState(State.GAME)
    }

    fun onGameCheckChanged(quiz: Quiz) {
        if (quiz.isChecked) {
            dao!!.setCheckedGame(quiz)
        } else {
            dao!!.setUncheckedGame(quiz)
        }
    }

    fun onLinkClick(quiz: Quiz) {
        var url = quiz.registrationLink
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        viewState.requestLink(url)
    }

    fun onDateSelect(date: Date) {
        selectedDate = date
    }

    private fun getGamesFromDb() {
        showLoadProgress()

        subscription = clearDbOldGames()
                .subscribeOn(Schedulers.io())
                .map { dao!!.getGames() }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe( { onLoadedFromDb(it) }, { this.onLoadError(it) } )
    }

    private fun showLoadProgress() {
        viewState.showLoadProgress()
        setCurrentState(State.LOAD)
    }

    private fun clearDbOldGames(): Observable<Int> {

        return Observable.create<Int> { subscriber ->
            subscriber.onNext(dao!!.clearGames(dates.first()))
        }
    }

    private fun onLoadedFromDb(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        if (hasGames(gamesByDate)) {
            showGames(gamesByDate)
        } else {
            startLoad()
        }
    }

    private fun showGames(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        setGames(gamesByDate)
        showGames()
    }

    private fun startLoad() {
        showLoadProgress()

        subscription = dataLoader!!.getQuizData(formatterISO.format(dates.first()), formatterISO.format(dates.last()))
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.map { games -> games.id }.toList()}") }
                .map { dao!!.saveGames(it) }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ showGames(it) }, { onLoadError(it) } )
    }

    private fun getGamesByDate(games: List<Quiz>): LinkedHashMap<Date, List<Quiz>> {
        val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
        for (date in dates) {
            gamesByDate[date] = ArrayList(games.filter { game -> isOneDay(game.date, date) })
        }
        return gamesByDate
    }

    private fun setGames(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        this.gamesByDate.clear()
        this.gamesByDate.putAll(gamesByDate)
    }

    private fun onLoadError(err: Throwable) {
        log("Load games error: ${err.message}")
        showReloadMsg(LOAD_ERROR_MESSAGE)
    }

    private fun showGames() {
        viewState.hideLoadProgress()
        viewState.setContent(gamesByDate, selectedDate)

        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }

        setCurrentState(State.GAMES_LIST)
    }

    private fun showReloadMsg(errMsg: String) {
        viewState.showDialog(DialogBuilder()
                .msg("$errMsg $RELOAD_REQUEST")
                .positive("Ок")
                .onPositive { this.startLoad() }
                .cancelable(false))
        setCurrentState(State.MESSAGE)
    }

    private fun setCurrentState(currentState: State) {
        this.currentState = currentState
    }

    private fun updateDates() {
        dates.clear()
        dates.addAll(getDates(DAYS_FROM, DAYS_TO))
    }

}
