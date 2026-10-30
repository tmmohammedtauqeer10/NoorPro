package com.noorpro.app.data.messaging

import com.noorpro.app.audio.search.AudioSearchEngine
import java.util.Calendar

/**
 * 1.1.3 (sample): NoorPro messaging model — 1:1 chats, groups/circles, public discovery and
 * unified people/group/chat search. Pure Kotlin so the UI and the ranking can be unit-tested.
 * Firestore wiring is designed in docs/MESSAGING_1.1.3.md and stays behind [MessagingFlags].
 */

enum class GroupVisibility(val label: String, val blurb: String) {
    PUBLIC_OPEN("Public · open", "Anyone can find and join"),
    PUBLIC_REQUEST("Public · by request", "Anyone can find it; admins approve"),
    PRIVATE("Private · invite only", "Hidden from Discover; join by link or code")
}

enum class GroupRole(val label: String) { OWNER("Owner"), ADMIN("Admin"), MEMBER("Member") }

data class GroupTopic(val id: String, val label: String, val blurb: String)

object GroupTopics {
    val all = listOf(
        GroupTopic("quran", "Quran circle", "Recite, reflect and memorise together"),
        GroupTopic("hadith", "Hadith study", "Read and discuss the Sunnah"),
        GroupTopic("duas", "Duas & dhikr", "Daily adhkar and duas"),
        GroupTopic("masjid", "Local masjid", "Your masjid's community"),
        GroupTopic("sisters", "Sisters only", "A space for sisters"),
        GroupTopic("brothers", "Brothers only", "A space for brothers"),
        GroupTopic("youth", "Youth", "Young Muslims growing together"),
        GroupTopic("reverts", "Reverts", "Support for new Muslims"),
        GroupTopic("hajj", "Hajj & Umrah", "Prepare and travel together"),
        GroupTopic("ramadan", "Ramadan", "Fasting, taraweeh and goals"),
        GroupTopic("arabic", "Arabic", "Learn the language of the Quran"),
        GroupTopic("family", "Family", "Parenting and family life"),
        GroupTopic("charity", "Charity", "Sadaqah and volunteering"),
    )
    fun byId(id: String) = all.firstOrNull { it.id == id }
}

data class MsgMember(val uid: String, val name: String, val handle: String = "", val photoUrl: String = "",
                     val role: GroupRole = GroupRole.MEMBER, val online: Boolean = false)

data class NoorGroup(
    val id: String,
    val name: String,
    val description: String = "",
    val photoUrl: String = "",
    val topicId: String = "general",
    val tags: List<String> = emptyList(),
    val visibility: GroupVisibility = GroupVisibility.PUBLIC_OPEN,
    val ownerUid: String = "",
    val adminUids: List<String> = emptyList(),
    val memberUids: List<String> = emptyList(),
    val memberCount: Int = memberUids.size,
    val lastMessage: String = "",
    val lastSenderName: String = "",
    val updatedAt: Long = 0,
    val unread: Int = 0,
    val muted: Boolean = false,
    val pinnedNote: String = "",
    val onlineCount: Int = 0,
) {
    val topic get() = GroupTopics.byId(topicId)
    fun roleOf(uid: String) = when { uid == ownerUid -> GroupRole.OWNER; uid in adminUids -> GroupRole.ADMIN; else -> GroupRole.MEMBER }
    fun isMember(uid: String) = uid in memberUids
}

