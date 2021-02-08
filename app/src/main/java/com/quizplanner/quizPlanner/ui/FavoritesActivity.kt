package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import moxy.InjectViewState
import moxy.MvpAppCompatActivity
import moxy.MvpPresenter
import moxy.MvpView
import moxy.presenter.InjectPresenter
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.isLast
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.exchange.Input
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import kotlinx.android.synthetic.main.activity_favorites.*
import kotlinx.android.synthetic.main.quiz_list.*
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
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
        private const val FAVORITES: String = "favorites"
    }

    private val adapter = FavoritesAdapter()

    @InjectPresenter(tag = FAVORITES)
    lateinit var presenter: FavoritesPresenter

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
            putExtra(QuizDetailActivity.SOURCE_CODE, QuizDetailActivity.MAIN)
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
class FavoritesAdapter : SimpleItemRecyclerViewAdapter(true) {

    class DummyViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.favorites_dates_divider, parent, false))

    private val game = 1
    private val gameDateDivider = 2

    private val dummy = Quiz()

    override fun setValues(values: List<Quiz>) {
        var dividerAdded = false
        val games = ArrayList<Quiz>()
        for (q in values) {
            if (!dividerAdded && isLast(q.getDate())) {
                games.add(dummy)
                dividerAdded = true
            }
            games.add(q)
        }
        super.setValues(games)
    }

    override fun removeItem(item: Quiz) {
        val isLast = isLast(item)

        super.removeItem(item)

        if(isLast && !hasLast()){
            removeItem(dummy)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val q = getItem(position)
        if (q == dummy) {
            return gameDateDivider
        }

        return game
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            game -> super.onCreateViewHolder(parent, viewType)
            else -> return DummyViewHolder(parent)
        }
    }

    private fun hasLast():Boolean {
        for (q in getValues()) {
            if (isLast(q)) {
                return true
            }
        }

        return false
    }

    private fun isLast(q: Quiz) = q != dummy && isLast(q.getDate())
/*

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        super.onBindViewHolder(holder, position)

        if (holder is ViewHolder) {
            val item = getItem(position)
            if (isLast(item.getDate())) {
                holder.title.setTextColor(getColor(holder.timeImg.context, R.color.dark_grey))
                holder.theme.setTextColor(getColor(holder.timeImg.context, R.color.dark_grey))

            } else {
                holder.title.setTextColor(getColor(holder.timeImg.context, R.color.black))
                holder.theme.setTextColor(getColor(holder.timeImg.context, R.color.black))
            }
        }
    }*/

}

//--------------------------------------------------------------------------------------------
@Suppress("DEPRECATION")
@InjectViewState
class FavoritesPresenter : MvpPresenter<FavoritesView>() {
    //----------------------------------------------------------------------------------------------
    companion object {

        private val TAG: String = FavoritesPresenter::class.java.name
        private fun log(msg: String) {
            QuizPlanner.log(TAG, msg)
        }

        private const val BD_ERROR_MESSAGE = "Внутренняя ошибка"

    }
    //----------------------------------------------------------------------------------------------

    private var dataLoader: RetrofitService? = null
    private var dao: Db.DAO? = null

    private val allGames = ArrayList<Quiz>()
    private var selectedQuiz: Quiz? = null

    private var disposable: Disposable? = null

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
        disposable?.dispose()
    }

    fun start() {
        disposable = Observable.create<List<Quiz>> { it.onNext(loadFromDb()) }
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

        allGames.addAll(ArrayList(games.sortedWith(kotlin.Comparator { o1, o2 -> compareGames(o1, o2) })))
    }

    private fun compareGames(quiz1: Quiz, quiz2: Quiz): Int {
        if (isLast(quiz1.getDate()) && isLast(quiz2.getDate())) {
            return quiz2.getDate().compareTo(quiz1.getDate())
        }

        if (isLast(quiz1.getDate())) {
            return 1
        }

        if (isLast(quiz2.getDate())) {
            return -1
        }

        return quiz1.getDate().compareTo(quiz2.getDate())
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

