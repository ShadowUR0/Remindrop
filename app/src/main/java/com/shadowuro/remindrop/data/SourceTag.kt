package com.shadowuro.remindrop.data

import android.net.Uri
import java.util.Locale

data class SourceTag(val id: String, val label: String) {
    companion object {
        const val TEXT_ID = "text"
        const val HOST_PREFIX = "h:"
        const val APP_PREFIX = "a:"
    }
}

private class KnownSource(
    val tag: SourceTag,
    val hosts: List<String>,
    val packages: List<String>,
)

private fun knownSource(
    id: String,
    label: String,
    hosts: List<String> = emptyList(),
    packages: List<String> = emptyList(),
) = KnownSource(SourceTag(id, label), hosts, packages)

object SourceTags {
    private val TEXT = SourceTag(SourceTag.TEXT_ID, "")

    private val known = listOf(
        knownSource(
            "reddit", "Reddit",
            listOf("reddit.com", "redd.it"),
            listOf("com.reddit.frontpage"),
        ),
        knownSource(
            "youtube", "YouTube",
            listOf("youtube.com", "youtu.be", "youtube-nocookie.com"),
            listOf("com.google.android.youtube", "com.google.android.apps.youtube.music"),
        ),
        knownSource(
            "x", "X",
            listOf("x.com", "twitter.com", "t.co"),
            listOf("com.twitter.android"),
        ),
        knownSource(
            "instagram", "Instagram",
            listOf("instagram.com", "instagr.am"),
            listOf("com.instagram.android"),
        ),
        knownSource(
            "tiktok", "TikTok",
            listOf("tiktok.com"),
            listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        ),
        knownSource(
            "facebook", "Facebook",
            listOf("facebook.com", "fb.com", "fb.watch", "fb.me"),
            listOf("com.facebook.katana", "com.facebook.lite"),
        ),
        knownSource(
            "messenger", "Messenger",
            listOf("messenger.com", "m.me"),
            listOf("com.facebook.orca", "com.facebook.mlite"),
        ),
        knownSource(
            "whatsapp", "WhatsApp",
            listOf("whatsapp.com", "wa.me"),
            listOf("com.whatsapp", "com.whatsapp.w4b"),
        ),
        knownSource(
            "telegram", "Telegram",
            listOf("t.me", "telegram.me", "telegram.org"),
            listOf("org.telegram.messenger", "org.thunderdog.challegram"),
        ),
        knownSource(
            "signal", "Signal",
            listOf("signal.me", "signal.group"),
            listOf("org.thoughtcrime.securesms"),
        ),
        knownSource(
            "messages", "Messages",
            packages = listOf("com.google.android.apps.messaging", "com.samsung.android.messaging"),
        ),
        knownSource(
            "discord", "Discord",
            listOf("discord.com", "discord.gg"),
            listOf("com.discord"),
        ),
        knownSource(
            "linkedin", "LinkedIn",
            listOf("linkedin.com", "lnkd.in"),
            listOf("com.linkedin.android"),
        ),
        knownSource(
            "pinterest", "Pinterest",
            listOf("pinterest.com", "pin.it"),
            listOf("com.pinterest"),
        ),
        knownSource(
            "threads", "Threads",
            listOf("threads.net", "threads.com"),
            listOf("com.instagram.barcelona"),
        ),
        knownSource(
            "snapchat", "Snapchat",
            listOf("snapchat.com"),
            listOf("com.snapchat.android"),
        ),
        knownSource(
            "bluesky", "Bluesky",
            listOf("bsky.app"),
            listOf("xyz.blueskyweb.app"),
        ),
        knownSource(
            "mastodon", "Mastodon",
            listOf("mastodon.social", "mastodon.online"),
            listOf("org.joinmastodon.android"),
        ),
        knownSource(
            "tumblr", "Tumblr",
            listOf("tumblr.com"),
            listOf("com.tumblr"),
        ),
        knownSource(
            "twitch", "Twitch",
            listOf("twitch.tv"),
            listOf("tv.twitch.android.app"),
        ),
        knownSource(
            "spotify", "Spotify",
            listOf("spotify.com", "spotify.link"),
            listOf("com.spotify.music"),
        ),
        knownSource(
            "soundcloud", "SoundCloud",
            listOf("soundcloud.com", "snd.sc"),
            listOf("com.soundcloud.android"),
        ),
        knownSource(
            "github", "GitHub",
            listOf("github.com", "github.io"),
            listOf("com.github.android"),
        ),
        knownSource(
            "medium", "Medium",
            listOf("medium.com"),
            listOf("com.medium.reader"),
        ),
        knownSource(
            "hackernews", "Hacker News",
            listOf("news.ycombinator.com"),
        ),
        knownSource(
            "stackoverflow", "Stack Overflow",
            listOf("stackoverflow.com", "stackexchange.com"),
        ),
        knownSource(
            "wikipedia", "Wikipedia",
            listOf("wikipedia.org", "wikimedia.org"),
            listOf("org.wikipedia"),
        ),
        knownSource(
            "quora", "Quora",
            listOf("quora.com"),
            listOf("com.quora.android"),
        ),
        knownSource(
            "imgur", "Imgur",
            listOf("imgur.com"),
        ),
        knownSource(
            "vimeo", "Vimeo",
            listOf("vimeo.com"),
        ),
        knownSource(
            "gmail", "Gmail",
            listOf("mail.google.com"),
            listOf("com.google.android.gm"),
        ),
        knownSource(
            "slack", "Slack",
            listOf("slack.com"),
            listOf("com.slack"),
        ),
        knownSource(
            "teams", "Teams",
            listOf("teams.microsoft.com"),
            listOf("com.microsoft.teams"),
        ),
    )

