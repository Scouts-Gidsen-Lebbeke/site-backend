package be.sgl.backend.util

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

class ZipUtilsTest {

    @Test
    fun `zipped should create a zip file from list of byte arrays`() {
        val file1 = "Content of file 1".toByteArray()
        val file2 = "Content of file 2".toByteArray()
        val files = listOf(file1, file2)

        val zipped = files.zipped()

        assertNotNull(zipped)
        assertTrue(zipped.isNotEmpty())
    }

    @Test
    fun `zipped should contain correct number of entries`() {
        val file1 = "Content of file 1".toByteArray()
        val file2 = "Content of file 2".toByteArray()
        val file3 = "Content of file 3".toByteArray()
        val files = listOf(file1, file2, file3)

        val zipped = files.zipped()

        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipped))
        var entryCount = 0
        while (zipInputStream.nextEntry != null) {
            entryCount++
        }

        assertEquals(3, entryCount)
    }

    @Test
    fun `zipped should name files correctly`() {
        val file1 = "Content of file 1".toByteArray()
        val file2 = "Content of file 2".toByteArray()
        val files = listOf(file1, file2)

        val zipped = files.zipped()

        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipped))
        val entry1 = zipInputStream.nextEntry
        val entry2 = zipInputStream.nextEntry

        assertEquals("file0.pdf", entry1.name)
        assertEquals("file1.pdf", entry2.name)
    }

    @Test
    fun `zipped should preserve content of files`() {
        val content1 = "Content of file 1".toByteArray()
        val content2 = "Content of file 2".toByteArray()
        val files = listOf(content1, content2)

        val zipped = files.zipped()

        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipped))
        zipInputStream.nextEntry
        val extractedContent1 = zipInputStream.readAllBytes()
        zipInputStream.nextEntry
        val extractedContent2 = zipInputStream.readAllBytes()

        assertArrayEquals(content1, extractedContent1)
        assertArrayEquals(content2, extractedContent2)
    }

    @Test
    fun `zipped should handle empty list`() {
        val files = emptyList<ByteArray>()

        val zipped = files.zipped()

        assertNotNull(zipped)
        assertTrue(zipped.isNotEmpty())
    }

    @Test
    fun `zipped should handle single file`() {
        val file = "Single file content".toByteArray()
        val files = listOf(file)

        val zipped = files.zipped()

        val zipInputStream = ZipInputStream(ByteArrayInputStream(zipped))
        var entryCount = 0
        while (zipInputStream.nextEntry != null) {
            entryCount++
        }

        assertEquals(1, entryCount)
    }

    @Test
    fun `zipped should handle large files`() {
        val largeContent = ByteArray(10000) { it.toByte() }
        val files = listOf(largeContent)

        val zipped = files.zipped()

        assertNotNull(zipped)
        assertTrue(zipped.isNotEmpty())
    }
}
