package com.grimsqueaker.comicpulse.parser.builtin

import com.grimsqueaker.comicpulse.data.model.ComicBook
import com.grimsqueaker.comicpulse.data.model.ComicBookDetail
import com.grimsqueaker.comicpulse.parser.ParserContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Built-in parser for getcomics.org.
 *
 * This implements the new [ParserContract] interface, making it fully compatible
 * with the plug-and-play parser registry while preserving every CSS selector and
 * fallback chain that was already battle-tested in the original GetComicsParserImpl.
 */
class GetComicsParser : ParserContract {

    override val id: String = "getcomics"
    override val displayName: String = "GetComics.org"
    override val description: String = "Scrapes the GetComics.org catalog for free DC, Marvel, Image, and indie comic downloads."
    override val version: String = "1.0.0"
    override val baseUrl: String = "https://getcomics.org"

    // ─────────────────────────────────────────
    // Network helpers
    // ─────────────────────────────────────────

    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"

    private suspend fun fetchHtml(url: String): String = withContext(Dispatchers.IO) {
        val response = Jsoup.connect(url)
            .userAgent(userAgent)
            .timeout(15000)
            .followRedirects(true)
            .execute()
        response.body()
    }

    // ─────────────────────────────────────────
    // ParserContract implementation
    // ─────────────────────────────────────────

    override suspend fun fetchComics(page: Int, searchQuery: String?): List<ComicBook> {
        val url = if (searchQuery.isNullOrBlank()) {
            if (page <= 1) "$baseUrl/" else "$baseUrl/page/$page/"
        } else {
            val encoded = java.net.URLEncoder.encode(searchQuery, "UTF-8")
            if (page <= 1) "$baseUrl/?s=$encoded" else "$baseUrl/page/$page/?s=$encoded"
        }

        val html = try {
            fetchHtml(url)
        } catch (e: org.jsoup.HttpStatusException) {
            if (e.statusCode == 404) return emptyList()
            throw e
        }

        return parseLatestComics(html)
    }

    override suspend fun fetchComicDetail(detailUrl: String): ComicBookDetail {
        val html = fetchHtml(detailUrl)
        return parseComicDetail(html)
    }

    // ─────────────────────────────────────────
    // HTML parsing (preserved from legacy GetComicsParserImpl)
    // ─────────────────────────────────────────

