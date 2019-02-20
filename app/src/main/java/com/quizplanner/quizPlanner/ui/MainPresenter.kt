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
        private const val BD_ERROR_MESSAGE = "Внутренняя ошибка"
        private const val LOAD_ERROR_MESSAGE = "Во время загрузки произошла ошибка. Проверьте наличие сети"
        private const val NO_DATA_MESSAGE = "Отсутствуют данные для отображения"
        //-------------------------------------------------------------------------------------------
        private const val DAYS_FROM: Int = 3
        private const val DAYS_TO: Int = 7
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

    private var dates: MutableList<Date> = ArrayList()

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.showStartLoad()
    }

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
        if (gamesByDate.isEmpty()) {
            getGamesFromDb()
        }
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun setEscapeHandler(escapeHandler: () -> Unit) {
        this.escapeHandler = escapeHandler
    }

    fun onRefreshClick() {
        viewState.showLoadProgress()

        startLoad { viewState.hideLoadProgress() }
    }

    fun onGameSelected(quiz: Quiz) {
        selectedQuiz = quiz

        viewState.showQuizView(quiz)
    }

    fun onGameCheckChanged(quiz: Quiz) {
        if (quiz.isChecked) {
            dao!!.setCheckedGame(quiz)
        } else {
            dao!!.setUncheckedGame(quiz)
        }
    }

    fun onLinkClick(quiz: Quiz) {
        var url = quiz.registrationLink!!
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        viewState.requestLink(url)
    }

    fun onDateSelect(date: Date) {
        selectedDate = date
    }

    private fun getGamesFromDb() {
        subscription = clearDbOldGames()
                .subscribeOn(Schedulers.io())
                .map { dao!!.getGames() }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onLoadedFromDb(it) }, { onBdError(it) })
    }

    private fun clearDbOldGames(): Observable<Int> {
        return Observable.create<Int> { subscriber ->
            subscriber.onNext(dao!!.clearGames(dates.first()))
        }
    }

    private fun onLoadedFromDb(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        if (hasGames(gamesByDate)) {
            showGames(gamesByDate)
        }

        startLoad { viewState.hideStartLoad() }
    }

    private fun hasGames(gamesByDate: Map<Date, List<Quiz>>): Boolean {
        for (games in gamesByDate.entries) {
            if (!games.value.isEmpty()) {
                return true
            }
        }
        return false
    }

    private fun onBdError(err: Throwable) {
        log("Bd error: ${err.message}")
        showReloadMsg(BD_ERROR_MESSAGE)

        viewState.hideStartLoad()
        onError()
    }

    private fun startLoad(onComplete: () -> Unit) {
        subscription = dataLoader!!.getQuizData(formatterISO().format(dates.first()), formatterISO().format(dates.last()))
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.size}") }
                .map { dao!!.saveGames(it) }
                .map { getGamesByDate(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showGames(it)
                    onComplete.invoke()
                }, {
                    onLoadError(it)
                    onComplete.invoke()
                })
    }

    private fun getGamesByDate(games: List<Quiz>): LinkedHashMap<Date, List<Quiz>> {
        val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
        for (date in dates) {
            gamesByDate[date] = ArrayList(games.filter { game -> isOneDay(game.getDate(), date) })
        }
        return gamesByDate
    }

    private fun showGames(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        setGames(gamesByDate)
        showGames()
    }

    private fun showGames() {
        viewState.setContent(gamesByDate, selectedDate)

        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }
    }

    private fun setGames(gamesByDate: LinkedHashMap<Date, List<Quiz>>) {
        this.gamesByDate.clear()
        this.gamesByDate.putAll(gamesByDate)
    }

    private fun updateDates() {
        dates.clear()
        dates.addAll(getDates(DAYS_FROM, DAYS_TO))
    }

    private fun onLoadError(err: Throwable) {
        log("Load games error: ${err.message}")
        showReloadMsg(LOAD_ERROR_MESSAGE)

        onError()
    }

    private fun onError() {
        if (gamesByDate.isEmpty()) {
            for (date in dates) {
                gamesByDate[date] = emptyList()
            }
        }

        showGames()
    }

    private fun showReloadMsg(errMsg: String) {
        viewState.showMessage(errMsg)
    }

}
