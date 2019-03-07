package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.exchange.Input
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import kotlinx.android.synthetic.main.activity_favorites.*
import kotlinx.android.synthetic.main.quiz_list.*
import rx.Observable
import rx.Subscription
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


//---------------------------------------------------------------------------------------------

interface FavoritesView : MvpView {

    companion object {
        const val PROGRESS_TAG = "PROGRESS_TAG"
    }

    @StateStrategyType(SkipStrategy::class)
    fun showMessage(msg: String)

    @StateStrategyType(SkipStrategy::class)
    fun showDialog(dialogBuilder: DialogBuilder)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setContent(items: List<Quiz>)

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun showLoadProgress()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun hideLoadProgress()

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showQuizView(quiz: Quiz)
}

//---------------------------------------------------------------------------------------------
class FavoritesActivity : MvpAppCompatActivity(), FavoritesView, SimpleItemRecyclerViewAdapter.ItemClickListener {

    companion object {
        const val FAVORITES_GAMES_CODE = "favorite_games_item"
        private const val FAVORITES: String = "FavoritesActivity"
    }

    private val adapter = SimpleItemRecyclerViewAdapter(true)

    @InjectPresenter(tag = FAVORITES)
    lateinit var presenter: FavoritesPresenter

    private lateinit var items: List<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorites)
        setSupportActionBar(detail_toolbar)
        detail_toolbar.setNavigationOnClickListener { this.onBackPressed() }

        adapter.setOnClickListener(this)

        quiz_list.adapter = adapter
        quiz_list_empty_view.text = getText(R.string.favorites_empty)
        refreshView()
    }

    override fun onResume() {
        super.onResume()
        presenter.init(this)
        presenter.start()
    }

    override fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun showDialog(dialogBuilder: DialogBuilder) {
        dialogBuilder.show(this)
    }

    override fun setContent(items: List<Quiz>) {
        adapter.setValues(items)
        refreshView()
    }

    override fun showLoadProgress() {
        quiz_list_empty_view.visibility = View.GONE
        quiz_list.visibility = View.GONE
        wait_view.visibility = View.VISIBLE
    }

    override fun hideLoadProgress() {
        quiz_list_empty_view.visibility = View.VISIBLE
        quiz_list.visibility = View.VISIBLE
        wait_view.visibility = View.GONE
    }

    override fun onItemClick(quiz: Quiz) {
        presenter.onGameSelected(quiz)
    }

    override fun onItemCheckChanged(quiz: Quiz) {
        if (!quiz.isChecked) {
            removeItem(quiz)
        }

        presenter.onGameCheckChanged(quiz)

    }

    override fun showQuizView(quiz: Quiz) {
        val intent = Intent(this, QuizDetailActivity::class.java).apply {
            putExtra(QuizDetailActivity.QUIZ_ITEM_CODE, quiz)
        }
        startActivity(intent)
    }

    private fun removeItem(quiz: Quiz) {
        adapter.removeItem(quiz)
        refreshView()
    }

    private fun refreshView() {
        if (adapter.isEmpty()) {
            quiz_list_empty_view.visibility = View.VISIBLE
            quiz_list.visibility = View.INVISIBLE
        } else {
            quiz_list_empty_view.visibility = View.INVISIBLE
            quiz_list.visibility = View.VISIBLE
        }
    }
}
//--------------------------------------------------------------------------------------------


@InjectViewState
class FavoritesPresenter : MvpPresenter<FavoritesView>() {
    //----------------------------------------------------------------------------------------------
    companion object {

        private val TAG: String = FavoritesPresenter::class.java.name
        private fun log(msg: String) {
            QuizPlanner.log(TAG, msg)
        }

        //-------------------------------------------------------------------------------------------
        private const val BD_ERROR_MESSAGE = "Внутренняя ошибка"
        private const val LOAD_ERROR_MESSAGE = "Во время загрузки произошла ошибка. Проверьте наличие сети"
        private const val NO_DATA_MESSAGE = "Отсутствуют данные для отображения"
        //-------------------------------------------------------------------------------------------

    }
    //----------------------------------------------------------------------------------------------

    private var dataLoader: RetrofitService? = null
    private var dao: Db.DAO? = null

    private val allGames = ArrayList<Quiz>()
    private var selectedQuiz: Quiz? = null

    private var subscription: Subscription? = null

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.showLoadProgress()
    }

    fun init(context: Context) {
        this.dataLoader = RetrofitService.getInstance(context)
        this.dao = Db.DAO(context)
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

    fun start() {
        subscription = Observable.create<List<Quiz>> { it.onNext(loadFromDb()) }
                .subscribeOn(Schedulers.io())
                .map { games -> games.filter { it.isChecked } }
                .doOnError { onBdError(it) }
                .doOnNext { setGames(it) }
                .flatMap { games ->
                    if (!games.isEmpty()) {
                        load({ }, { dataLoader!!.getQuizData(ArrayList(games.map { it.id!! }.toList())) })
                    } else {
                        Observable.just(Collections.emptyList())
                    }
                }
                .doOnNext { setGames(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    showGames()
                }, {
                    onError(it)
                })
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

        allGames[allGames.indexOf(quiz)].isChecked = quiz.isChecked
    }

    private fun loadFromDb(): List<Quiz> {
        return dao!!.getGames()
    }

    private fun load(beforeStartLoad: () -> Unit, from: () -> Observable<List<Input.QuizData>>): Observable<List<Quiz>> {
        beforeStartLoad.invoke()

        return from.invoke()
                .timeout(1, TimeUnit.SECONDS)
                .retry(2)
                .subscribeOn(Schedulers.io())
                .doOnNext { log("Consume games count ${it.size}") }
                .map { dao!!.saveGames(it) }
    }

    private fun showGames() {
        viewState.hideLoadProgress()
        viewState.setContent(allGames)
    }

    private fun setGames(games: List<Quiz>) {
        allGames.clear()
        allGames.addAll(games)
    }

    private fun onError(err: Throwable) {
        log("Load games error: ${err.message}")
        showGames()
    }

    private fun onBdError(err: Throwable) {
        log("Bd error: ${err.message}")
        showReloadMsg(BD_ERROR_MESSAGE)

        showGames()
    }

    private fun showReloadMsg(errMsg: String) {
        viewState.showMessage(errMsg)
    }

}