    private val byId = known.associate { it.tag.id to it.tag }
    private val byHost = known.flatMap { source -> source.hosts.map { it to source.tag.id } }.toMap()
    private val byPackage = known.flatMap { source -> source.packages.map { it to source.tag.id } }.toMap()

    private val hostPrefixes = listOf("www.", "m.", "mobile.", "amp.")
    private val genericSegments = setOf("com", "org", "net", "io", "app", "apps", "android", "mobile")

    fun resolveId(url: String?, sourcePackage: String?): String =
        fromUrlId(url) ?: fromPackageId(sourcePackage) ?: SourceTag.TEXT_ID

    fun tag(id: String): SourceTag {
        byId[id]?.let { return it }
        return when {
            id == SourceTag.TEXT_ID -> TEXT
            id.startsWith(SourceTag.HOST_PREFIX) -> {
                val host = id.removePrefix(SourceTag.HOST_PREFIX)
                SourceTag(id, prettify(host.substringBefore('.')))
            }
            id.startsWith(SourceTag.APP_PREFIX) -> {
                val packageName = id.removePrefix(SourceTag.APP_PREFIX)
                SourceTag(id, packageLabel(packageName))
            }
            else -> SourceTag(id, prettify(id.substringBefore('.')))
        }
    }

    private fun fromUrlId(url: String?): String? {
        val host = url
            ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
            ?.lowercase(Locale.ROOT)
            ?.trim('.')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val normalized = normalizeHost(host)
        var candidate = normalized
        while (candidate.isNotEmpty()) {
            byHost[candidate]?.let { return it }
            val dot = candidate.indexOf('.')
            if (dot < 0) break
            candidate = candidate.substring(dot + 1)
        }
        return SourceTag.HOST_PREFIX + normalized
    }

    private fun fromPackageId(sourcePackage: String?): String? {
        val packageName = sourcePackage
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        byPackage[packageName]?.let { return it }
        return SourceTag.APP_PREFIX + packageName
    }

    private fun normalizeHost(host: String): String {
        var value = host
        while (true) {
            val prefix = hostPrefixes.firstOrNull { value.startsWith(it) } ?: return value
            value = value.removePrefix(prefix)
        }
    }

    private fun packageLabel(packageName: String): String {
        val segment = packageName.split('.')
            .firstOrNull { it.isNotBlank() && it !in genericSegments }
            ?: packageName
        return prettify(segment)
    }

    private fun prettify(name: String): String = when {
        name.isEmpty() -> name
        name.length <= 3 -> name.uppercase(Locale.ROOT)
        else -> name.replaceFirstChar { it.uppercase() }
    }

}
