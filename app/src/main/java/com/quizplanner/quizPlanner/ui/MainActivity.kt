package com.quizplanner.quizPlanner.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.*
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import com.arellomobile.mvp.MvpAppCompatActivity
import com.arellomobile.mvp.MvpView
import com.arellomobile.mvp.presenter.InjectPresenter
import com.arellomobile.mvp.viewstate.strategy.AddToEndSingleStrategy
import com.arellomobile.mvp.viewstate.strategy.SkipStrategy
import com.arellomobile.mvp.viewstate.strategy.StateStrategyType
import com.quizplanner.quizPlanner.QuizPlanner.formatterDate
import com.quizplanner.quizPlanner.QuizPlanner.formatterDay
import com.quizplanner.quizPlanner.QuizPlanner.formatterMonth
import com.quizplanner.quizPlanner.QuizPlanner.isOneDay
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Quiz
import com.quizplanner.quizPlanner.ui.QuizDetailActivity.Companion.QUIZ_ITEM_CODE
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.quiz_list_item.view.*
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
    fun showMessage(msg: String)

    @StateStrategyType(SkipStrategy::class)
    fun showDialog(dialogBuilder: DialogBuilder)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun setContent(dateByGames: LinkedHashMap<Date, List<Quiz>>, selectedDate: Date)

    @StateStrategyType(AddToEndSingleStrategy::class)
    fun showQuizView(quiz: Quiz)

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun showLoadProgress()

    @StateStrategyType(value = AddToEndSingleStrategy::class, tag = PROGRESS_TAG)
    fun hideLoadProgress()

}

//---------------------------------------------------------------------------------------------
private const val MAIN: String = "MainActivity"
//-------------------------------------------------------------------------------------------

class MainActivity : MvpAppCompatActivity(), MainView, DateFragment.ItemClickListener {

    private lateinit var sectionsPagerAdapter: SectionsPagerAdapter

    @InjectPresenter(tag = MAIN)
    lateinit var presenter: MainPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        for (f in supportFragmentManager.fragments) {
            supportFragmentManager.beginTransaction().remove(f).commit()
        }

        setSupportActionBar(toolbar)
        sectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.adapter = sectionsPagerAdapter
        container.addOnPageChangeListener(TabLayout.TabLayoutOnPageChangeListener(tabs))

        tabs.addOnTabSelectedListener(TabLayout.ViewPagerOnTabSelectedListener(container))
        tabs.setupWithViewPager(container)

        fab.setOnClickListener { presenter.onRefreshClick() }
    }

    override fun onResume() {
        super.onResume()

        if (!presenter.isInitialized()) {
            presenter.init(this)
            presenter.setEscapeHandler { finish() }
        }
        presenter.start()
    }

    override fun requestLink(link: String) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
        startActivity(browserIntent)
    }

    override fun showQuizView(quiz: Quiz) {
        val intent = Intent(this, QuizDetailActivity::class.java).apply {
            putExtra(QUIZ_ITEM_CODE, quiz)
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
    }

    override fun hideLoadProgress() {
        wait_view.visibility = View.GONE
    }

    override fun setContent(dateByGames: LinkedHashMap<Date, List<Quiz>>, selectedDate: Date) {
        sectionsPagerAdapter.setItems(dateByGames)

        for (i in 0..tabs.tabCount) {
            val tab = tabs.getTabAt(i)
            if (tab != null) {
                tab.customView = sectionsPagerAdapter.getTabView(i)

                if (isOneDay(sectionsPagerAdapter.getItemDate(i), selectedDate)) {
                    tab.select()
                }
            }
        }

        tabs.addOnTabSelectedListener(object : TabLayout.BaseOnTabSelectedListener<TabLayout.Tab> {
            val dates = ArrayList(dateByGames.keys)

            override fun onTabReselected(p0: TabLayout.Tab) {

            }

            override fun onTabUnselected(p0: TabLayout.Tab) {

            }

            override fun onTabSelected(p0: TabLayout.Tab) {
                val pos = p0.position
                presenter.onDateSelect(dates[pos])
            }
        })

    }

    override fun onItemClick(quiz: Quiz) {
        presenter.onGameSelected(quiz)
    }

    override fun onItemCheckChanged(quiz: Quiz) {
        presenter.onGameCheckChanged(quiz)
    }

    override fun onLinkClick(quiz: Quiz) {
        presenter.onLinkClick(quiz)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId

        if (id == R.id.action_settings) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
    //------------------------------------------------------------------------------------------------

    inner class SectionsPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

        private val tabItems: MutableList<Date> = ArrayList()
        private var gamesByDate: MutableMap<Date, List<Quiz>> = LinkedHashMap()
        private val pages: MutableMap<Date, DateFragment> = LinkedHashMap()

        fun setItems(items: LinkedHashMap<Date, List<Quiz>>) {
            tabItems.clear()
            tabItems.addAll(items.keys)

            gamesByDate = items

            for (date in ArrayList(pages.keys)) {
                if (!tabItems.contains(date)) {
                    pages.remove(date)
                }
            }

            for (page in pages.entries) {
                page.value.setValues(gamesByDate[page.key]!!)
            }

            notifyDataSetChanged()
        }

        fun getItemDate(position: Int): Date {
            return tabItems[position]
        }

        override fun getItem(position: Int): Fragment {
            val fragment = DateFragment()

            val date = tabItems[position]
            fragment.setValues(gamesByDate[date]!!)

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

            toolbar_month.text = formatterMonth.format(item)

            return view
        }
    }

}
//------------------------------------------------------------------------------------------------

