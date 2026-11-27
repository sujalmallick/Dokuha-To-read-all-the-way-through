package ui

import androidx.compose.ui.Modifier
import core.platform.PlatformInfo
import core.storage.InMemoryKeyValueStorage
import data.settings.AppLogoStyle
import data.settings.FakeSettingsRepository
import data.settings.PersistentSettingsRepository
import data.settings.SplashMotionStyle
import panelreader.composeapp.generated.resources.Res
import panelreader.composeapp.generated.resources.logo_classic
import panelreader.composeapp.generated.resources.logo_glassmorphic
import ui.components.drawableResource
import ui.glass.PanelGlassState
import ui.glass.glassSource
import ui.glass.internal.LiquidState
import ui.screens.splash.SplashFormats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppLogoAndSplashTest {

    @Test
    fun testAppLogoStyle_enumDefaultsAndResources() {
        assertEquals("Glassmorphic Edition", AppLogoStyle.GLASSMORPHIC.displayName)
        assertEquals("Classic Edition", AppLogoStyle.CLASSIC.displayName)

        assertEquals(Res.drawable.logo_glassmorphic, AppLogoStyle.GLASSMORPHIC.drawableResource())
        assertEquals(Res.drawable.logo_classic, AppLogoStyle.CLASSIC.drawableResource())
    }

    @Test
    fun testFakeSettingsRepository_logoStyleStateAndUpdates() {
        val repo = FakeSettingsRepository()

        // 1. Must default to Glassmorphic Edition
        assertEquals(AppLogoStyle.GLASSMORPHIC, repo.appLogoStyle.value)

        // 2. Can switch to Classic Edition
        repo.setAppLogoStyle(AppLogoStyle.CLASSIC)
        assertEquals(AppLogoStyle.CLASSIC, repo.appLogoStyle.value)

        // 3. Can switch back to Glassmorphic Edition
        repo.setAppLogoStyle(AppLogoStyle.GLASSMORPHIC)
        assertEquals(AppLogoStyle.GLASSMORPHIC, repo.appLogoStyle.value)
    }

    @Test
    fun testPersistentSettingsRepository_persistsLogoStyleAcrossRestarts() {
        val storage = InMemoryKeyValueStorage()
        val repo1 = PersistentSettingsRepository(storage)

        // Verify initial default is Glassmorphic Edition
        assertEquals(AppLogoStyle.GLASSMORPHIC, repo1.appLogoStyle.value)

        // User switches to Classic Edition
        repo1.setAppLogoStyle(AppLogoStyle.CLASSIC)
        assertEquals(AppLogoStyle.CLASSIC, repo1.appLogoStyle.value)

        // Simulate app restart by re-instantiating repository with identical storage
        val repo2 = PersistentSettingsRepository(storage)
        assertEquals(AppLogoStyle.CLASSIC, repo2.appLogoStyle.value)

        // User switches back to Glassmorphic Edition
        repo2.setAppLogoStyle(AppLogoStyle.GLASSMORPHIC)
        assertEquals(AppLogoStyle.GLASSMORPHIC, repo2.appLogoStyle.value)

        val repo3 = PersistentSettingsRepository(storage)
        assertEquals(AppLogoStyle.GLASSMORPHIC, repo3.appLogoStyle.value)
    }

    @Test
    fun testGlassModifier_safeFallbackOnNonAndroid() {
        val state = PanelGlassState(LiquidState())
        val originalModifier = Modifier
        val sourceModifier = originalModifier.glassSource(state)
        // On non-Android targets (iOS / JVM unit tests), glassSource returns original Modifier directly without layer recording
        if (!PlatformInfo.isAndroid) {
            assertEquals(originalModifier, sourceModifier)
        }
    }

    @Test
    fun testSplashMotionStyle_allSixConceptsRegistered() {
        val entries = SplashMotionStyle.entries
        assertEquals(6, entries.size)
        assertTrue(entries.contains(SplashMotionStyle.AMBIENT_RIBBON))
        assertTrue(entries.contains(SplashMotionStyle.MANGA_RAZOR_SLICE))
        assertTrue(entries.contains(SplashMotionStyle.INK_DROP))
        assertTrue(entries.contains(SplashMotionStyle.DIMENSIONAL_GLASS))
        assertTrue(entries.contains(SplashMotionStyle.LUNAR_SILVER_GLOW))
        assertTrue(entries.contains(SplashMotionStyle.LIQUID_GLASS))
    }

    @Test
    fun testSplashFormats_rotatesMultipleFormats() {
        assertEquals(
            listOf("manhua", "manhwa", "manga", "comic", "novel"),
            SplashFormats
        )
    }
}
