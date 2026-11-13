package core.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import okio.Path

expect fun getCacheDirectory(context: PlatformContext): Path

fun newPanelImageLoader(
    context: PlatformContext,
    httpClient: HttpClient? = null
): ImageLoader {
    val client = httpClient ?: HttpClient {
        defaultRequest {
            header(HttpHeaders.UserAgent, "Dokuha/1.0.0 (https://github.com/RitwikGupta-0501/dokuha)")
        }
    }
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(client))
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
