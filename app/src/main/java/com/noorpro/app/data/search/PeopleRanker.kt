package com.noorpro.app.data.search

import com.noorpro.app.audio.search.AudioSearchEngine

/** 1.0.92: Instagram-style people search — prefix + fuzzy on name/@username, relationship first. */
object PeopleRanker {
    data class Person(
        val uid: String, val name: String, val handle: String, val photoUrl: String,
        val reels: Int = 0, val views: Long = 0, val following: Boolean = false, val followsMe: Boolean = false
    ) {
        val mutual get() = following && followsMe
        val relation get() = when { mutual -> 3; following -> 2; followsMe -> 1; else -> 0 }
        val subtitle: String get() = listOfNotNull(
            when { mutual -> "Mutual"; following -> "Following"; followsMe -> "Follows you"; else -> null },
            if (reels > 0) "$reels reel${if (reels == 1) "" else "s"}" else null
        ).joinToString(" · ")
    }

    fun search(query: String, people: List<Person>, me: String = ""): List<Person> {
        val q = query.trim().trimStart('@'); if (q.isEmpty()) return emptyList()
        return people.asSequence().filter { it.uid != me }
            .map { p ->
                val handleTier = AudioSearchEngine.tier(q, listOf(p.handle.removePrefix("@")))
                val nameTier = AudioSearchEngine.tier(q, listOf(p.name))
                p to maxOf(handleTier, nameTier)
            }
            .filter { it.second > AudioSearchEngine.NONE }
            .sortedWith(
                compareByDescending<Pair<Person, Int>> { it.second }
                    .thenByDescending { it.first.relation }
                    .thenByDescending { it.first.reels }
                    .thenByDescending { it.first.views }
            ).map { it.first }.toList()
    }

    /** Empty query: people who follow you (not followed back), then popular creators you don't follow. */
    fun suggested(people: List<Person>, me: String = "", limit: Int = 12): List<Person> =
        people.filter { it.uid != me && !it.following }
            .sortedWith(compareByDescending<Person> { it.followsMe }.thenByDescending { it.reels > 0 }.thenByDescending { it.views }.thenByDescending { it.reels })
            .take(limit)
}