/** What a viewer can do with a group. Mirrors the planned Firestore rules (see docs). */
object GroupPermissions {
    fun isAdmin(g: NoorGroup, uid: String) = uid.isNotBlank() && (uid == g.ownerUid || uid in g.adminUids)
    fun canEditInfo(g: NoorGroup, uid: String) = isAdmin(g, uid)
    fun canManageMembers(g: NoorGroup, uid: String) = isAdmin(g, uid)
    /** Owner can remove anyone but themself; admins can remove plain members only. */
    fun canRemove(g: NoorGroup, actor: String, target: String): Boolean = when {
        target == actor || target == g.ownerUid -> false
        actor == g.ownerUid -> g.isMember(target)
        isAdmin(g, actor) -> g.isMember(target) && target !in g.adminUids
        else -> false
    }
    fun canPromote(g: NoorGroup, actor: String, target: String) = isAdmin(g, actor) && g.isMember(target) && !isAdmin(g, target)
    fun canDeleteMessage(g: NoorGroup, actor: String, senderUid: String) = actor == senderUid || isAdmin(g, actor)
    fun canLeave(g: NoorGroup, uid: String) = g.isMember(uid) && uid != g.ownerUid

    enum class JoinAction(val label: String) { OPEN("Open"), JOIN("Join"), REQUEST("Request"), REQUESTED("Requested"), INVITE_ONLY("Invite only") }
    fun joinAction(g: NoorGroup, uid: String, requested: Boolean = false): JoinAction = when {
        g.isMember(uid) -> JoinAction.OPEN
        requested -> JoinAction.REQUESTED
        g.visibility == GroupVisibility.PUBLIC_OPEN -> JoinAction.JOIN
        g.visibility == GroupVisibility.PUBLIC_REQUEST -> JoinAction.REQUEST
        else -> JoinAction.INVITE_ONLY
    }
}

data class MsgChat(
    val id: String, val otherUid: String, val name: String, val handle: String = "", val photoUrl: String = "",
    val lastMessage: String = "", val lastMine: Boolean = false, val tick: Int = 0, val updatedAt: Long = 0,
    val unread: Int = 0, val online: Boolean = false, val typing: Boolean = false, val pinned: Boolean = false,
    val voice: Boolean = false
)

data class MsgPerson(
    val uid: String, val name: String, val handle: String, val photoUrl: String = "",
    val following: Boolean = false, val followsMe: Boolean = false, val sharedGroups: Int = 0,
    val interests: List<String> = emptyList()
) {
    val mutual get() = following && followsMe
    val reason: String get() = when {
        mutual -> "Mutual follow"
        followsMe -> "Follows you"
        sharedGroups > 0 -> "In $sharedGroups of your groups"
        interests.isNotEmpty() -> "Also into ${interests.first()}"
        else -> "On NoorPro"
    }
}

/** Unified search across recent chats, people and groups — exact matches first, then related. */
object UnifiedSearch {
    enum class Kind { CHAT, PERSON, GROUP }
    data class Hit(val kind: Kind, val id: String, val title: String, val subtitle: String, val photoUrl: String, val tier: Int, val exact: Boolean)
    data class Results(val best: List<Hit>, val chats: List<Hit>, val people: List<Hit>, val groups: List<Hit>) {
        val isEmpty get() = best.isEmpty() && chats.isEmpty() && people.isEmpty() && groups.isEmpty()
    }

