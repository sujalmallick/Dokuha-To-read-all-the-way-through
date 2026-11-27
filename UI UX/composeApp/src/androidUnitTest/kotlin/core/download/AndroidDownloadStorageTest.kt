package core.download

import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AndroidDownloadStorageTest {

    @Test
    fun detectImageExtensionRecognizesPngGifWebpAndDefaultsToJpg() {
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)
        val gif = byteArrayOf(0x47, 0x49, 0x46, 0x38, 0x39, 0x61)
        val webp = "RIFF1234WEBP".encodeToByteArray()
        val jpg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte())

        assertEquals("png", detectImageExtension(png))
        assertEquals("gif", detectImageExtension(gif))
        assertEquals("webp", detectImageExtension(webp))
        assertEquals("jpg", detectImageExtension(jpg))
    }

    @Test(expected = IllegalArgumentException::class)
    fun savePageImageRejectsEmptyBytes() {
        val storage = AndroidDownloadStorage(ApplicationProvider.getApplicationContext())
        storage.savePageImage("manga/ch-empty", 1, byteArrayOf())
    }

    @Test
    fun getDownloadedPagesFiltersOutZeroByteAndMissingFiles() {
        val storage = AndroidDownloadStorage(ApplicationProvider.getApplicationContext())
        val pngBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x01, 0x02)
        val savedUri = storage.savePageImage("manga/ch-filter", 1, pngBytes)
        assertTrue(savedUri.endsWith("1.png"))

        val validPages = storage.getDownloadedPages("manga/ch-filter")
        assertNotNull(validPages)
        assertEquals(1, validPages!!.size)

        // Corrupt the saved file to 0 bytes -> getDownloadedPages must return null
        val savedFile = File(savedUri.removePrefix("file://"))
        savedFile.writeBytes(byteArrayOf())
        assertNull(storage.getDownloadedPages("manga/ch-filter"))
    }
}
