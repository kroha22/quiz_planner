package com.quizplanner.quizPlanner.ui

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.CalendarContract.Events
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.quizplanner.quizPlanner.HttpHelper
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterTime
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.databinding.ActivityQuizDetailBinding
import com.quizplanner.quizPlanner.databinding.QuizDetailBinding
import com.quizplanner.quizPlanner.model.Db
import com.quizplanner.quizPlanner.model.Quiz
import com.quizplanner.quizPlanner.player.YouTubePlayer
import com.quizplanner.quizPlanner.player.listeners.AbstractYouTubePlayerListener
import com.quizplanner.quizPlanner.player.ui.views.YouTubePlayerView
import com.squareup.picasso.Picasso
import moxy.InjectViewState
import moxy.MvpAppCompatActivity
import moxy.MvpPresenter
import moxy.MvpView
import moxy.presenter.InjectPresenter
import moxy.viewstate.strategy.SkipStrategy
import moxy.viewstate.strategy.StateStrategyType
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

    private lateinit var binding: ActivityQuizDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityQuizDetailBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.detailToolbar)
        binding.detailToolbar.setNavigationOnClickListener { this.onBackPressed() }

        if (savedInstanceState == null) {
            val item = intent.getSerializableExtra(QUIZ_ITEM_CODE)
                    ?: throw AssertionError()

            this.item = item as Quiz

            binding.quizDetail.detailTitle.text = item.organisationName
            binding.quizDetail.detailTheme.text = item.gameTheme
            binding.quizDetail.detailDate.text = QuizPlanner.formatterDateMonth.format(item.date)
            binding.quizDetail.detailTime.text = formatterTime.format(item.date)

            binding.quizDetail.detailLocation.text = if (item.isOnlineGame()) {
                binding.quizDetail.detailLocation.context.getString(R.string.online)
            } else {
                item.location
            }

            binding.quizDetail.detailOnline.visibility = if (item.isOnlineGame()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            if (item.countOfPlayers != null) {
                binding.quizDetail.detailCount.text = item.countOfPlayers.toString()
                binding.quizDetail.detailCount.visibility = View.VISIBLE
            } else {
                binding.quizDetail.detailCount.visibility = View.GONE
            }

            binding.quizDetail.detailPrice.text = item.price.toString()
            binding.quizDetail.detailDescription.text = item.description

            if (item.registrationLink!!.isEmpty()) {
                binding.quizDetail.detailLinkLabel.visibility = View.INVISIBLE
                binding.quizDetail.detailLink.visibility = View.INVISIBLE
            } else {
                binding.quizDetail.detailLinkLabel.visibility = View.VISIBLE
                binding.quizDetail.detailLink.visibility = View.VISIBLE
                binding.quizDetail.detailLink.text = item.registrationLink
                binding.quizDetail.detailLink.setOnClickListener { presenter.onLinkClick() }
                binding.quizDetail.detailLink.setOnLongClickListener {
                    setClipboard(binding.quizDetail.detailLink.text.toString())
                    Toast.makeText(this, getString(R.string.copy), Toast.LENGTH_SHORT).show()
                    true
                }
            }

            if (!item.getImgUrl().isEmpty()) {
                val apiUrl = getString(R.string.base_api_img_url)

                Picasso.get()
                        .load(apiUrl + item.getImgUrl())
                        .placeholder(R.drawable.ic_image_placeholder)
                        .fit()
                        .centerInside()
                        .error(R.drawable.ic_broken_image)
                        .into(binding.quizDetail.detailImg)
            }

            binding.toolbarCalendar.setOnClickListener { presenter.onCalendarClick() }
            binding.quizDetail.detailAuthorGames.setOnClickListener { presenter.onAuthorClick() }

            updateFavoritesView()

            binding.quizDetail.detailFavorites.setOnClickListener {
                presenter.onGameCheckChanged()
            }

            if (QuizPlanner.isLast(item.getDate()) || item.isGamePostponed()) {
                binding.quizDetail.detailTimeImg.setColorFilter(ContextCompat.getColor(this, R.color.medium_grey))
            } else {
                binding.quizDetail.detailTimeImg.setColorFilter(ContextCompat.getColor(this, R.color.red))
            }

            if (item.isGamePostponed()) {
                binding.quizDetail.detailPostponed.visibility = View.VISIBLE
            } else {
                binding.quizDetail.detailPostponed.visibility = View.GONE
            }

            if (intent.getStringExtra(SOURCE_CODE) == AUTHOR) {
                binding.quizDetail.detailAuthorGames.visibility = View.GONE
            }

            initLinks(item)
        }

    }

    override fun inverseFavorites(quiz: Quiz) {
        item.isChecked = quiz.isChecked
        updateFavoritesView()
    }

    override fun updateFavoritesView() {
        if (item.isChecked) {
            binding.quizDetail.detailFavoritesText.text = getText(R.string.in_favorites)
            binding.quizDetail.detailFavoritesImg.setColorFilter(ContextCompat.getColor(this, R.color.colorAccent))
        } else {
            binding.quizDetail.detailFavoritesText.text = getText(R.string.add_to_favorites)
            binding.quizDetail.detailFavoritesImg.colorFilter = null
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

    private fun initLinks(item: Quiz) {
        var videoLink: String? = null

        if (item.description != null) {

            val allLinks = HttpHelper.findUrl(item.description!!)
            if (allLinks.isNotEmpty()) {
                val ss = SpannableString(item.description)

                for (link in HttpHelper.findUrl(item.description!!)) {
                    if (link.isNotEmpty()) {
                        if (HttpHelper.isVideoUrl(link)) {
                            videoLink = link
                        } else {
                            val clickableSpan: ClickableSpan = object : ClickableSpan() {
                                override fun onClick(textView: View) {
                                    requestLink(link)
                                }

                                override fun updateDrawState(ds: TextPaint) {
                                    super.updateDrawState(ds)
                                    ds.isUnderlineText = false
                                }
                            }

                            val start = item.description!!.indexOf(link)

                            ss.setSpan(clickableSpan, start, start + link.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        }
                    }
                }

                binding.quizDetail.detailDescription.text = ss
                binding.quizDetail.detailDescription.movementMethod = LinkMovementMethod.getInstance()
                binding.quizDetail.detailDescription.highlightColor = Color.TRANSPARENT
            }
        }

        initVideoView(videoLink)
    }

    private fun initVideoView(videoLink: String?) {
        val youTubePlayerView = findViewById<YouTubePlayerView>(R.id.youtube_player_view)

        if (videoLink != null && videoLink.isNotEmpty()) {
            val videoId = HttpHelper.extractVideoIdFromUrl(videoLink)

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

        youTubePlayerView.visibility = View.GONE
    }

    private fun setClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = android.content.ClipData.newPlainText(getString(R.string.app_name), text)
        clipboard.setPrimaryClip(clip)
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