class DateFragment : Fragment() {
    //------------------------------------------------------------------------------------------------
    interface ItemClickListener {
        fun onItemClick(quiz: Quiz)

        fun onItemCheckChanged(quiz: Quiz)

        fun onLinkClick(quiz: Quiz)
    }

    //------------------------------------------------------------------------------------------------
    private var isCreated = false
    private val adapter = SimpleItemRecyclerViewAdapter()
    private var clickListener: ItemClickListener? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val mainView = inflater.inflate(R.layout.quiz_list, container, false) as RelativeLayout
        recyclerView = mainView.findViewById(R.id.quiz_list)
        emptyView = mainView.findViewById(R.id.quiz_list_empty_view)

        adapter.setOnClickListener(clickListener)
        recyclerView.adapter = adapter
        isCreated = true
        return mainView
    }

    fun setValues(values: List<Quiz>) {
        adapter.setValues(values)

        if (isCreated) {
            refreshView()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshView()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        try {
            clickListener = context as ItemClickListener
        } catch (e: ClassCastException) {
            throw ClassCastException(context.toString() + " must implement OnListItemSelectedListener")
        }
    }

    private fun refreshView() {
        if (adapter.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            recyclerView.visibility = View.INVISIBLE
        } else {
            emptyView.visibility = View.INVISIBLE
            recyclerView.visibility = View.VISIBLE
        }
    }

    //------------------------------------------------------------------------------------------------
    class SimpleItemRecyclerViewAdapter :
            RecyclerView.Adapter<SimpleItemRecyclerViewAdapter.ViewHolder>() {

        private var onClickListener: ItemClickListener? = null
        private var values: List<Quiz> = ArrayList()

        fun setValues(values: List<Quiz>) {
            this.values = values
            notifyDataSetChanged()
        }

        fun isEmpty(): Boolean {
            return values.isEmpty()
        }

        fun setOnClickListener(onClickListener: ItemClickListener?) {
            this.onClickListener = onClickListener
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.quiz_list_item, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = values[position]
            holder.title.text = item.organization
            holder.theme.text = item.gameTheme
            holder.location.text = item.location
            holder.price.text = item.price
            holder.difficulty.text = item.difficulty
            holder.count.text = item.countOfPlayers
            holder.time.text = item.time

            if (item.registrationLink.isEmpty()) {
                holder.link.visibility = View.INVISIBLE
            } else {
                holder.link.visibility = View.VISIBLE
                holder.link.setOnClickListener { onClickListener?.onLinkClick(item) }
            }

            holder.setChecked(item.isChecked)

            if (!item.imgUrl.isEmpty()) {
                Picasso.get()
                        .load(item.imgUrl)
                        .placeholder(R.drawable.ic_image_placeholder)
                        .error(R.drawable.ic_broken_image)
                        .into(holder.img)
            }

            holder.check.setOnClickListener {
                val curr = item.isChecked
                item.isChecked = !curr
                holder.setChecked(item.isChecked)
                onClickListener?.onItemCheckChanged(item)
            }

            with(holder.itemView) {
                tag = item
                setOnClickListener { onClickListener?.onItemClick(item) }
            }
        }

        override fun getItemCount() = values.size

        //------------------------------------------------------------------------------------------------

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val title: TextView = view.item_title
            val theme: TextView = view.item_theme
            val location: TextView = view.item_location
            val price: TextView = view.item_price
            val difficulty: TextView = view.item_difficulty
            val link: TextView = view.item_link
            val count: TextView = view.item_count
            val time: TextView = view.item_time
            val img: ImageView = view.item_img
            val check: ImageView = view.item_check

            fun setChecked(isChecked: Boolean) {
                if (isChecked) {
                    check.setColorFilter(ContextCompat.getColor(check.context, R.color.colorAccent))
                } else {
                    check.colorFilter = null
                }
            }
        }
    }
    //------------------------------------------------------------------------------------------------
}
