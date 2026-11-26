package com.cinemagallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.dnsoverhttps.DnsOverHttps
import java.net.InetAddress
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class CinemaGalleryApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        val bootstrapClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

        val cloudflareDns = try {
            DnsOverHttps.Builder()
                .client(bootstrapClient)
                .url("https://1.1.1.1/dns-query".toHttpUrl())
                .bootstrapDnsHosts(
                    InetAddress.getByName("1.1.1.1"),
                    InetAddress.getByName("1.0.0.1"),
                    InetAddress.getByName("2606:4700:4700::1111"),
                    InetAddress.getByName("2606:4700:4700::1001")
                )
                .build()
        } catch (e: Exception) {
            null
        }

        val okHttpClientBuilder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)

        if (cloudflareDns != null) {
            okHttpClientBuilder.dns(cloudflareDns)
        }

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClientBuilder.build())
            .crossfade(true)
            .build()
    }
}
