package be.sgl.backend.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ZipUtilsTest {

    private fun unzip(zip: ByteArray): Map<String, ByteArray> {
        val entries = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(zip)).use { stream ->
            generateSequence { stream.nextEntry }.forEach { entries[it.name] = stream.readBytes() }
        }
        return entries
    }

    @Test
    fun `should zip every file as numbered pdf entry when files are given`() {
        // given
        val files = listOf(byteArrayOf(1, 2, 3), byteArrayOf(4), byteArrayOf())

        // when
        val zip = files.zipped()

        // then
        val entries = unzip(zip)
        assertThat(entries.keys).containsExactly("file0.pdf", "file1.pdf", "file2.pdf")
        assertThat(entries.values.toList()).usingRecursiveComparison().isEqualTo(files)
    }

    @Test
    fun `should create an empty zip when no files are given`() {
        // given
        val files = emptyList<ByteArray>()

        // when
        val zip = files.zipped()

        // then
        assertThat(unzip(zip)).isEmpty()
    }
}
