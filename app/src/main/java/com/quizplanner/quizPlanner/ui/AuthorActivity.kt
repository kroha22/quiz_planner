package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.databinding.ActivityFavoritesBinding
import com.quizplanner.quizPlanner.exchange.Input
import com.quizplanner.quizPlanner.exchange.RetrofitService
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import io.reactivex.android.schedulers.AndroidSchedulers
import moxy.InjectViewState
import moxy.MvpAppCompatActivity
import moxy.MvpPresenter
import moxy.MvpView
import moxy.presenter.InjectPresenter
import moxy.viewstate.strategy.AddToEndSingleStrategy
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

//---------------------------------------------------------------------------------------------

interface AuthorView : MvpView {

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
class AuthorActivity : MvpAppCompatActivity(), AuthorView, SimpleItemRecyclerViewAdapter.ItemClickListener {

    companion object {
        const val AUTHOR_CODE = "author_code"
        private const val AUTHOR: String = "author"
    }

    private val adapter = SimpleItemRecyclerViewAdapter(true)
    private lateinit var author: String

    @InjectPresenter(tag = AUTHOR)
    lateinit var presenter: AuthorPresenter
    
    private lateinit var binding: ActivityFavoritesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityFavoritesBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        
        setSupportActionBar(binding.detailToolbar)
        binding.detailToolbar.setNavigationOnClickListener { this.onBackPressed() }

        adapter.setOnClickListener(this)

        binding.quizList.quizList.adapter = adapter
        binding.quizList.quizListEmptyView.text = getText(R.string.favorites_empty)
        refreshView()

        if (savedInstanceState == null) {
            author = intent.getStringExtra(AUTHOR_CODE)
                    ?: throw AssertionError()
            binding.detailToolbar.title = getString(R.string.all_games)
        }
    }

    override fun onResume() {
        super.onResume()

        if (!presenter.isStarted()) {
            presenter.init(this)

            presenter.start(author)
        } else {
            presenter.onResume()
        }
    }

    override fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun setContent(items: List<Quiz>) {
        adapter.setValues(items)
        refreshView()
    }

    override fun showLoadProgress() {
        binding.quizList.quizListEmptyView.visibility = View.GONE
        binding.quizList.quizList.visibility = View.GONE
        binding.waitView.visibility = View.VISIBLE
    }

    override fun hideLoadProgress() {
        binding.quizList.quizListEmptyView.visibility = View.VISIBLE
        binding.quizList.quizList.visibility = View.VISIBLE
        binding.waitView.visibility = View.GONE
    }

    override fun onItemClick(quiz: Quiz) {
        presenter.onGameSelected(quiz)
    }

    override fun onItemCheckChanged(quiz: Quiz) {
        presenter.onGameCheckChanged(quiz)
    }

    override fun showQuizView(quiz: Quiz) {
        val intent = Intent(this, QuizDetailActivity::class.java).apply {
            putExtra(QuizDetailActivity.QUIZ_ITEM_CODE, quiz)
            putExtra(QuizDetailActivity.SOURCE_CODE, QuizDetailActivity.AUTHOR)
        }
        startActivity(intent)
    }

    private fun refreshView() {
        if (adapter.isEmpty()) {
            binding.quizList.quizListEmptyView.visibility = View.VISIBLE
            binding.quizList.quizList.visibility = View.INVISIBLE
        } else {
            binding.quizList.quizListEmptyView.visibility = View.INVISIBLE
            binding.quizList.quizList.visibility = View.VISIBLE
        }
    }
}
//--------------------------------------------------------------------------------------------


@Suppress("DEPRECATION")
@InjectViewState
class AuthorPresenter : MvpPresenter<AuthorView>() {
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

    private lateinit var author: String

    private val allGames = ArrayList<Quiz>()
    private var selectedQuiz: Quiz? = null

    private var disposable: Disposable? = null

    private var isStarted = false

    override fun onFirstViewAttach() {
        super.onFirstViewAttach()
        viewState.showLoadProgress()
    }

    fun init(context: Context) {
        this.dataLoader = RetrofitService.getInstance(context)
        this.dao = Db.DAO(context)
    }

    fun start(author: String) {
        this.author = author
        isStarted = true

        disposable = Observable.create<List<Quiz>> { it.onNext(loadFromDb(author)) }
                .subscribeOn(Schedulers.io())
                .doOnError { onBdError(it) }
                .doOnNext { setGames(it) }
                .flatMap { games ->
                    if (games.isNotEmpty()) {
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

    fun onResume() {
        disposable = Observable.create<List<Quiz>> { it.onNext(loadFromDb(author)) }
                .subscribeOn(Schedulers.io())
                .map { setGames(it) }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ needUpdateView ->
                    if (needUpdateView) {
                        showGames()
                    }
                }, {
                    onBdError(it)
                })
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable?.dispose()
    }

    fun isStarted() = isStarted

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

    private fun loadFromDb(author: String): List<Quiz> {
        return dao!!.getGames(author)
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

    private fun setGames(games: List<Quiz>): Boolean {
        if (isIdentical(allGames, games)) {
            return false
        }

        allGames.clear()
        allGames.addAll(ArrayList(games.sortedWith(kotlin.Comparator { quiz1, quiz2 -> quiz1.getDate().compareTo(quiz2.getDate()) })))
        return true
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

