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
import kotlin.collections.ArrayList

@Suppress("DEPRECATION")
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
        private const val DAYS_TO: Int = 13
        //-------------------------------------------------------------------------------------------

    }
    //----------------------------------------------------------------------------------------------

    private var dataLoader: RetrofitService? = null
    private var dao: Db.DAO? = null

    private lateinit var escapeHandler: () -> Unit
    private var isInitialized: Boolean = false
    private var needCheckFavourites: Boolean = false
    private var loadInProgress: Boolean = false
    private var lastLoadTime: Long = 0
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
        } else if (needCheckFavourites) {

            subscription = Observable.create<List<Quiz>> { it.onNext(dao!!.getGames()) }
                    .subscribeOn(Schedulers.io())
                    .map { setGames(it) }
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({ needUpdateView ->
                        needCheckFavourites = false
                        if (needUpdateView) {
                            showGames()
                        }
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
        if (loadInProgress) {
            return
        }

        val now = Date().time

        /*if (now - lastLoadTime < 15000) {
            onStartRefresh()

            subscription = Observable.interval(1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { onStopRefresh() }
            return
        }*/

        lastLoadTime = now
        startLoad({ onStartRefresh() }, { hasChanges ->
            if (hasChanges) {
                showGames()
            }
            onStopRefresh()
        })
    }

    fun onGameSelected(quiz: Quiz) {
        needCheckFavourites = true
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
        needCheckFavourites = true
        viewState.showCheckedGames()
    }

    private fun onStartRefresh() {
        loadInProgress = true
        viewState.showLoadProgress()
    }

    private fun onStopRefresh() {
        loadInProgress = false
        viewState.hideLoadProgress()
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
        startLoad({ }, {
            showGames()
            viewState.hideStartLoad()
        })
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

    // onComplete invoke true if games is updated
    private fun startLoad(beforeStartLoad: () -> Unit, onComplete: (Boolean) -> Unit) {
        startLoad({ dataLoader!!.getQuizData(formatterISO().format(dates.first()), formatterISO().format(dates.last())) }, beforeStartLoad, onComplete)
    }

    private fun startLoad(from: () -> Observable<List<Input.QuizData>>, beforeStartLoad: () -> Unit, onComplete: (Boolean) -> Unit) {
        beforeStartLoad.invoke()

        subscription = load(from)
                .map { setGames(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ needUpdateView ->
                    onComplete.invoke(needUpdateView)
                }, {
                    onLoadError(it)
                    onComplete.invoke(true)
                })
    }

    private fun load(from: () -> Observable<List<Input.QuizData>>): Observable<List<Quiz>> {
        return from.invoke()
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.size}") }
                .map {
                    if (hasChanges(it)) {
                        val games = dao!!.saveGames(it)

                        val toDel = ArrayList<Quiz>()
                        for (game in allGames) {
                            if (!games.contains(game)) {
                                toDel.add(game)
                            }
                        }
                        dao!!.delete(toDel)

                        games
                    } else {
                        allGames
                    }
                }
    }

    private fun hasChanges(newData: List<Input.QuizData>): Boolean {
        if (newData.size != allGames.size) {
            return true
        }

        val gamesMap = HashMap<String, Quiz>()
        for (g in allGames) {
            gamesMap[g.id!!] = g
        }
        for (g in newData) {
            val game = gamesMap[g.id!!]
            if (game == null || !game.equalQuizData(g)) {
                return true
            }
        }

        return false

    }

    private fun isIdentical(games1: List<Quiz>, games2: List<Quiz>): Boolean {
        if (games1.size != games2.size) {
            return false
        }

        val gamesMap = HashMap<String, Quiz>()
        for (g in games1) {
            gamesMap[g.id!!] = g
        }
        for (g in games2) {
            val game = gamesMap[g.id!!]
            if (game == null || !game.identical(g)) {
                return false
            }
        }

        return true
    }

    private fun getGamesByDate(games: List<Quiz>): LinkedHashMap<Date, List<Quiz>> {
        val gamesByDate = LinkedHashMap<Date, List<Quiz>>()
        for (date in dates) {
            gamesByDate[date] = ArrayList(games.filter { game -> isOneDay(game.getDate(), date) }.sortedWith(kotlin.Comparator { o1, o2 -> o1.date!!.compareTo(o2.date!!) }))
        }

        return gamesByDate
    }

    private fun showGames() {
        viewState.setContent(gamesByDate, selectedDate)

        if (gamesByDate.isEmpty()) {
            viewState.showMessage(NO_DATA_MESSAGE)
        }
    }

    //return true if has changes
    private fun setGames(games: List<Quiz>): Boolean {
        if (isIdentical(allGames, games)) {
            return false
        }

        allGames.clear()
        allGames.addAll(games)

        setGames(getGamesByDate(allGames))
        return true
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
