package com.quizplanner.quizPlanner.model

import java.io.Serializable

/**
 * Created by Olga Cherepanova
 * on 20.01.2019.
 */

data class Quiz(val id: String,
                val name: String,
                val place: String,
                val price: String,
                val time: String,
                val minCount: String,
                val maxCount: String,
                val link: String,
                val detail: String,
                val imgUrl: String): Serializable {
    override fun toString(): String {
        return "Quiz(id='$id', name='$name', place='$place', price='$price', time='$time', minCount='$minCount', maxCount='$maxCount', link='$link', detail='$detail', imgUrl='$imgUrl')"
    }

    fun count(): String{
        return "$minCount-$maxCount"
    }
}