    fun search(query: String, chats: List<MsgChat>, people: List<MsgPerson>, groups: List<NoorGroup>, me: String = ""): Results {
        val q = query.trim().trimStart('@', '#')
        if (q.isEmpty()) return Results(emptyList(), emptyList(), emptyList(), emptyList())
        fun exact(fields: List<String>) = fields.any { AudioSearchEngine.normalize(it) == AudioSearchEngine.normalize(q) }
        val chatHits = chats.mapNotNull { c ->
            val f = listOf(c.name, c.handle.removePrefix("@"))
            val t = AudioSearchEngine.tier(q, f).takeIf { it > AudioSearchEngine.NONE } ?: return@mapNotNull null
            Hit(Kind.CHAT, c.id, c.name, c.lastMessage, c.photoUrl, t, exact(f)) to c.updatedAt.toDouble()
        }
        val personHits = people.filter { it.uid != me }.mapNotNull { p ->
            val f = listOf(p.name, p.handle.removePrefix("@"))
            val t = AudioSearchEngine.tier(q, f).takeIf { it > AudioSearchEngine.NONE } ?: return@mapNotNull null
            val rel = (if (p.mutual) 3 else if (p.following) 2 else if (p.followsMe) 1 else 0) + p.sharedGroups * 0.1
            Hit(Kind.PERSON, p.uid, p.name, "@${p.handle.removePrefix("@")} · ${p.reason}", p.photoUrl, t, exact(f)) to rel
        }
        val groupHits = groups.filter { it.visibility != GroupVisibility.PRIVATE || it.isMember(me) }.mapNotNull { g ->
            val f = listOf(g.name) + g.tags + listOfNotNull(g.topic?.label)
            val t = maxOf(AudioSearchEngine.tier(q, f), if (AudioSearchEngine.tier(q, listOf(g.description)) >= AudioSearchEngine.CONTAINS) AudioSearchEngine.FUZZY else 0)
                .takeIf { it > AudioSearchEngine.NONE } ?: return@mapNotNull null
            Hit(Kind.GROUP, g.id, g.name, "${g.topic?.label ?: "Group"} · ${formatCount(g.memberCount)} members", g.photoUrl, t, exact(listOf(g.name))) to g.memberCount.toDouble()
        }
        fun List<Pair<Hit, Double>>.ranked() = sortedWith(compareByDescending<Pair<Hit, Double>> { it.first.tier }.thenByDescending { it.second }).map { it.first }
        val c = chatHits.ranked(); val p = personHits.ranked(); val g = groupHits.ranked()
        // One best-match card per person: an exact person hit replaces their exact chat hit.
        val exactPeople = p.filter { it.exact }.map { it.id }.toSet()
        val chatOwner = chats.associate { it.id to it.otherUid }
        val best = (p + g + c).filter { it.exact && !(it.kind == Kind.CHAT && chatOwner[it.id] in exactPeople) }.distinctBy { it.kind to it.id }
        return Results(best, c.filterNot { it in best }, p.filterNot { it in best }, g.filterNot { it in best })
    }

    /** Suggested people: mutuals you haven't chatted with, then followers, then shared groups/interests. */
    fun suggested(people: List<MsgPerson>, chattedWith: Set<String>, me: String = "", limit: Int = 10): List<MsgPerson> =
        people.filter { it.uid != me && it.uid !in chattedWith }
            .sortedWith(compareByDescending<MsgPerson> { it.mutual }.thenByDescending { it.followsMe && !it.following }
                .thenByDescending { it.sharedGroups }.thenByDescending { it.interests.size })
            .take(limit)
}

/** Public groups for Discover: by topic, then by query, biggest first. Private groups never appear. */
object GroupDiscovery {
    fun browse(groups: List<NoorGroup>, topicId: String?, query: String = ""): List<NoorGroup> {
        val q = query.trim().trimStart('#')
        return groups.asSequence()
            .filter { it.visibility != GroupVisibility.PRIVATE }
            .filter { topicId == null || it.topicId == topicId }
            .map { g -> g to if (q.isEmpty()) AudioSearchEngine.EXACT else AudioSearchEngine.tier(q, listOf(g.name, g.description) + g.tags + listOfNotNull(g.topic?.label)) }
            .filter { it.second > AudioSearchEngine.NONE }
            .sortedWith(compareByDescending<Pair<NoorGroup, Int>> { it.second }.thenByDescending { it.first.memberCount })
            .map { it.first }.toList()
    }
}

/** Gentle Islamic touches: salam quick replies and Jumu'ah / Ramadan moments. */
object IslamicTouches {
    private val salam = Regex("(?i)(assalam|as-salam|asalam|salaam|salam\\b|السلام)")
    fun quickReplies(lastIncoming: String): List<String> =
        if (salam.containsMatchIn(lastIncoming)) listOf("Wa alaikum assalam", "Wa alaikum assalam wa rahmatullah", "JazakAllahu khayran")
        else listOf("Assalamu alaikum", "JazakAllahu khayran", "In sha Allah", "BarakAllahu feek")

