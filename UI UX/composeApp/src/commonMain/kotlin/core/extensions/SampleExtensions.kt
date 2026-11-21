package core.extensions

import core.extensions.js.ExtensionCatalogItem

/**
 * Extension bundle provider for fallback or demonstration items.
 * By default, no demo/sample extensions are bundled with the app.
 */
object SampleExtensions {
    val DEMO_CATALOG_ITEMS: List<ExtensionCatalogItem> = emptyList()

    fun getDemoBundle(id: String): String? = null
}
