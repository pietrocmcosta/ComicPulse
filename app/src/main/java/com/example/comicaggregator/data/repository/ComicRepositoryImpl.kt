package com.example.comicaggregator.data.repository

import com.example.comicaggregator.data.model.ComicBook
import com.example.comicaggregator.data.model.ComicBookDetail
import com.example.comicaggregator.data.parser.ComicParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/**
 * Concrete implementation of [ComicRepository].
 * Feeds HTML content fetched over HTTP into the [ComicParser] within the IO thread pool context.
 *
 * @param parser The parsing contract implementation (e.g. [GetComicsParserImpl]).
 * @param ioDispatcher The thread pool dispatcher where the networking/scraping takes place (defaults to [Dispatchers.IO]).
 */
class ComicRepositoryImpl(
    private val parser: ComicParser,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ComicRepository {

    override suspend fun getLatestComics(page: Int, searchQuery: String?): Result<List<ComicBook>> = withContext(ioDispatcher) {
        runCatching {
            val url = if (searchQuery.isNullOrBlank()) {
                if (page <= 1) "https://getcomics.org/" else "https://getcomics.org/page/$page/"
            } else {
                val encodedQuery = java.net.URLEncoder.encode(searchQuery, "UTF-8")
                if (page <= 1) "https://getcomics.org/?s=$encodedQuery" else "https://getcomics.org/page/$page/?s=$encodedQuery"
            }
            // Fetch HTML over the wire with custom User-Agent and connection settings
            val response = try {
                Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                    .timeout(15000) // robust 15 seconds timeout
                    .followRedirects(true)
                    .execute()
            } catch (e: org.jsoup.HttpStatusException) {
                if (e.statusCode == 404) {
                    return@runCatching emptyList<ComicBook>()
                }
                throw e
            }
            
            val html = response.body()
            parser.parseLatestComics(html)
        }
    }

    override suspend fun getComicDetail(detailUrl: String): Result<ComicBookDetail> = withContext(ioDispatcher) {
        runCatching {
            // Fetch raw HTML of the specific detail page on the network IO context
            val response = Jsoup.connect(detailUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36")
                .timeout(15000)
                .followRedirects(true)
                .execute()

            val html = response.body()
            parser.parseComicDetail(html)
        }
    }
}
