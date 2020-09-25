package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.support.v4.content.ContextCompat
import android.view.View
import com.arellomobile.mvp.InjectViewState
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpPresenter
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterTime
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import com.quizplanner.quizPlanner.player.YouTubeHelper
import com.quizplanner.quizPlanner.player.YouTubePlayer
import com.quizplanner.quizPlanner.player.listeners.AbstractYouTubePlayerListener
import com.quizplanner.quizPlanner.player.ui.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_quiz_detail.*
import kotlinx.android.synthetic.main.quiz_detail.*
import rx.Subscription


//---------------------------------------------------------------------------------------------

@StateStrategyType(SkipStrategy::class)
interface QuizDetailView : MvpView {
    fun requestLink(link: String)

    fun createNotify(quiz: Quiz)

    fun updateFavoritesView()

    fun inverseFavorites(quiz: Quiz)

    fun showAuthorGames()
}

//---------------------------------------------------------------------------------------------

class QuizDetailActivity : MvpAppCompatActivity(), QuizDetailView {

    companion object {
        const val QUIZ_ITEM_CODE = "quiz_item"
        const val SOURCE_CODE = "source_code"
        const val MAIN = "main"
        const val AUTHOR = "author"
        private const val QUIZ_DETAIL: String = "quiz_detail"
    }

    private lateinit var item: Quiz

    @InjectPresenter(tag = QUIZ_DETAIL)
    lateinit var presenter: QuizDetailPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz_detail)
        setSupportActionBar(detail_toolbar)
        detail_toolbar.setNavigationOnClickListener { this.onBackPressed() }

        if (savedInstanceState == null) {
            val item = intent.getSerializableExtra(QUIZ_ITEM_CODE)
                    ?: throw AssertionError()

            this.item = item as Quiz

            detail_title.text = item.organisationName
            detail_theme.text = item.gameTheme
            detail_date.text = QuizPlanner.formatterDateMonth.format(item.date)
            detail_time.text = formatterTime.format(item.date)

            detail_location.text = if(item.isOnlineGame()){
                detail_location.context.getString(R.string.online)
            } else {
                item.location
            }

            detail_online.visibility = if(item.isOnlineGame()){
                View.VISIBLE
            } else {
                View.GONE
            }

            if (item.countOfPlayers != null) {
                detail_count.text = item.countOfPlayers.toString()
                detail_count.visibility = View.VISIBLE
            } else {
                detail_count.visibility = View.GONE
            }

            detail_price.text = item.price.toString()
            detail_description.text = item.description

            if (item.registrationLink!!.isEmpty()) {
                detail_link_label.visibility = View.INVISIBLE
                detail_link.visibility = View.INVISIBLE
            } else {
                detail_link_label.visibility = View.VISIBLE
                detail_link.visibility = View.VISIBLE
                detail_link.text = item.registrationLink
                detail_link.setOnClickListener { presenter.onLinkClick() }
            }

            if (!item.getImgUrl().isEmpty()) {
                val apiUrl = getString(R.string.base_api_img_url)

                Picasso.get()
                        .load(apiUrl + item.getImgUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .fit()
                        .centerInside()
                        .error(R.drawable.ic_broken_image)
                        .into(detail_img)
            }

            toolbar_calendar.setOnClickListener { presenter.onCalendarClick() }
            detail_author_games.setOnClickListener { presenter.onAuthorClick() }

            updateFavoritesView()

            detail_favorites.setOnClickListener {
                presenter.onGameCheckChanged()
            }

            if (QuizPlanner.isLast(item.getDate()) || item.isGamePostponed()) {
                detail_time_img.setColorFilter(ContextCompat.getColor(this, R.color.medium_grey))
            } else {
                detail_time_img.setColorFilter(ContextCompat.getColor(this, R.color.red))
            }

            if(item.isGamePostponed()){
                detail_postponed.visibility = View.VISIBLE
            } else {
                detail_postponed.visibility = View.GONE
            }

            if (intent.getStringExtra(SOURCE_CODE) == AUTHOR) {
                detail_author_games.visibility = View.GONE
            }

            initVideoView(item)
        }

    }

    override fun inverseFavorites(quiz: Quiz) {
        item.isChecked = quiz.isChecked
        updateFavoritesView()
    }

    override fun updateFavoritesView() {
        if (item.isChecked) {
            detail_favorites_text.text = getText(R.string.in_favorites)
            detail_favorites_img.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            detail_favorites_text.text = getText(R.string.add_to_favorites)
            detail_favorites_img.colorFilter = null
        }
    }

    override fun onResume() {
        super.onResume()

        if (presenter.isStarted()) {
            presenter.onResume()
            return
        }
        presenter.init(this)
        presenter.start(item)
    }

    override fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    override fun createNotify(quiz: Quiz) {
        val intent = Intent(Intent.ACTION_INSERT)
                .setData(Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, quiz.date)
                .putExtra(Events.TITLE, quiz.gameTheme)
                .putExtra(Events.DESCRIPTION, quiz.description)
                .putExtra(Events.EVENT_LOCATION, quiz.location)
                .putExtra(Intent.EXTRA_EMAIL, quiz.registrationLink)
        startActivity(intent)
    }

    override fun showAuthorGames() {
        val intent = Intent(this, AuthorActivity::class.java).apply {
            putExtra(AuthorActivity.AUTHOR_CODE, item.organisationName)
        }
        startActivity(intent)
    }

    private fun initVideoView(item: Quiz) {
        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)
        if (item.description != null) {

            val youTubeHelper = YouTubeHelper()
            val vid = youTubeHelper.findVideoUrl(item.description!!)

            if (vid != null && vid.isNotEmpty()) {
                val videoId = youTubeHelper.extractVideoIdFromUrl(vid)

                if (videoId != null && videoId.isNotEmpty()) {
                    youTubePlayerView.visibility = View.VISIBLE

                    lifecycle.addObserver(youTubePlayerView)

                    youTubePlayerView.addYouTubePlayerListener(object : AbstractYouTubePlayerListener() {
                        override fun onReady(youTubePlayer: YouTubePlayer) {
                            youTubePlayer.loadVideo(videoId, 0f)
                        }
                    })
                    return
                }
            }
        }

        youTubePlayerView.visibility = View.GONE
    }
}

//---------------------------------------------------------------------------------------------
@InjectViewState
class QuizDetailPresenter : MvpPresenter<QuizDetailView>() {

    private lateinit var quiz: Quiz
    private lateinit var dao: Db.DAO
    private var isStarted = false
    private var subscription: Subscription? = null

    fun init(context: Context) {
        this.dao = Db.DAO(context)
    }

    fun start(quiz: Quiz) {
        isStarted = true
        this.quiz = quiz
    }

    fun onResume() {
        val isCheckedNew = dao.isChecked(quiz.id!!)
        val isCheckedCurr = quiz.isChecked
        if (isCheckedNew != isCheckedCurr) {
            quiz.isChecked = isCheckedNew
            viewState.inverseFavorites(quiz)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        subscription?.unsubscribe()
    }

    fun onLinkClick() {
        var url = quiz.registrationLink!!
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://$url"
        }
        viewState.requestLink(url)
    }

    fun onCalendarClick() {
        viewState.createNotify(quiz)
    }

    fun onGameCheckChanged() {
        val isChecked = !quiz.isChecked
        quiz.isChecked = isChecked

        if (isChecked) {
            dao.setCheckedGame(quiz)
        } else {
            dao.setUncheckedGame(quiz)
        }

        viewState.updateFavoritesView()
    }

    fun onAuthorClick() {
        viewState.showAuthorGames()
    }

    fun isStarted() = isStarted


}
