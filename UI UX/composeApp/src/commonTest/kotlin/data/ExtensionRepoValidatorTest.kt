package data

import core.extensions.ExtensionRepoValidator
import core.extensions.Sha256
import core.storage.InMemoryKeyValueStorage
import data.settings.PersistentSettingsRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExtensionRepoValidatorTest {

    @Test
    fun `https enforcement rejects non-https schemes`() {
        val httpResult = ExtensionRepoValidator.validateAndNormalize("http://example.com/repo")
        assertFalse(httpResult.isValid)
        assertTrue(httpResult.errorMessage?.contains("https://", ignoreCase = true) == true)

        val fileResult = ExtensionRepoValidator.validateAndNormalize("file:///data/repo/index.json")
        assertFalse(fileResult.isValid)

        val ftpResult = ExtensionRepoValidator.validateAndNormalize("ftp://example.com/repo")
        assertFalse(ftpResult.isValid)

        val validResult = ExtensionRepoValidator.validateAndNormalize("https://example.com/repo")
        assertTrue(validResult.isValid)
    }

    @Test
    fun `userinfo in URL is strictly rejected`() {
        val result = ExtensionRepoValidator.validateAndNormalize("https://admin:secret@example.com/repo")
        assertFalse(result.isValid)
        assertTrue(result.errorMessage?.contains("credentials", ignoreCase = true) == true)
    }

    @Test
    fun `localhost and private network addresses are rejected`() {
        val localhost = ExtensionRepoValidator.validateAndNormalize("https://localhost/repo")
        assertFalse(localhost.isValid)

        val loopback = ExtensionRepoValidator.validateAndNormalize("https://127.0.0.1/repo")
        assertFalse(loopback.isValid)

        val privateClassC = ExtensionRepoValidator.validateAndNormalize("https://192.168.1.100/repo")
        assertFalse(privateClassC.isValid)

        val privateClassA = ExtensionRepoValidator.validateAndNormalize("https://10.0.0.1/repo")
        assertFalse(privateClassA.isValid)

        val privateClassB = ExtensionRepoValidator.validateAndNormalize("https://172.16.0.5/repo")
        assertFalse(privateClassB.isValid)
    }

    @Test
    fun `url length constraint enforces maximum length of 512 characters`() {
        val validUrl = "https://example.com/" + "a".repeat(480)
        assertTrue(validUrl.length <= 512)
        val validResult = ExtensionRepoValidator.validateAndNormalize(validUrl)
        assertTrue(validResult.isValid)

        val tooLongUrl = "https://example.com/" + "a".repeat(500)
        assertTrue(tooLongUrl.length > 512)
        val invalidResult = ExtensionRepoValidator.validateAndNormalize(tooLongUrl)
        assertFalse(invalidResult.isValid)
        assertTrue(invalidResult.errorMessage?.contains("512") == true)
    }

    @Test
    fun `url normalization strips trailing slashes and index json`() {
        val slashResult = ExtensionRepoValidator.validateAndNormalize("https://example.com/repo/")
        assertTrue(slashResult.isValid)
        assertEquals("https://example.com/repo", slashResult.normalizedUrl)

        val indexResult = ExtensionRepoValidator.validateAndNormalize("https://example.com/repo/index.json")
        assertTrue(indexResult.isValid)
        assertEquals("https://example.com/repo", indexResult.normalizedUrl)

        val rootIndexResult = ExtensionRepoValidator.validateAndNormalize("https://example.com/index.json")
        assertTrue(rootIndexResult.isValid)
        assertEquals("https://example.com", rootIndexResult.normalizedUrl)

        val mixedCaseResult = ExtensionRepoValidator.validateAndNormalize("HTTPS://EXAMPLE.COM/my-extensions/")
        assertTrue(mixedCaseResult.isValid)
        assertEquals("https://example.com/my-extensions", mixedCaseResult.normalizedUrl)
    }

    @Test
    fun `deterministic sha-256 fingerprint generation`() {
        val url = "https://extensions.panel.io/main"
        val result1 = ExtensionRepoValidator.validateAndNormalize(url)
        val result2 = ExtensionRepoValidator.validateAndNormalize("https://extensions.panel.io/main/")

        assertTrue(result1.isValid)
        assertTrue(result2.isValid)
        assertNotNull(result1.fingerprint)
        // Normalized URLs are identical so fingerprints must match
        assertEquals(result1.fingerprint, result2.fingerprint)

        // Verify SHA-256 colon-delimited format (16 byte pairs)
        val fingerprint = result1.fingerprint!!
        val parts = fingerprint.split(":")
        assertEquals(16, parts.size)
        parts.forEach { part ->
            assertEquals(2, part.length)
            assertTrue(part.all { it in "0123456789ABCDEF" })
        }

        // Distinct URLs must produce distinct fingerprints
        val distinctResult = ExtensionRepoValidator.validateAndNormalize("https://other-extensions.org/repo")
        assertTrue(distinctResult.isValid)
        assertTrue(result1.fingerprint != distinctResult.fingerprint)
    }

    @Test
    fun `repository storage starts with ZERO default repositories and enforces deduplication`() {
        val storage = InMemoryKeyValueStorage()
        val settingsRepo = PersistentSettingsRepository(storage)

        // Invariant: Ship with ZERO default repositories
        assertEquals(0, settingsRepo.extensionRepos.value.size)

        // Add first repo
        val add1 = settingsRepo.addExtensionRepo("Test Repo", "https://extensions.panel.io/manga")
        assertTrue(add1.isSuccess)
        assertEquals(1, settingsRepo.extensionRepos.value.size)
        val repo = settingsRepo.extensionRepos.value.first()
        assertEquals("https://extensions.panel.io/manga", repo.url)
        assertNotNull(repo.fingerprint)

        // Adding exact duplicate must fail
        val addDuplicate = settingsRepo.addExtensionRepo("Another Name", "https://extensions.panel.io/manga")
        assertTrue(addDuplicate.isFailure)
        assertEquals(1, settingsRepo.extensionRepos.value.size)

        // Adding duplicate with trailing slash (normalized to same URL) must also fail
        val addDuplicateNormalized = settingsRepo.addExtensionRepo("Another Name", "https://extensions.panel.io/manga/")
        assertTrue(addDuplicateNormalized.isFailure)
        assertEquals(1, settingsRepo.extensionRepos.value.size)

        // Removal
        settingsRepo.removeExtensionRepo(repo.id)
        assertEquals(0, settingsRepo.extensionRepos.value.size)
    }
}