    enum class Moment { JUMUAH, RAMADAN, NONE }
    fun moment(cal: Calendar, hijriMonth: Int): Moment = when {
        hijriMonth == 9 -> Moment.RAMADAN
        cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY -> Moment.JUMUAH
        else -> Moment.NONE
    }
}

/**
 * Remote Config gates (default off) for anything that needs the 1.1.3 Firestore rules/functions
 * deploy. Without the deploy the sample UI hides these actions.
 */
object MessagingFlags {
    const val GROUPS_V2 = "messaging_groups_v2_enabled"   // admins, leave, join requests, invite codes, admin delete
    const val DISCOVER = "messaging_discover_enabled"      // public directory + topic browse
    const val GROUP_SHARE = "messaging_group_share_enabled" // send reels/posts into circles (needs group post_share rule)
    val override = mutableMapOf<String, Boolean>()
    fun enabled(key: String): Boolean = override[key] ?: runCatching {
        com.google.firebase.remoteconfig.FirebaseRemoteConfig.getInstance().getBoolean(key)
    }.getOrDefault(false)
}

internal fun formatCount(n: Int): String = when {
    n >= 1_000_000 -> "%.1fM".format(n / 1_000_000.0).replace(".0M", "M")
    n >= 1_000 -> "%.1fK".format(n / 1_000.0).replace(".0K", "K")
    else -> n.toString()
}

// ------------------------------------------------------------------ 1.1.3 reel / post sharing

/** Someone (1:1 chat) or a circle (group) a reel/post can be sent to. */
data class ShareTarget(
    val id: String, val kind: Kind, val name: String, val subtitle: String = "", val photoUrl: String = "",
    val lastAt: Long = 0, val sharesSent: Int = 0, val online: Boolean = false, val group: NoorGroup? = null,
) {
    enum class Kind { PERSON, GROUP }
}

/**
 * Share sheet ordering and send planning. Uses the live 1.0.82 "post_share" model:
 * person targets go through UmmahRepository.sendPostShare (already allowed by the rules);
 * circle targets need the group post_share rule (rules v2) and stay behind [MessagingFlags.GROUP_SHARE].
 */
object ShareTargets {
    /** Frequent first (shares you've sent), then most recent chat; query = exact > prefix > contains > fuzzy. */
    fun rank(targets: List<ShareTarget>, query: String = "", groupsEnabled: Boolean = true): List<ShareTarget> {
        val q = query.trim().trimStart('@')
        return targets.asSequence()
            .filter { groupsEnabled || it.kind == ShareTarget.Kind.PERSON }
            .map { t -> t to if (q.isEmpty()) AudioSearchEngine.EXACT else AudioSearchEngine.tier(q, listOf(t.name, t.subtitle.removePrefix("@"))) }
            .filter { it.second > AudioSearchEngine.NONE }
            .sortedWith(compareByDescending<Pair<ShareTarget, Int>> { it.second }
                .thenByDescending { it.first.sharesSent }.thenByDescending { it.first.lastAt })
            .map { it.first }.toList()
    }

    data class Plan(val personUids: List<String>, val groupIds: List<String>, val note: String) {
        val count get() = personUids.size + groupIds.size
    }

    /** Max 15 recipients per share (same cap as sendPostShare); note trimmed to the rule's 500 chars. */
    fun plan(selected: List<ShareTarget>, note: String): Plan {
        val picked = selected.distinctBy { it.kind to it.id }.take(15)
        return Plan(picked.filter { it.kind == ShareTarget.Kind.PERSON }.map { it.id },
            picked.filter { it.kind == ShareTarget.Kind.GROUP }.map { it.id }, note.trim().take(500))
    }

    fun sendLabel(count: Int) = when (count) { 0 -> "Send"; 1 -> "Send"; else -> "Send to $count" }
}
