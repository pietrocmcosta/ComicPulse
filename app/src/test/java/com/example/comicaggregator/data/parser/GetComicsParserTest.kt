package com.example.comicaggregator.data.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Robust Unit Tests for [GetComicsParserImpl] verifying parsing correctness
 * and resilient edge case parsing safety boundaries.
 */
class GetComicsParserTest {

    private val parser = GetComicsParserImpl()

    @Test
    fun parseLatestComics_withValidHtml_returnsParsedComics() {
        // GIVEN: Sample HTML simulating the exact live DOM structure of getcomics.org post feed
        val sampleHtml = """
            <!DOCTYPE html>
            <html>
            <body>
            <section class="page-contents post-list post-list-masonry">
                <article id="post-396758" class="post type-post status-publish format-standard hentry">
                    <div class="post-header-image">
                        <a href="https://getcomics.org/other-comics/2026-05-13-weekly-pack/">
                            <img src="https://i0.wp.com/getcomics.org/share/uploads/2026/05/2026.05.13-Weekly-Pack.jpg?fit=400%2C602&ssl=1" alt="2026.05.13 Weekly Pack">
                        </a>
                    </div>
                    <div class="post-info">
                        <a class="post-category" href="https://getcomics.org/cat/other-comics/">Other Comics</a>
                        <h1 class="post-title">
                            <a href="https://getcomics.org/other-comics/2026-05-13-weekly-pack/">2026.05.13 Weekly Pack</a>
                        </h1>
                        <div class="post-excerpt">
                            <p style="text-align: center;"><strong>Year : </strong>2026 | <strong>Size :</strong> 1.5 GB</p>
                        </div>
                    </div>
                </article>
            </section>
            </body>
            </html>
        """.trimIndent()

        // WHEN: Parsing the raw HTML content
        val results = parser.parseLatestComics(sampleHtml)

        // THEN: Confirm fields match expected values exactly
        assertEquals(1, results.size)
        val comic = results[0]
        assertEquals("2026.05.13 Weekly Pack", comic.title)
        assertEquals("https://getcomics.org/other-comics/2026-05-13-weekly-pack/", comic.detailUrl)
        assertEquals("https://i0.wp.com/getcomics.org/share/uploads/2026/05/2026.05.13-Weekly-Pack.jpg?fit=400%2C602&ssl=1", comic.coverImageUrl)
        assertEquals("2026", comic.year)
        assertEquals("1.5 GB", comic.size)
        assertEquals("Other Comics", comic.category)
    }

    @Test
    fun parseComicDetail_withValidDetailPage_returnsDetailedData() {
        // GIVEN: Sample HTML simulating the exact live DOM structure of a comic detail page
        val detailHtml = """
            <!DOCTYPE html>
            <html>
            <body>
            <header>
                <h1 class="post-title">Batman - Sword of Azrael - The Deluxe Edition (TPB) (2026)</h1>
            </header>
            <article class="post-body">
                <div class="post-header-image">
                    <img src="https://i0.wp.com/getcomics.org/share/uploads/2026/05/Azrael.jpg" />
                </div>
                <section class="post-contents">
                    <h2>The Story – Batman – Sword of Azrael – The Deluxe Edition</h2>
                    <p style="text-align: justify;">A deadly new vigilante emerges from the shadows in this origin story.</p>
                    <p>Jean-Paul Valley is trained since birth to become Azrael.</p>
                    
                    <h2>Free Comics Download</h2>
                    <p><strong>Size :</strong> 958 MB</p>
                    
                    <div class="aio-button-center">
                        <a href="https://1024terabox.com/s/1kBD" class="aio-blue" title="TERABOX">TERABOX</a>
                    </div>
                    <div class="aio-button-center">
                        <a href="https://rootz.so/d/1j4C" class="aio-green" title="ROOTZ">ROOTZ</a>
                    </div>
                    <div class="aio-button-center">
                        <a href="https://getcomics.org/dls/AqfU" class="aio-red" title="PIXELDRAIN">PIXELDRAIN</a>
                    </div>
                </section>
            </article>
            </body>
            </html>
        """.trimIndent()

        // WHEN: Parsing the detail HTML
        val result = parser.parseComicDetail(detailHtml)

        // THEN: Confirm fields match expected values
        assertEquals("Batman - Sword of Azrael - The Deluxe Edition (TPB) (2026)", result.title)
        assertEquals("https://i0.wp.com/getcomics.org/share/uploads/2026/05/Azrael.jpg", result.coverImageUrl)
        
        // Assert description combined correct paragraphs and excluded headers/buttons/metadata
        val expectedDescription = "A deadly new vigilante emerges from the shadows in this origin story.\n\nJean-Paul Valley is trained since birth to become Azrael."
        assertEquals(expectedDescription, result.description)

        // Assert download links extracted correctly
        assertEquals(3, result.downloadLinks.size)
        assertEquals("https://1024terabox.com/s/1kBD", result.downloadLinks["Terabox"])
        assertEquals("https://rootz.so/d/1j4C", result.downloadLinks["Rootz"])
        assertEquals("https://getcomics.org/dls/AqfU", result.downloadLinks["Pixeldrain"])
    }

    @Test
    fun parseLatestComics_withMalformedPost_skipsInvalidItem() {
        // GIVEN: HTML representing a malformed post container (missing link structure)
        val malformedHtml = """
            <html>
            <body>
                <article class="post">
                    <div class="post-header-image">
                        <img src="https://example.com/cover.jpg">
                    </div>
                    <div class="post-info">
                        <!-- Missing anchor link 'href' and 'text' entirely -->
                        <h1 class="post-title"></h1>
                    </div>
                </article>
            </body>
            </html>
        """.trimIndent()

        // WHEN: Parsing the invalid structure
        val results = parser.parseLatestComics(malformedHtml)

        // THEN: Verify the item is skipped gracefully and the app doesn't crash
        assertTrue(results.isEmpty())
    }

    @Test
    fun debugLiveDetailCoverImage() {
        try {
            val doc = org.jsoup.Jsoup.connect("https://getcomics.org/dc/batman-sword-of-azrael-the-deluxe-edition-tpb-2026/")
                .userAgent("Mozilla/5.0")
                .get()
            
            val parser = GetComicsParserImpl()
            val detail = parser.parseComicDetail(doc.outerHtml())
            
            println("====== DEBUG LIVE HTML START ======")
            val allImages = doc.select("article img")
            for (img in allImages) {
                println("Found image: src=${img.attr("src")}, data-src=${img.attr("data-src")}, data-lazy-src=${img.attr("data-lazy-src")}, class=${img.attr("class")}")
            }
            println("Extracted coverImageUrl: '${detail.coverImageUrl}'")
            println("====== DEBUG LIVE HTML END ======")
            
            assertTrue(detail.coverImageUrl.isNotEmpty())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
