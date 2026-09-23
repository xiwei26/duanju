package com.juku.app.ui.components

import android.content.Context
import coil.request.ImageRequest
import okhttp3.Headers

fun authenticatedImageRequest(
    context: Context,
    url: String,
    requestHeaders: Map<String, String>
): ImageRequest {
    val headers = Headers.Builder().apply {
        requestHeaders.forEach { (name, value) -> add(name, value) }
    }.build()
    return ImageRequest.Builder(context)
        .data(url)
        .headers(headers)
        .crossfade(true)
        .build()
}
