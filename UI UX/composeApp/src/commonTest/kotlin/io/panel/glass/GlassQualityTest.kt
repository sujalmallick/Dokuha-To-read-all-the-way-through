package io.panel.glass

import core.storage.InMemoryKeyValueStorage
import data.settings.PersistentSettingsRepository
import ui.glass.GlassQuality
import ui.glass.GlassQualitySetting
import ui.glass.GlassSurfaceContext
import ui.glass.PlatformGlassEnvironment
import ui.glass.resolveGlassQuality
import kotlin.test.Test
import kotlin.test.assertEquals

class GlassQualityTest {

    @Test
    fun resolveGlassQuality_autoRuleRespectsCapabilitiesAndContext() {
        val iosNormal = PlatformGlassEnvironment(isIos = true, isLowPowerMode = false, platformReduceTransparency = false)
        val android33 = PlatformGlassEnvironment(isIos = false, androidSdkInt = 33)
        val android29 = PlatformGlassEnvironment(isIos = false, androidSdkInt = 29)
        val lowPower = PlatformGlassEnvironment(isIos = false, androidSdkInt = 34, isLowPowerMode = true)
        val reduceTrans = PlatformGlassEnvironment(isIos = true, platformReduceTransparency = true)

        assertEquals(GlassQuality.FULL, resolveGlassQuality(GlassQualitySetting.AUTO, iosNormal))
        assertEquals(GlassQuality.FULL, resolveGlassQuality(GlassQualitySetting.AUTO, android33))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, android29))
        assertEquals(GlassQuality.BALANCED, resolveGlassQuality(GlassQualitySetting.AUTO, lowPower))
        assertEquals(GlassQuality.REDUCED, resolveGlassQuality(GlassQualitySetting.AUTO, reduceTrans))

        // Readers and scrolling lists never use live blur
        assertEquals(
            GlassQuality.BALANCED,
            resolveGlassQuality(GlassQualitySetting.FULL, android33, context = GlassSurfaceContext.READER_OVERLAY)
        )
        assertEquals(
            GlassQuality.BALANCED,
            resolveGlassQuality(GlassQualitySetting.FULL, android33, context = GlassSurfaceContext.SCROLLING_LIST)
        )
        assertEquals(
            GlassQuality.BALANCED,
            resolveGlassQuality(GlassQualitySetting.FULL, android33, isScrolling = true)
        )

        // In-app reduceGlass forces REDUCED even if FULL is selected
        assertEquals(
            GlassQuality.REDUCED,
            resolveGlassQuality(GlassQualitySetting.FULL, android33, inAppReduceGlass = true)
        )
    }

    @Test
    fun persistentSettingsRepository_persistsGlassQuality() {
        val storage = InMemoryKeyValueStorage()
        val repo1 = PersistentSettingsRepository(storage)
        assertEquals(GlassQualitySetting.AUTO, repo1.glassQuality.value)

        repo1.setGlassQuality(GlassQualitySetting.BALANCED)
        val repo2 = PersistentSettingsRepository(storage)
        assertEquals(GlassQualitySetting.BALANCED, repo2.glassQuality.value)
    }
}
