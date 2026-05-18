package com.example.comicaggregator.data.repository

import com.example.comicaggregator.data.model.ComicBook
import com.example.comicaggregator.data.model.ComicBookDetail
import com.example.comicaggregator.parser.ParserRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Registry-backed implementation of [ComicRepository].
 *
 * Instead of depending on a single [ComicParser], this repository queries
 * **all active parsers** from the [ParserRegistry] and aggregates results.
 *
 * For detail pages, it routes the request to the first parser whose
 * [baseUrl] matches the detail URL prefix, falling back to the first
 * active parser if none matches.
 *
 * @param parserRegistry The global parser registry holding all active parsers.
 * @param ioDispatcher The thread pool dispatcher for I/O work (defaults to [Dispatchers.IO]).
 */
class ComicRepositoryImpl(
    private val parserRegistry: ParserRegistry,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : ComicRepository {

    override suspend fun getLatestComics(page: Int, searchQuery: String?): Result<List<ComicBook>> = withContext(ioDispatcher) {
        runCatching {
            val activeParsers = parserRegistry.getActiveParsers()

            if (activeParsers.isEmpty()) {
                return@runCatching emptyList<ComicBook>()
            }

            // Fan-out to all active parsers in parallel and merge results
            val deferredResults = activeParsers.map { parser ->
                async {
                    try {
                        parser.fetchComics(page, searchQuery)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        emptyList()
                    }
                }
            }

            deferredResults.awaitAll().flatten()
        }
    }

    override suspend fun getComicDetail(detailUrl: String): Result<ComicBookDetail> = withContext(ioDispatcher) {
        runCatching {
            val activeParsers = parserRegistry.getActiveParsers()

            // Try to find the parser whose baseUrl matches the detail URL
            val matchingParser = activeParsers.firstOrNull { parser ->
                detailUrl.startsWith(parser.baseUrl)
            } ?: activeParsers.firstOrNull()
            ?: throw IllegalStateException("No active parsers available. Enable at least one parser in Settings.")

            matchingParser.fetchComicDetail(detailUrl)
        }
    }
}
