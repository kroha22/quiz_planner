package com.quizplanner.quizPlanner.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.*
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.QuizPlanner.formatterDate
import com.quizplanner.quizPlanner.QuizPlanner.formatterDay
import com.quizplanner.quizPlanner.QuizPlanner.formatterMonth
import com.quizplanner.quizPlanner.QuizPlanner.isOneDay
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Filter
import com.quizplanner.quizPlanner.model.Quiz
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.LinkedHashMap


/**
 * Created by Olga Cherepanova
 * on 20.01.2019.
 */

//---------------------------------------------------------------------------------------------

interface MainView : MvpView {

    companion object {
        const val PROGRESS_TAG = "PROGRESS_TAG"
    }

    @StateStrategyType(SkipStrategy::class)
    fun requestLink(link: String)

    @StateStrategyType(SkipStrategy::class)
    fun requestEmail(mail: String)

    @StateStrategyType(SkipStrategy::class)
    fun showMessage(msg: String)

    @StateStrategyType(SkipStrategy::class)
    fun showDialog(dialogBuilder: DialogBuilder)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setContent(gamesByDate: LinkedHashMap<Date, List<Quiz>>, selectedDate: Date)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showQuizView(quiz: Quiz)

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun showLoadProgress()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun hideLoadProgress()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun showStartLoad()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun hideStartLoad()

    @StateStrategyType(value = AddToEndSingleStrategy::class)
    fun showCheckedGames()

    @StateStrategyType(value = AddToEndSingleStrategy::class)
    fun showContacts()

}

//---------------------------------------------------------------------------------------------
private const val MAIN: String = "MainActivity"
//-------------------------------------------------------------------------------------------

