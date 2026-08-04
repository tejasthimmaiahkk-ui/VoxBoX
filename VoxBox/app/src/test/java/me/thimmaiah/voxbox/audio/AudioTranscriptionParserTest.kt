package me.thimmaiah.voxbox.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class AudioTranscriptionParserTest {
    @Test
    fun parsesStrictDiarizedProxyResponse() {
        val result = parseAudioTranscriptionResponse(
            """
            {
              "sessionId":"s1",
              "chunkId":"c1",
              "text":"Power rule",
              "durationMs":2500,
              "segments":[{"id":"c1:seg","speakerId":"A","startMs":30000,"endMs":32500,"text":"Power rule"}],
              "source":"openai"
            }
            """.trimIndent(),
        )

        assertEquals("s1", result.sessionId)
        assertEquals(TranscriptionSource.PROVIDER, result.source)
        assertEquals("A", result.segments.single().speakerId)
        assertEquals(30_000, result.segments.single().startMs)
    }
}
