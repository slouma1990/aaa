package com.anime4up

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import org.jsoup.nodes.Element

class WitanimeProvider : MainAPI() {
    override var mainUrl = "https://witanime.pics"
    override var name = "Witanime"
    override var lang = "ar"
    override val hasMainPage = true
    override val hasDownloadSupport = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private fun String.getIntFromText(): Int? {
        return Regex("""\d+""").find(this)?.groupValues?.firstOrNull()?.toIntOrNull()
    }

    private fun Element.toSearchResponse(): SearchResponse? {
        val title = select("div.anime-card-title h3 a").text() ?: return null
        val href = select("div.anime-card-title h3 a").attr("href") ?: return null
        val posterUrl = select("div.anime-card-poster img").attr("src")
        val type = select("div.anime-card-type").text()
        val episodeText = select("div.anime-card-episodes").text()
        val episode = episodeText.getIntFromText()

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
            addDubStatus(dubExist = false, subExist = true, subEpisodes = episode)
        }
    }

    override val mainPage = mainPageOf(
        "$mainUrl/episode/page/" to "Latest Episodes",
        "$mainUrl/anime-movies/page/" to "Latest Movies",
        "$mainUrl/anime/page/" to "Latest Anime"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get(request.data + page).document
        val items = document.select("div.anime-card-container div.anime-card").mapNotNull { it.toSearchResponse() }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("$mainUrl/?search_param=animes&s=$query").document
        return document.select("div.anime-card-container div.anime-card").mapNotNull { it.toSearchResponse() }
    }

    override suspend fun load(url: String): LoadResponse {
        val document = app.get(url).document
        
        val title = document.selectFirst("div.anime-details-title h1")?.text() ?: ""
        val poster = document.selectFirst("div.anime-details-poster img")?.attr("src")
        val description = document.selectFirst("div.anime-details-story p")?.text()
        val type = document.selectFirst("div.anime-details-type")?.text()?.trim()
        val status = document.selectFirst("div.anime-details-status")?.text()?.trim()
        val genres = document.select("div.anime-details-genres a").map { it.text() }
        
        val episodes = document.select("div.episodes-list-content div.episode").map { ep ->
            val epTitle = ep.selectFirst("div.episode-title")?.text() ?: ""
            val epHref = ep.selectFirst("a")?.attr("href") ?: ""
            val epNum = epTitle.getIntFromText() ?: 0
            
            Episode(
                epHref,
                epTitle,
                episode = epNum
            )
        }.reversed()

        val tvType = if (type?.contains("فيلم") == true) TvType.AnimeMovie else TvType.Anime

        return newAnimeLoadResponse(title, url, tvType) {
            this.posterUrl = poster
            this.plot = description
            this.tags = genres
            this.showStatus = when (status) {
                "مستمر" -> ShowStatus.Ongoing
                "منتهي" -> ShowStatus.Completed
                else -> null
            }
            addEpisodes(DubStatus.Subbed, episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        
        document.select("div.servers-list div.server").forEach { server ->
            val serverUrl = server.attr("data-url")
            if (serverUrl.isNotBlank()) {
                loadExtractor(serverUrl, data, subtitleCallback, callback)
            }
        }
        
        return true
    }
}