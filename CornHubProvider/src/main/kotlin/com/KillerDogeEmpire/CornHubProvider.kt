package com.KillerDogeEmpire

import android.util.Log
import org.jsoup.nodes.Element
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.mvvm.logError
import com.lagradost.cloudstream3.network.WebViewResolver
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.M3u8Helper
import com.lagradost.cloudstream3.utils.getQualityFromName
import org.json.JSONObject

class CornHubProvider : MainAPI() {
    private val globalTvType = TvType.NSFW
    override var mainUrl              = "https://www.radiovatani.com"
    override var name                 = "Persian World"
    override val hasMainPage          = true
    override var lang                 = "en"
    override val hasQuickSearch       = false
    override val hasDownloadSupport   = true
    override val hasChromecastSupport = true
    override val supportedTypes       = setOf(TvType.NSFW)
    override val vpnStatus            = VPNStatus.MightBeNeeded

    override val mainPage = mainPageOf(
        "/fill1.html" to "Movies",
        "/sell1.html" to "TV Shows",
        "/live-tv.html" to "Live TVs",        
    )
    private val cookies = mapOf(Pair("hasVisited", "1"), Pair("accessAgeDisclaimerPH", "1"))

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        try {
            val categoryData = request.data
            val categoryName = request.name
            val pagedLink = if (page > 0) categoryData + page else categoryData
            val soup = app.get(pagedLink, cookies = cookies).document
            val home = soup.select("div.sectionWrapper div.wrap").mapNotNull {
                if (it == null) {
                    return@mapNotNull null
                }
                val title = it.selectFirst("span.title a")?.text() ?: ""
                val link = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
                val img = fetchImgUrl(it.selectFirst("img"))
                MovieSearchResponse(
                    name = title,
                    url = link,
                    apiName = this.name,
                    type = globalTvType,
                    posterUrl = img
                )
            }
            if (home.isNotEmpty()) {
                return newHomePageResponse(
                    list = HomePageList(
                        name = categoryName, list = home, isHorizontalImages = true
                    ), hasNext = true
                )
            } else {
                throw ErrorLoadingException("No homepage data found!")
            }
        } catch (e: Exception) {
            logError(e)
        }
        throw ErrorLoadingException()
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val url = "$mainUrl/search?q=${query.replace(" ", "+")}"
        val document = app.get(url, cookies = cookies).document

        return document.select("div.col-md-2.col-sm-3.col-xs-6").mapNotNull { resultElement ->
            val title = resultElement.selectFirst("div.movie-title h3 a")?.text() ?: return@mapNotNull null
            val link = fixUrlNull(resultElement.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val image = fixUrlNull(resultElement.selectFirst("div.latest-movie-img-container img")?.attr("src")) ?: return@mapNotNull null

            MovieSearchResponse(
                name = title,
                url = link,
                apiName = this.name,
                type = globalTvType,
                posterUrl = image
            )
        }.distinctBy { it.url }
    }

    override suspend fun load(url: String): LoadResponse {
        val soup = app.get(url, cookies = cookies).document
    
     // Extracting poster URL
        val poster = fixUrlNull(soup.selectFirst("div.col-md-3 img.img-responsive")?.attr("src"))
    
     // Extracting title
        val title = soup.selectFirst("div.col-md-12 h1")?.text() ?: ""

     // Extracting plot
     //    val plot = soup.select("div.col-md-12 p").joinToString("\n") { it.text().trim() }

     // Extracting tags
     //    val tags = soup.select("div.categoriesWrapper a").map { it.text().trim().replace(", ", "") }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = title
        }
    }


    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val request = app.get(url = data, cookies = cookies)
        val document = request.document

     // Find the video element with id "play_html5_api"
        val videoElement = document.selectFirst("video#play_html5_api")

        if (videoElement != null) {
        // Extract the MP4 link from the src attribute
            val mp4Link = videoElement.attr("src")

        // Create an ExtractorLink and pass it to the callback
            callback(
                ExtractorLink(
                    source = name,
                    name = "${this.name} (MP4)",
                    url = mp4Link,
                    referer = mainUrl,
                    quality = "MP4", // You can set the quality to a specific value
                    isM3u8 = false // Since it's an MP4 link
                )
            )
        }

        return true
    }

    private fun fetchImgUrl(imgsrc: Element?): String? {
        return try {
            imgsrc?.attr("src") ?: imgsrc?.attr("data-src") ?: imgsrc?.attr("data-mediabook")
            ?: imgsrc?.attr("alt") ?: imgsrc?.attr("data-mediumthumb")
            ?: imgsrc?.attr("data-thumb_url")
        } catch (e: Exception) {
            null
        }
    }
}