package com.example.comicaggregator.data.parser

import com.example.comicaggregator.data.model.ComicBook
import com.example.comicaggregator.data.model.ComicBookDetail
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

/**
 * Concrete implementation of [ComicParser] using the Jsoup library.
 * Designed to target the exact CSS selectors of the Ecko Onyx theme used by getcomics.org.
 */
class GetComicsParserImpl : ComicParser {

    override fun parseLatestComics(html: String): List<ComicBook> {
        val doc: Document = Jsoup.parse(html)
        
        // Target all post cards inside getcomics.org feed (represented by <article class="post ...">)
        val postElements = doc.select("article.post")
        
        val comicList = mutableListOf<ComicBook>()
        
        for (element in postElements) {
            try {
                // 1. Extract cover image URL (highly resilient to WordPress lazy loading / WP Rocket)
                val imgElement = element.selectFirst(".post-header-image img")
                val coverImageUrl = imgElement?.attr("data-lazy-src")?.takeIf { it.isNotEmpty() }
                    ?: imgElement?.attr("data-src")?.takeIf { it.isNotEmpty() }
                    ?: imgElement?.attr("src")?.takeIf { it.isNotEmpty() && !it.startsWith("data:") }
                    ?: imgElement?.attr("data-orig-file")?.takeIf { it.isNotEmpty() }
                    ?: ""
                
                // 2. Extract title & detail URL (inside .post-title a)
                val titleAnchor = element.selectFirst(".post-title a")
                val title = titleAnchor?.text() ?: ""
                val detailUrl = titleAnchor?.attr("href") ?: ""
                
                // Robustness safety check: if we cannot extract title or detail URL, skip this element
                if (title.isEmpty() || detailUrl.isEmpty()) {
                    continue
                }
                
                // 3. Extract Category (inside .post-category)
                val category = element.selectFirst(".post-category")?.text() ?: ""
                
                // 4. Extract Year and Size metadata from post-excerpt (e.g., "Year : 2026 | Size : 47 MB")
                val excerptParagraph = element.selectFirst(".post-excerpt p")
                val excerptText = excerptParagraph?.text() ?: ""
                
                var year: String? = null
                var size: String? = null
                
                if (excerptText.isNotEmpty()) {
                    val parts = excerptText.split("|")
                    for (part in parts) {
                        val cleaned = part.trim()
                        if (cleaned.startsWith("Year", ignoreCase = true)) {
                            // Extract year after the colon
                            year = cleaned.substringAfter(":").trim()
                        } else if (cleaned.startsWith("Size", ignoreCase = true)) {
                            // Extract size after the colon
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
                // Individual item parsing failure is caught locally to prevent a single malformed post
                // from crashing the entire feed fetch, boosting production-grade robustness.
                e.printStackTrace()
            }
        }
        
        return comicList
    }

    override fun parseComicDetail(html: String): ComicBookDetail {
        val doc: Document = Jsoup.parse(html)

        // 1. Extract Full Title (target post-title first, then fallback to h1)
        val title = doc.selectFirst("h1.post-title")?.text()
            ?: doc.selectFirst("h1")?.text()
            ?: "Unknown Comic Title"

        // 2. Extract Cover Image URL (highly resilient to WordPress lazy loading / WP Rocket)
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

        // 3. Extract Description / Story Plot summary (paragraphs inside post-contents / post-body)
        val postBody = doc.selectFirst("article.post-body") ?: doc.selectFirst(".post-contents")
        var description = ""
        
        if (postBody != null) {
            val paragraphs = postBody.select("p")
            val descLines = mutableListOf<String>()
            for (p in paragraphs) {
                val text = p.text().trim()
                if (text.isEmpty()) continue
                // Skip download button paragraphs and system advertisement indicators
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

        // Fallback meta description if DOM scraper failed to extract proper p tags
        if (description.isEmpty()) {
            description = doc.selectFirst("meta[name=description]")?.attr("content")
                ?: "No description plot summary available for this issue."
        }

        // 4. Extract Available Download Links
        // Filter elements to look specifically for download-ready hosting anchor tags
        val downloadLinks = mutableMapOf<String, String>()
        val anchors = doc.select("article.post-body a, .post-contents a")

        for (a in anchors) {
            val href = a.attr("href").trim()
            val text = a.text().trim().uppercase()
            val titleAttr = a.attr("title").trim().uppercase()

            if (href.isEmpty() || href.startsWith("#") || href.contains("how-to-download")) continue

            // Determine if standard button style aio- class is present
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
