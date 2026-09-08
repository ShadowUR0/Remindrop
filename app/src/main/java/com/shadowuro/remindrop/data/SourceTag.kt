package com.shadowuro.remindrop.data

import android.net.Uri
import java.util.Locale

data class SourceTag(val id: String, val label: String) {
    companion object {
        const val TEXT_ID = "text"
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

    private val byHost = known.flatMap { source -> source.hosts.map { it to source.tag } }.toMap()
    private val byPackage = known.flatMap { source -> source.packages.map { it to source.tag } }.toMap()

    private val hostPrefixes = listOf("www.", "m.", "mobile.", "amp.")
    private val sharedDomains = setOf("co", "com", "org", "net", "ac", "gov", "edu")
    private val genericSegments = setOf("com", "org", "net", "io", "app", "apps", "android", "mobile")

    fun of(reminder: Reminder): SourceTag = of(reminder.url, reminder.source)

    fun of(url: String?, source: String?): SourceTag = fromUrl(url) ?: fromPackage(source) ?: TEXT

    private fun fromUrl(url: String?): SourceTag? {
        val host = url
            ?.let { runCatching { Uri.parse(it).host }.getOrNull() }
            ?.lowercase(Locale.ROOT)
            ?.trim('.')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val normalized = hostPrefixes.fold(host) { value, prefix -> value.removePrefix(prefix) }
        byHost[normalized]?.let { return it }
        byHost.entries.firstOrNull { normalized.endsWith(".${it.key}") }?.let { return it.value }

        val domain = registrableDomain(normalized)
        return SourceTag(domain, prettify(domain.substringBefore('.')))
    }

    private fun fromPackage(source: String?): SourceTag? {
        val packageName = source
            ?.trim()
            ?.lowercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
            ?: return null

        byPackage[packageName]?.let { return it }
        val segment = packageName.split('.')
            .firstOrNull { it.isNotBlank() && it !in genericSegments }
            ?: return null
        return SourceTag(packageName, prettify(segment))
    }

    private fun registrableDomain(host: String): String {
        val labels = host.split('.').filter { it.isNotBlank() }
        if (labels.size <= 2) return labels.joinToString(".")
        val keep = if (labels[labels.size - 2] in sharedDomains) 3 else 2
        return labels.takeLast(keep).joinToString(".")
    }

    private fun prettify(name: String): String = when {
        name.isEmpty() -> name
        name.length <= 3 -> name.uppercase(Locale.ROOT)
        else -> name.replaceFirstChar { it.uppercase() }
    }
}
