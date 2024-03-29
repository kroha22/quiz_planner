package com.quizplanner.quizPlanner.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.quizplanner.quizPlanner.QuizPlanner
import com.quizplanner.quizPlanner.R
import com.quizplanner.quizPlanner.model.Quiz
import com.squareup.picasso.Picasso


//------------------------------------------------------------------------------------------------
open class SimpleItemRecyclerViewAdapter(private val showDate: Boolean) :
        RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    //------------------------------------------------------------------------------------------------
    interface ItemClickListener {
        fun onItemClick(quiz: Quiz)

        fun onItemCheckChanged(quiz: Quiz)
    }

    //------------------------------------------------------------------------------------------------

    private var onClickListener: ItemClickListener? = null
    private var values: MutableList<Quiz> = ArrayList()
    private val onItemCheckChanged: MutableMap<Quiz, () -> Unit> = HashMap()

    open fun setValues(values: List<Quiz>) {
        this.values = ArrayList(values)
        notifyDataSetChanged()
    }

    protected fun getValues() = values

    open fun removeItem(item: Quiz) {
        val pos = values.indexOf(item)
        values.remove(item)
        notifyItemRemoved(pos)
    }

    fun isEmpty(): Boolean {
        return values.isEmpty()
    }

    fun setOnClickListener(onClickListener: ItemClickListener?) {
        this.onClickListener = onClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.quiz_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is ViewHolder) {
            val item = getItem(position)
            holder.title.text = item.organisationName
            holder.theme.text = item.gameTheme

            holder.location.text = if(item.isOnlineGame()){
                holder.location.context.getString(R.string.online)
            } else {
                item.location
            }

            holder.onlineImg.visibility = if(item.isOnlineGame()){
               View.VISIBLE
            } else {
                View.INVISIBLE
            }

            if (showDate) {
                holder.date.text = QuizPlanner.formatterDateMonth.format(item.date)
                holder.dateLine.visibility = View.VISIBLE
            } else {
                holder.dateLine.visibility = View.GONE
            }

            if (item.countOfPlayers != null) {
                holder.count.text = item.countOfPlayers.toString()
                holder.countLine.visibility = View.VISIBLE
            } else {
                holder.countLine.visibility = View.GONE
            }

            holder.time.text = QuizPlanner.formatterTime.format(item.date)

            holder.setChecked(item.isChecked)

            onItemCheckChanged[item] = { holder.setChecked(item.isChecked) }

            if (!item.getLogoUrl().isEmpty()) {
                val apiUrl = holder.title.context.getString(R.string.base_api_img_url)
                Picasso.get()
                        .load(apiUrl + item.getLogoUrl())
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

            if (QuizPlanner.isLast(item.getDate()) || item.isGamePostponed()) {
                holder.timeImg.setColorFilter(ContextCompat.getColor(holder.timeImg.context, R.color.medium_grey))
             //   holder.title.setTextColor(ContextCompat.getColor(holder.timeImg.context, R.color.dark_grey))
           //    holder.theme.setTextColor(ContextCompat.getColor(holder.timeImg.context, R.color.dark_grey))

            } else {
                holder.timeImg.setColorFilter(ContextCompat.getColor(holder.timeImg.context, R.color.red))
           //     holder.title.setTextColor(ContextCompat.getColor(holder.timeImg.context, R.color.black))
           //     holder.theme.setTextColor(ContextCompat.getColor(holder.timeImg.context, R.color.black))
            }

            if(item.isGamePostponed()){
                holder.postponed.visibility = View.VISIBLE
            } else {
                holder.postponed.visibility = View.GONE
            }
        }
    }

    fun getItem(position: Int) = values[position]

    override fun getItemCount() = values.size

    //------------------------------------------------------------------------------------------------

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.item_title)
        val theme: TextView = view.findViewById(R.id.item_theme)
        val location: TextView = view.findViewById(R.id.item_location)
        val onlineImg: ImageView = view.findViewById(R.id.item_online)
        val count: TextView = view.findViewById(R.id.item_count)
        val time: TextView = view.findViewById(R.id.item_time)
        val timeImg: ImageView = view.findViewById(R.id.time_img)
        val img: ImageView = view.findViewById(R.id.item_img)
        val check: ImageView = view.findViewById(R.id.item_check)
        val date: TextView = view.findViewById(R.id.item_date)
        val dateLine: LinearLayout = view.findViewById(R.id.item_date_line)
        val countLine: LinearLayout = view.findViewById(R.id.item_count_line)
        val postponed: LinearLayout = view.findViewById(R.id.item_postponed)

        fun setChecked(isChecked: Boolean) {
            if (isChecked) {
                check.setColorFilter(ContextCompat.getColor(check.context, R.color.colorAccent))
            } else {
                check.colorFilter = null
            }
        }
    }
}
