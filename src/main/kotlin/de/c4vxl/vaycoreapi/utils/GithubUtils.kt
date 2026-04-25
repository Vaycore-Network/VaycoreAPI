package de.c4vxl.vaycoreapi.utils

import de.c4vxl.vaycoreapi.Main
import java.net.URI
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

object GithubUtils {
    /**
     * Fetches the latest release of a specific github repository
     * @param repo The repo (format: user/repo)
     */
    fun getLatestRelease(repo: String): String? {
        val apiUrl = "https://api.github.com/repos/$repo/releases/latest"

        val client = HttpClient.newHttpClient()
        val request = HttpRequest.newBuilder()
            .uri(URI.create(apiUrl))
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "VaycoreAPI")
            .GET()
            .build()

        val response = client.send(request, HttpResponse.BodyHandlers.ofString())

        if (response.statusCode() != 200) return null

        return response.body()
    }

    /**
     * Returns the URL of a specific file in a github release
     * @param release The release response
     * @param fileName The file to search for
     */
    fun getReleaseFileURL(release: String, fileName: String): URI? {
        return """"browser_download_url"\s*:\s*"(https?://[^"]*${Regex.escape(fileName)})"""".toRegex()
            .find(release)
            ?.groupValues?.get(1)
            ?.let { URI(it) }
    }

    /**
     * Parses a github url format
     * @param url The url (format: gh:user/repo:filename)
     */
    fun parseGitHubURL(url: String): Pair<String, String>? {
        val parts = url.split(":")

        // Check for "gh:" header
        if (parts.firstOrNull() != "gh") {
            Main.logger.warning("Tried to parse a non-github url format. Skipping")
            return null
        }

        // Extract repo
        val repo = parts.getOrNull(1) ?: run {
            Main.logger.warning("Tried to parse github url without a valid repo")
            return null
        }

        // Extract file name
        val file = parts.getOrNull(2) ?: run {
            Main.logger.warning("Tried to parse github url without a valid filename")
            return null
        }

        return repo to file
    }

    /**
     * Returns a download link to a specific file in a repos last release
     * @param url The gh url (format: gh:user/repo:filename)
     */
    fun latestReleaseFile(url: String): URL? {
        val parsed = parseGitHubURL(url) ?: return null

        // Get release
        val release = getLatestRelease(parsed.first) ?: run {
            Main.logger.warning("Tried to fetch latest github release. None found!")
            return null
        }

        return getReleaseFileURL(release, parsed.second)?.toURL()
    }
}