package com.quizplanner.quizPlanner.player

import java.util.regex.Pattern

class YouTubeHelper {

    internal val youTubeUrl = "^(http(s)?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/?([A-Za-z0-9\\\\-]*)"
    internal val youTubeUrlRegEx = "^(https?)?(://)?(www.)?(m.)?((youtube.com)|(youtu.be))/"
    internal val videoIdRegex = arrayOf("\\?vi?=([^&]*)", "watch\\?.*v=([^&]*)", "(?:embed|vi?)/([^/?]*)", "^([A-Za-z0-9\\-]*)")

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