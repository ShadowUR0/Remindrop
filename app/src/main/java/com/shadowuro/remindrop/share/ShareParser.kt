package com.shadowuro.remindrop.share

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.util.Patterns


data class SharedItem(
    val title: String,
    val content: String,
    val url: String?,
    val source: String?,
)

object ShareParser {
    fun parse(activity: Activity): SharedItem? {
        val intent = activity.intent
        val rawText = when (intent.action) {
            Intent.ACTION_PROCESS_TEXT -> intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            else -> intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }?.trim().orEmpty()

        if (rawText.isBlank()) return null

        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.trim().orEmpty()
        val url = extractUrl(rawText)
        val title = when {
            subject.isNotBlank() -> subject.take(160)
            else -> inferTitle(rawText, url)
        }
        val source = activity.referrer
            ?.takeIf { it.scheme == "android-app" }
            ?.host

        return SharedItem(
            title = title,
            content = rawText.take(12_000),
            url = url,
            source = source,
        )
    }

    private fun extractUrl(text: String): String? {
        val matcher = Patterns.WEB_URL.matcher(text)
        if (!matcher.find()) return null
        var candidate = matcher.group().trim().trimEnd('.', ',', ')', ']', '}', ';')
        if (!candidate.startsWith("http://", ignoreCase = true) &&
            !candidate.startsWith("https://", ignoreCase = true)
        ) {
            candidate = "https://$candidate"
        }
        return runCatching { Uri.parse(candidate) }
            .getOrNull()
            ?.takeIf { it.host != null }
            ?.toString()
    }

    private fun inferTitle(text: String, url: String?): String {
        val firstMeaningfulLine = text.lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotBlank() && (url == null || !line.contains(url, ignoreCase = true))
            }

        if (!firstMeaningfulLine.isNullOrBlank()) {
            return firstMeaningfulLine.take(160)
        }

        val host = url?.let { runCatching { Uri.parse(it).host }.getOrNull() }
            ?.removePrefix("www.")
        if (!host.isNullOrBlank()) return host

        return text.replace('\n', ' ').trim().take(160)
    }
}
