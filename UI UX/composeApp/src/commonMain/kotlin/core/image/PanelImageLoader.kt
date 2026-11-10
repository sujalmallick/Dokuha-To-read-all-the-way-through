package core.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import okio.Path

expect fun getCacheDirectory(context: PlatformContext): Path

fun newPanelImageLoader(
    context: PlatformContext,
    httpClient: HttpClient? = null
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            if (httpClient != null) {
                add(KtorNetworkFetcherFactory(httpClient))
            } else {
                add(KtorNetworkFetcherFactory())
            }
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(getCacheDirectory(context))
                .maxSizeBytes(250L * 1024 * 1024) // 250 MB
                .build()
        }
        .build()
}