    private fun parseLatestComics(html: String): List<ComicBook> {
        val doc: Document = Jsoup.parse(html)
        val postElements = doc.select("article.post")
        val comicList = mutableListOf<ComicBook>()

        for (element in postElements) {
            try {
                val imgElement = element.selectFirst(".post-header-image img")
                val coverImageUrl = imgElement?.attr("data-lazy-src")?.takeIf { it.isNotEmpty() }
                    ?: imgElement?.attr("data-src")?.takeIf { it.isNotEmpty() }
                    ?: imgElement?.attr("src")?.takeIf { it.isNotEmpty() && !it.startsWith("data:") }
                    ?: imgElement?.attr("data-orig-file")?.takeIf { it.isNotEmpty() }
                    ?: ""

                val titleAnchor = element.selectFirst(".post-title a")
                val title = titleAnchor?.text() ?: ""
                val detailUrl = titleAnchor?.attr("href") ?: ""

                if (title.isEmpty() || detailUrl.isEmpty()) continue

                val category = element.selectFirst(".post-category")?.text() ?: ""

                val excerptParagraph = element.selectFirst(".post-excerpt p")
                val excerptText = excerptParagraph?.text() ?: ""

                var year: String? = null
                var size: String? = null

                if (excerptText.isNotEmpty()) {
                    val parts = excerptText.split("|")
                    for (part in parts) {
                        val cleaned = part.trim()
                        if (cleaned.startsWith("Year", ignoreCase = true)) {
                            year = cleaned.substringAfter(":").trim()
                        } else if (cleaned.startsWith("Size", ignoreCase = true)) {
                            size = cleaned.substringAfter(":").trim()
                        }
                    }
                }

                comicList.add(
                    ComicBook(
                        title = title,
                        detailUrl = detailUrl,
                        coverImageUrl = coverImageUrl,
                        year = year,
                        size = size,
                        category = category
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return comicList
    }

    private fun parseComicDetail(html: String): ComicBookDetail {
        val doc: Document = Jsoup.parse(html)

        val title = doc.selectFirst("h1.post-title")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: "Unknown Comic Title"

        val imgElement = doc.selectFirst(".post-header-image img")
            ?: doc.selectFirst("article.post-body img")
            ?: doc.selectFirst("article img")

        var coverImageUrl = ""
        if (imgElement != null) {
            coverImageUrl = imgElement.attr("data-lazy-src").takeIf { it.isNotEmpty() }
                ?: imgElement.attr("data-src").takeIf { it.isNotEmpty() }
                ?: imgElement.attr("src").takeIf { it.isNotEmpty() && !it.startsWith("data:") }
                ?: imgElement.attr("data-orig-file").takeIf { it.isNotEmpty() }
                ?: ""
        }

        val postBody = doc.selectFirst("article.post-body") ?: doc.selectFirst(".post-contents")
        var description = ""

        if (postBody != null) {
            val paragraphs = postBody.select("p")
            val descLines = mutableListOf<String>()
            for (p in paragraphs) {
                val text = p.text().trim()
                if (text.isEmpty()) continue
                if (p.selectFirst("a[class^=aio-]") != null) continue
                if (text.startsWith("Language :", ignoreCase = true)) continue
                if (text.startsWith("Download Free Comics", ignoreCase = true)) continue
                if (text.startsWith("Free Comics Download", ignoreCase = true)) continue
                if (text.startsWith("Year :", ignoreCase = true)) continue
                if (text.startsWith("Size :", ignoreCase = true)) continue
                if (text.contains("Image Format :", ignoreCase = true)) continue
                if (text.startsWith("Notes :", ignoreCase = true)) continue
                if (text.startsWith("-", ignoreCase = true)) continue

                descLines.add(text)
            }
            description = descLines.joinToString("\n\n")
        }

        if (description.isEmpty()) {
            description = doc.selectFirst("meta[name=description]")?.attr("content")
                ?: "No description plot summary available for this issue."
        }

        val downloadLinks = mutableMapOf<String, String>()
        val anchors = doc.select("article.post-body a, .post-contents a")

        for (a in anchors) {
            val href = a.attr("href").trim()
            val text = a.text().trim().uppercase()
            val titleAttr = a.attr("title").trim().uppercase()

            if (href.isEmpty() || href.startsWith("#") || href.contains("how-to-download")) continue

            val isAioButton = a.attr("class").startsWith("aio-")

            val hostName = when {
                text.contains("DOWNLOAD NOW") || titleAttr.contains("DOWNLOAD NOW") -> "Main Server"
                text.contains("PIXELDRAIN") || titleAttr.contains("PIXELDRAIN") -> "Pixeldrain"
                text.contains("TERABOX") || titleAttr.contains("TERABOX") -> "Terabox"
                text.contains("ROOTZ") || titleAttr.contains("ROOTZ") -> "Rootz"
                text.contains("VIKINGFILE") || titleAttr.contains("VIKINGFILE") -> "Vikingfile"
                text.contains("DATANODES") || titleAttr.contains("DATANODES") -> "Datanodes"
                text.contains("MEDIAFIRE") || titleAttr.contains("MEDIAFIRE") -> "Mediafire"
                text.contains("MEGA") || titleAttr.contains("MEGA") -> "Mega"
                text.contains("DRIVE") || titleAttr.contains("DRIVE") -> "Google Drive"
                isAioButton && text.isNotEmpty() -> text.lowercase().replaceFirstChar { it.uppercase() }
                else -> null
            }

            if (hostName != null) {
                val fullUrl = if (href.startsWith("/")) "https://getcomics.org$href" else href
                downloadLinks[hostName] = fullUrl
            }
        }

        return ComicBookDetail(
            title = title,
            description = description,
            coverImageUrl = coverImageUrl,
            downloadLinks = downloadLinks
        )
    }
}
