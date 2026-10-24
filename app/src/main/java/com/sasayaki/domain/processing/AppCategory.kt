package com.sasayaki.domain.processing

enum class AppCategory(val styleHint: String?) {
    EMAIL("Use a professional written tone with proper greetings and sign-offs if present."),
    CHAT("Use a casual conversational tone. Keep it concise and natural for chat."),
    NOTES("Use a clear, structured writing style suitable for documents and notes."),
    SOCIAL("Keep it concise and suitable for social media posts."),

    // AI_CHAT carries no hint: the text is a prompt for another model, which reads
    // rambling input fine, and a cleanup model handed a prompt tends to answer it.
    AI_CHAT(null)
}

// appCategoryOf matches whole package names only. Matching words in labels or packages
// misfiled apps: "x" for X/Twitter caught Firefox and Termux.
fun appCategoryOf(packageName: String?): AppCategory? = packageName?.let(appCategories::get)

private val appCategories: Map<String, AppCategory> = buildMap {
    fun category(category: AppCategory, vararg packages: String) = packages.forEach { put(it, category) }

    category(
        AppCategory.EMAIL,
        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "ch.protonmail.android",
        "de.tutao.tutanota",
        "eu.faircode.email",
        "com.fsck.k9",
        "net.thunderbird.android",
        "com.samsung.android.email.provider",
        "com.yahoo.mobile.client.android.mail",
        "me.bluemail.mail",
        "com.readdle.spark"
    )
    category(
        AppCategory.CHAT,
        "com.slack",
        "com.discord",
        "org.telegram.messenger",
        "org.telegram.messenger.web",
        "org.thunderdog.challegram",
        "com.whatsapp",
        "com.whatsapp.w4b",
        "com.facebook.orca",
        "org.thoughtcrime.securesms",
        "im.molly.app",
        "im.vector.app",
        "io.element.android.x",
        "de.spiritcroc.riotx",
        "com.google.android.apps.messaging",
        "com.samsung.android.messaging",
        "org.fossify.messages",
        "com.simplemobiletools.smsmessenger",
        "com.google.android.apps.dynamite",
        "com.microsoft.teams",
        "chat.simplex.app",
        "com.wire",
        "ch.threema.app",
        "ch.threema.app.libre",
        "eu.siacs.conversations",
        "org.briarproject.briar.android",
        "com.beeper.android",
        "jp.naver.line.android",
        "com.viber.voip",
        "com.kakao.talk",
        "com.tencent.mm",
        "com.mattermost.rn",
        "chat.rocket.android",
        "com.zulipmobile"
    )
    category(
        AppCategory.NOTES,
        "com.google.android.keep",
        "com.google.android.apps.docs.editors.docs",
        "notion.id",
        "md.obsidian",
        "com.evernote",
        "com.microsoft.office.word",
        "com.microsoft.office.onenote",
        "com.samsung.android.app.notes",
        "org.fossify.notes",
        "com.simplemobiletools.notes.pro",
        "net.cozic.joplin",
        "com.standardnotes",
        "it.niedermann.owncloud.notes",
        "com.automattic.simplenote",
        "net.gsantner.markor",
        "com.orgzly",
        "com.logseq.app",
        "io.anytype.app"
    )
    category(
        AppCategory.SOCIAL,
        "com.twitter.android",
        "org.joinmastodon.android",
        "com.keylesspalace.tusky",
        "app.fedilab.android",
        "com.instagram.barcelona",
        "xyz.blueskyweb.app",
        "com.instagram.android",
        "com.facebook.katana",
        "com.reddit.frontpage",
        "com.linkedin.android"
    )
    category(
        AppCategory.AI_CHAT,
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.google.android.apps.bard",
        "ai.perplexity.app.android",
        "com.microsoft.copilot",
        "com.deepseek.chat",
        "ai.x.grok",
        "ai.mistral.chat"
    )
}
