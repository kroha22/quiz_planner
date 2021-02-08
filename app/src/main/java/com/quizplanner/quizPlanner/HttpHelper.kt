package com.quizplanner.quizPlanner

import java.util.regex.Pattern

object HttpHelper {

    const val regex = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"

    fun findUrl(text: String): List<String> {
        val pattern = Pattern.compile(regex)

        val allMatches = arrayListOf<String>()

        for(word in text.split(" ", "\n\n", "\n")){
            val matcher = pattern.matcher(word)
            if (matcher.find()) {
                allMatches.add(word)
            }
        }
        return allMatches
    }
    const val youTubeUrl = "^(http(s)?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/?([A-Za-z0-9\\\\-]*)"
    const val youTubeUrlRegEx = "^(https?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/"
    val videoIdRegex = arrayOf("\\?vi?=([^&]*)", "watch\\?.*v=([^&]*)", "(?:embed|vi?)/([^/?]*)", "^([A-Za-z0-9\\-]*)")

    fun isVideoUrl(text: String) = Pattern.compile(youTubeUrl).matcher(text).find()

    fun findVideoUrl(text: String): String? {
        val pattern = Pattern.compile(youTubeUrl)

        for(word in text.split(" ")){
            val matcher = pattern.matcher(word)
            if (matcher.find()) {
                return word
            }
        }
        return null
    }

    fun extractVideoIdFromUrl(url: String): String? {
        val youTubeLinkWithoutProtocolAndDomain = youTubeLinkWithoutProtocolAndDomain(url)

        for (regex in videoIdRegex) {
            val compiledPattern = Pattern.compile(regex)
            val matcher = compiledPattern.matcher(youTubeLinkWithoutProtocolAndDomain)

            if (matcher.find()) {
                return matcher.group(1)
            }
        }

        return null
    }

    private fun youTubeLinkWithoutProtocolAndDomain(url: String): String {
        val compiledPattern = Pattern.compile(youTubeUrlRegEx)
        val matcher = compiledPattern.matcher(url)

        return if (matcher.find()) {
            url.replace(matcher.group(), "")
        } else url
    }
}