class MainActivity : MvpAppCompatActivity(), MainView, SimpleItemRecyclerViewAdapter.ItemClickListener, RecyclerViewScrollListener, TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter
    private lateinit var slideDownAnimation: Animation
    private lateinit var slideLeftAnimation: Animation
    private lateinit var slideRightAnimation: Animation

    @InjectPresenter(tag = MAIN)
    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        for (f in supportFragmentManager.fragments) {
            supportFragmentManager.beginTransaction().remove(f).commit()
        }

        setSupportActionBar(toolbar)

        sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        container.adapter = sectionsPagerAdapter

        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.setupWithViewPager(container)
        tabs.isSmoothScrollingEnabled = true

        slideDownAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_down_animation)
        slideLeftAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_left_animation)
        slideRightAnimation = AnimationUtils.loadAnimation(this, R.anim.slide_right_animation)

        filters_view.consumer = { applyFilters(it) }

        filters_view.onClose = { hideFilters() }

        filters_view_container.visibility = View.GONE

        toolbar_filter.setOnClickListener {
            if(filters_view_container.visibility == View.VISIBLE){
                filters_view.close()
            } else {
                showFilters()
            }

        }

        toolbar_filter_check.visibility = View.GONE
    }

    override fun onResume() {
        super.onResume()

        QuizPlanner.log("!!!", "onResume presenter.isInitialized ${presenter.isInitialized()}")

        if (!presenter.isInitialized()) {
            presenter.init(this)
            presenter.setEscapeHandler { finish() }
        }
        presenter.start()
        tabs.addOnTabSelectedListener(this)
    }

    override fun onPause() {
        super.onPause()
        tabs.removeOnTabSelectedListener(this)
    }

    override fun onBackPressed() {
        if(filters_view_container.visibility == View.VISIBLE){
            filters_view.close()
            return
        }
        super.onBackPressed()
    }

    override fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    override fun requestEmail(mail: String) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/html"
        intent.putExtra(Intent.EXTRA_EMAIL, mail)

        startActivity(Intent.createChooser(intent, "Send Email"))
    }

    override fun showQuizView(quiz: Quiz) {
        val intent = Intent(this, QuizDetailActivity::class.java).apply {
            putExtra(QuizDetailActivity.QUIZ_ITEM_CODE, quiz)
            putExtra(QuizDetailActivity.SOURCE_CODE, QuizDetailActivity.MAIN)
        }
        startActivity(intent)
    }

    override fun showMessage(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    override fun showDialog(dialogBuilder: DialogBuilder) {
        dialogBuilder.show(this)
    }

    override fun showLoadProgress() {
        wait_view.visibility = View.VISIBLE
        wait_view.startAnimation(slideDownAnimation)
    }

    override fun hideLoadProgress() {
        wait_view.visibility = View.GONE
    }

    override fun onLoadMore() {
        presenter.onRefreshClick()
    }

    override fun showStartLoad() {
        start_load_view.visibility = View.VISIBLE
        appbar.visibility = View.GONE
    }

    override fun hideStartLoad() {
        start_load_view.visibility = View.GONE
        appbar.visibility = View.VISIBLE
    }

    override fun showCheckedGames() {
        val intent = Intent(this, FavoritesActivity::class.java)
        startActivity(intent)
    }

    @SuppressLint("InflateParams")
    override fun showContacts() {
        val view = layoutInflater.inflate(R.layout.contacts_view, null, false) as LinearLayout
        view.findViewById<TextView>(R.id.email).apply {
            setOnClickListener { requestEmail(text.toString()) }
        }

        AlertDialog.Builder(this)
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.contacts)
                .setView(view)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .setCancelable(true)
                .create()
                .show()
    }

    override fun setContent(gamesByDate: LinkedHashMap<Date, List<Quiz>>, selectedDate: Date) {

        val isEmpty = sectionsPagerAdapter.count == 0
        if (sectionsPagerAdapter.setItems(gamesByDate)) {

            for (i in 0..tabs.tabCount) {
                val tab = tabs.getTabAt(i)
                if (tab != null) {
                    tab.customView = sectionsPagerAdapter.getTabView(i)

                    if (isOneDay(sectionsPagerAdapter.getItemDate(i), selectedDate)) {
                        Handler().postDelayed({
                            container.setCurrentItem(i, isEmpty)
                        }, 200)
                    }
                }
            }
        }
    }

    override fun onItemClick(quiz: Quiz) {
        presenter.onGameSelected(quiz)
    }

    override fun onItemCheckChanged(quiz: Quiz) {
        presenter.onGameCheckChanged(quiz)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    @SuppressLint("InflateParams")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == R.id.action_contacts) {
            presenter.onContactsRequested()
            return true
        } else if (id == R.id.action_favorites) {
            presenter.onCheckedGamesRequested()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onTabReselected(p0: TabLayout.Tab) {

    }

    override fun onTabUnselected(p0: TabLayout.Tab) {

    }

    override fun onTabSelected(p0: TabLayout.Tab) {
        val pos = p0.position
        val date = sectionsPagerAdapter.getItemDate(pos)
        presenter.onDateSelect(date)
        toolbar_month.text = formatterMonth.format(date)
    }

    private fun applyFilters(it: List<Filter>) {
        sectionsPagerAdapter.filter(it)

        if(it.isEmpty() || it.containsAll(Filter.values().asList())){
            toolbar_filter_check.visibility = View.GONE
        } else {
            toolbar_filter_check.visibility = View.VISIBLE
        }

    }

    private fun showFilters() {
        filters_view_container.visibility = View.VISIBLE
        filters_view_container.startAnimation(slideLeftAnimation)
    }

    private fun hideFilters() {
        filters_view_container.startAnimation(slideRightAnimation)

        filters_view_container.visibility = View.GONE
    }
//------------------------------------------------------------------------------------------------

    inner class SectionsPagerAdapter(context: Context, fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val filters = arrayListOf<Filter>()
        private val tabItems: MutableList<Date> = ArrayList()
        private var gamesByDate: MutableMap<Date, List<Quiz>> = LinkedHashMap()
        private val pages: MutableMap<Date, DateFragment> = LinkedHashMap()
        private val textColors: ColorStateList

        init {
            val states = arrayOf(intArrayOf(android.R.attr.state_enabled), // enabled
                    intArrayOf(-android.R.attr.state_enabled) // disabled
            )

            val colors = intArrayOf(
                    ContextCompat.getColor(context, R.color.white),
                    ContextCompat.getColor(context, R.color.medium_grey)
            )

            textColors = ColorStateList(states, colors)
        }

        // return true if tabs need update
        fun setItems(items: LinkedHashMap<Date, List<Quiz>>): Boolean {
            if (tabItems.map { formatterDate.format(it) } == items.keys.map { formatterDate.format(it) }) {

                gamesByDate = items

                for (page in pages.entries) {
                    page.value.setValues(getGames(page.key)!!)
                }

                return false
            }

            tabItems.clear()
            tabItems.addAll(items.keys)

            gamesByDate = items

            for (date in ArrayList(pages.keys)) {
                if (!tabItems.contains(date)) {
                    pages.remove(date)
                }
            }

            for (page in pages.entries) {
                page.value.setValues(getGames(page.key)!!)
            }

            notifyDataSetChanged()
            return true
        }

        fun getItemDate(position: Int): Date {
            return tabItems[position]
        }

        override fun getItem(position: Int): Fragment {
            val fragment = DateFragment()

            val date = tabItems[position]
            fragment.setValues(getGames(date)!!)

            pages[date] = fragment

            return fragment
        }

        override fun getCount(): Int {
            return tabItems.size
        }

        @SuppressLint("InflateParams")
        fun getTabView(position: Int): View {
            val view = LayoutInflater.from(this@MainActivity).inflate(R.layout.layout_custom_tab, null)

            val title = view.findViewById<TextView>(R.id.tab_title)
            val subtitle = view.findViewById<TextView>(R.id.tab_sub_title)

            val item = tabItems[position]

            title.text = formatterDate.format(item)
            subtitle.text = formatterDay.format(item)

            title.setTextColor(textColors)
            subtitle.setTextColor(textColors)

            val date = tabItems[position]
            val isEnabled = getGames(date)?.isNotEmpty() == true
            title.isEnabled = isEnabled
            subtitle.isEnabled = isEnabled
            view.isClickable = !isEnabled

            return view
        }

        private fun getGames(forDate: Date): List<Quiz>? {
            if (filters.isEmpty()) {
                return gamesByDate[forDate]
            }

            val games = arrayListOf<Quiz>()

            for (game in gamesByDate[forDate] ?: emptyList()) {

                if (filters.contains(Filter.ONLINE) && game.isOnlineGame()) {
                    games.add(game)
                } else if (filters.contains(Filter.OFFLINE) && !game.isOnlineGame()) {
                    games.add(game)
                }

            }

            return games
        }

        fun filter(filter: List<Filter>) {
            Log.d("!!!!!", "set ${filter.joinToString( )} ")

            if (filters.containsAll(filter) && filter.containsAll(filters)) {
                Log.d("!!!!!", "not changed, current ${filters.joinToString( )} ")
                return
            }

            filters.clear()
            filters.addAll(filter)

            Log.d("!!!!!", "apply, new ${filters.joinToString( )} ")

            for (page in pages.entries) {
                page.value.setValues(getGames(page.key)!!)
            }

        }

    }

}

