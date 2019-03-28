package com.quizplanner.quizPlanner.ui

import android.content.Context
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpPresenter
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterISO
import com.quizplanner.quizPlanner.QuizPlanner.getDates
import com.quizplanner.quizPlanner.QuizPlanner.isOneDay
import com.quizplanner.quizPlanner.QuizPlanner.today
import com.quizplanner.quizPlanner.exchange.Input
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
    private var needUpdate: Boolean = false
    private val allGames = ArrayList<Quiz>()
    private val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
    private var selectedDate: Date = today()

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
        } else if (needUpdate) {

            subscription = Observable.create<List<Quiz>> { it.onNext(dao!!.getGames()) }
                    .subscribeOn(Schedulers.io())
                    .map { setGames(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        needUpdate = false
                        showGames()
                    }, {
                        onBdError(it)
                    })
        }
    }

    fun isInitialized(): Boolean {
        return isInitialized
    }

    fun setEscapeHandler(escapeHandler: () -> Unit) {
        this.escapeHandler = escapeHandler
    }

    fun onRefreshClick() {
        startLoad({ viewState.showLoadProgress() }, { viewState.hideLoadProgress() })
    }

    fun onGameSelected(quiz: Quiz) {
        needUpdate = true
        viewState.showQuizView(quiz)
    }

    fun onGameCheckChanged(quiz: Quiz) {
        if (quiz.isChecked) {
            dao!!.setCheckedGame(quiz)
        } else {
            dao!!.setUncheckedGame(quiz)
        }

        allGames[allGames.indexOf(quiz)].isChecked = quiz.isChecked
    }

    fun onDateSelect(date: Date) {
        selectedDate = date
    }

    fun onContactsRequested() {
        viewState.showContacts()
    }

    fun onCheckedGamesRequested() {
        needUpdate = true
        viewState.showCheckedGames()
    }

    private fun getGamesFromDb() {
        subscription = clearDbOldGames()
                .subscribeOn(Schedulers.io())
                .map { dao!!.getGames() }
                .map { setGames(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ onLoadedFromDb() }, { onBdError(it) })
    }

    private fun clearDbOldGames() = Observable.create<Int> { it.onNext(dao!!.clearGames(dates.first())) }

    private fun onLoadedFromDb() {
        if (hasGames(gamesByDate)) {
            showGames()
        }

        startLoad({ }, { viewState.hideStartLoad() })
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

    private fun startLoad(beforeStartLoad: () -> Unit, onComplete: () -> Unit) {
        startLoad({ dataLoader!!.getQuizData(formatterISO().format(dates.first()), formatterISO().format(dates.last())) }, beforeStartLoad, onComplete)
    }

    private fun startLoad(from: () -> Observable<List<Input.QuizData>>, beforeStartLoad: () -> Unit, onComplete: () -> Unit) {
        subscription = load(beforeStartLoad, from)
                .doOnNext { setGames(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showGames()
                    onComplete.invoke()
                }, {
                    onLoadError(it)
                    onComplete.invoke()
                })
    }

    private fun load(beforeStartLoad: () -> Unit, from: () -> Observable<List<Input.QuizData>>): Observable<List<Quiz>> {
        beforeStartLoad.invoke()

        return from.invoke()
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.size}") }
                .map { dao!!.saveGames(it) }
    }


    private fun getGamesByDate(games: List<Quiz>): LinkedHashMap<Date, List<Quiz>> {
        val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
        for (date in dates) {
            gamesByDate[date] = ArrayList(games.filter { game -> isOneDay(game.getDate(), date) })
        }
        return gamesByDate
    }

    private fun showGames() {
        viewState.setContent(gamesByDate, selectedDate)

        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }
    }

    private fun setGames(games: List<Quiz>) {
        allGames.clear()
        allGames.addAll(games)

        setGames(getGamesByDate(allGames))
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
