package de.c4vxl.vaycoreapi.utils

import de.c4vxl.vaycoreapi.Main
import java.io.File
import java.io.FileOutputStream
import java.net.URL

object DownloadUtils {
    /**
     * Downloads a file
     * @param url The url to the file
     * @param outputFile The file to download the content in to
     */
    fun downloadFile(url: URL, outputFile: File) {
        Main.logger.info("Downloading $url to $outputFile...")

        url.openStream().use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }
    }
}