//------------------------------------------------------------------------------------------------
interface RecyclerViewScrollListener {
    fun onLoadMore()
}
//------------------------------------------------------------------------------------------------

class DateFragment : Fragment() {
    private var isCreated = false
    private val adapter = SimpleItemRecyclerViewAdapter(false)
    private var clickListener: SimpleItemRecyclerViewAdapter.ItemClickListener? = null
    private var endlessRecyclerViewScrollListener: RecyclerViewScrollListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var emptyView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val mainView = inflater.inflate(R.layout.quiz_list, container, false) as RelativeLayout
        recyclerView = mainView.findViewById(R.id.quiz_list)
        emptyView = mainView.findViewById(R.id.quiz_list_empty_view)

        adapter.setOnClickListener(clickListener)
        recyclerView.adapter = adapter
        isCreated = true

        linearLayoutManager = recyclerView.layoutManager as LinearLayoutManager

        return mainView
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setValues(values: List<Quiz>) {
        adapter.setValues(values)

        if (isCreated) {
            refreshView()
        }
    }

    override fun onResume() {
        super.onResume()
        QuizPlanner.log("!!!", "DateFragment onResume")

        refreshView()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            clickListener = context as SimpleItemRecyclerViewAdapter.ItemClickListener
            endlessRecyclerViewScrollListener = context as RecyclerViewScrollListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement OnListItemSelectedListener")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun refreshView() {
        QuizPlanner.log("!!!", "DateFragment refreshView adapter.isEmpty() ${adapter.isEmpty()}")
        if (adapter.isEmpty()) {
            emptyView.visibility = View.VISIBLE
        } else {
            emptyView.visibility = View.INVISIBLE
        }

        recyclerView.setOnTouchListener(
                ListScrollListener(
                        recyclerView,
                        linearLayoutManager
                ) { endlessRecyclerViewScrollListener?.onLoadMore() })
    }

    class ListScrollListener(private val recyclerView: RecyclerView,
                             private val linearLayoutManager: LinearLayoutManager,
                             private val onLoadMore: () -> Unit) : View.OnTouchListener {

        private val maxY
            get() = recyclerView.height
        private val deltaToRequestLoad
            get() = maxY / 3
        private var lastTouchY = -1
        private var startScrollY = -1

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {

            val isEmpty = recyclerView.adapter!!.itemCount == 0
            val isFirstItemPositionVisible = linearLayoutManager.findFirstCompletelyVisibleItemPosition() == 0

            if (!isEmpty && !isFirstItemPositionVisible) {
                resetStartScrollY()
                resetLastTouchY()
                return recyclerView.onTouchEvent(event)
            }

            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    if (isScrollDown((event.y).toInt())) {

                        if (startScrollY == -1) {
                            startScrollY = (event.y).toInt()
                        } else if (needLoadMore((event.y).toInt())) {
                            onLoadMore.invoke()
                            return true
                        }
                    } else {
                        lastTouchY = (event.y).toInt()
                        resetStartScrollY()
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    resetLastTouchY()
                    resetStartScrollY()
                }
            }

            return recyclerView.onTouchEvent(event)
        }

        private fun resetStartScrollY() {
            startScrollY = -1
        }

        private fun resetLastTouchY() {
            lastTouchY = -1
        }

        private fun needLoadMore(y: Int) = ((y - startScrollY) > deltaToRequestLoad)

        private fun isScrollDown(y: Int) = (y > lastTouchY)

    }
}