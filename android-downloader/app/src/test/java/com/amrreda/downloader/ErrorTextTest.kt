package com.amrreda.downloader

import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTextTest {
    private val http = Regex("HTTP(?: Error)? (\\d{3})")

    @Test
    fun recognisesBothHttpErrorFormats() {
        assertEquals("403", http.find("HTTP 403")!!.groupValues[1])
        assertEquals("403", http.find("unable to download video data: HTTP Error 403: Forbidden")!!.groupValues[1])
    }
}
