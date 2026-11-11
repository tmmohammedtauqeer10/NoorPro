package com.noorpro.app.data.media.edit

import org.json.JSONArray
import org.json.JSONObject

/**
 * Drafts keep the editing steps: photo / reel edits as JSON (org.json, no new dependency). Unknown or
 * newer fields fall back to defaults, so an old draft always opens.
 */
object EditCodec {
    private inline fun <reified E : Enum<E>> JSONObject.enumOr(k: String, d: E): E =
        optString(k).takeIf { it.isNotEmpty() }?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: d
    private fun JSONObject.f(k: String, d: Float) = optDouble(k, d.toDouble()).toFloat()
    private fun JSONArray?.objs() = this?.let { a -> (0 until a.length()).mapNotNull { a.optJSONObject(it) } }.orEmpty()
    private fun JSONArray?.strings() = this?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
    private fun floats(l: List<Float>) = JSONArray().apply { l.forEach { put(it.toDouble()) } }

    fun adjust(a: Adjustments) = JSONObject().apply { AdjustKind.entries.forEach { k -> a.value(k).takeIf { it != 0f }?.let { put(k.name, it.toDouble()) } } }
    fun adjust(o: JSONObject?): Adjustments = o?.let { j -> AdjustKind.entries.fold(Adjustments()) { acc, k -> if (j.has(k.name)) acc.with(k, j.f(k.name, 0f)) else acc } } ?: Adjustments()

    fun layer(l: EditorLayer) = JSONObject().apply {
        put("id", l.id); l.sticker?.let { put("sticker", it.name) }; put("text", l.text); put("font", l.font.name); put("color", l.color)
        put("pill", l.pill.name); put("pillColor", l.pillColor); put("x", l.x.toDouble()); put("y", l.y.toDouble())
        put("scale", l.scale.toDouble()); put("rotation", l.rotation.toDouble()); put("startMs", l.startMs); put("endMs", l.endMs)
        put("look", l.look.name); put("align", l.align.name); put("anim", l.anim.name); put("locked", l.locked)
        put("sub", l.sub); put("options", JSONArray(l.options)); put("answer", l.answer)
    }
    fun layer(o: JSONObject): EditorLayer? = runCatching {
        val d = EditorLayer(o.getString("id"))
        d.copy(
            sticker = o.optString("sticker").takeIf { it.isNotEmpty() }?.let { n -> StickerKind.entries.firstOrNull { it.name == n } ?: return null },
            text = o.optString("text"), font = o.enumOr("font", d.font), color = o.optLong("color", d.color),
            pill = o.enumOr("pill", d.pill), pillColor = o.optLong("pillColor", d.pillColor), x = o.f("x", d.x), y = o.f("y", d.y),
            scale = o.f("scale", d.scale), rotation = o.f("rotation", d.rotation), startMs = o.optLong("startMs", d.startMs), endMs = o.optLong("endMs", d.endMs),
            look = o.enumOr("look", d.look), align = o.enumOr("align", d.align), anim = o.enumOr("anim", d.anim), locked = o.optBoolean("locked"),
            sub = o.optString("sub"), options = o.optJSONArray("options").strings(), answer = o.optInt("answer", -1)
        )
    }.getOrNull()

    private fun stroke(s: BrushStroke) = JSONObject().apply {
        put("p", JSONArray().apply { s.points.forEach { put(it.first.toDouble()); put(it.second.toDouble()) } })
        put("color", s.color); put("width", s.width.toDouble()); put("glow", s.glow)
    }
    private fun stroke(o: JSONObject): BrushStroke {
        val p = o.optJSONArray("p"); val pts = if (p == null) emptyList() else (0 until p.length() / 2).map { p.optDouble(it * 2).toFloat() to p.optDouble(it * 2 + 1).toFloat() }
        return BrushStroke(pts, o.optLong("color", 0xFFD4AF37), o.f("width", 0.012f), o.optBoolean("glow"))
    }

    fun photo(e: PhotoEdit): JSONObject = JSONObject().apply {
        put("v", 1); put("quarterTurns", e.quarterTurns); put("aspect", e.aspect.name)
        put("crop", floats(listOf(e.crop.left, e.crop.top, e.crop.right, e.crop.bottom)))
        put("filter", e.filter.name); put("strength", e.strength.toDouble()); put("adjust", adjust(e.adjust))
        put("layers", JSONArray(e.layers.map(::layer))); put("strokes", JSONArray(e.strokes.map(::stroke)))
        put("curve", floats(e.curve.points))
        put("hsl", JSONObject().apply { e.hsl.bands.forEach { (b, s) -> put(b.name, floats(listOf(s.hue, s.sat, s.lum))) } })
        e.tiltShift?.let { t -> put("tilt", floats(listOf(t.centerY, t.band, t.feather, t.angleDeg))) }
        put("blurStrokes", JSONArray(e.blurStrokes.map(::stroke))); put("blurAmount", e.blurAmount.toDouble())
    }
    fun photo(o: JSONObject): PhotoEdit = runCatching {
        val d = PhotoEdit()
        val c = o.optJSONArray("crop")?.let { a -> CropRect(a.optDouble(0, 0.0).toFloat(), a.optDouble(1, 0.0).toFloat(), a.optDouble(2, 1.0).toFloat(), a.optDouble(3, 1.0).toFloat()) } ?: d.crop
        val curve = o.optJSONArray("curve")?.takeIf { it.length() == 5 }?.let { a -> ToneCurve((0 until 5).map { a.optDouble(it).toFloat().coerceIn(0f, 1f) }) } ?: d.curve
        val hsl = o.optJSONObject("hsl")?.let { j -> HslAdjust(HueBand.entries.mapNotNull { b -> j.optJSONArray(b.name)?.let { a -> b to HslShift(a.optDouble(0).toFloat(), a.optDouble(1).toFloat(), a.optDouble(2).toFloat()) } }.toMap()) } ?: d.hsl
        PhotoEdit(
            quarterTurns = o.optInt("quarterTurns").mod(4), aspect = o.enumOr("aspect", d.aspect), crop = c,
            filter = o.enumOr("filter", d.filter), strength = o.f("strength", 1f), adjust = adjust(o.optJSONObject("adjust")),
            layers = o.optJSONArray("layers").objs().mapNotNull(::layer), strokes = o.optJSONArray("strokes").objs().map(::stroke),
            curve = curve, hsl = hsl,
            tiltShift = o.optJSONArray("tilt")?.let { a -> TiltShift(a.optDouble(0).toFloat(), a.optDouble(1).toFloat(), a.optDouble(2).toFloat(), a.optDouble(3).toFloat()) },
            blurStrokes = o.optJSONArray("blurStrokes").objs().map(::stroke), blurAmount = o.f("blurAmount", d.blurAmount)
        )
    }.getOrDefault(PhotoEdit())

    fun reel(e: ReelEdit): JSONObject = JSONObject().apply {
        put("v", 1); put("sourceDurationMs", e.sourceDurationMs)
        put("clips", JSONArray(e.clips.map { c -> JSONObject().apply { put("s", c.startMs); put("e", c.endMs); put("src", c.source); put("t", c.transition.name); put("freeze", c.freezeMs) } }))
        put("speed", e.speed.name); put("filter", e.filter.name); put("strength", e.strength.toDouble()); put("adjust", adjust(e.adjust))
        put("layers", JSONArray(e.layers.map(::layer))); put("originalVolume", e.originalVolume.toDouble())
        e.sound?.let { s -> put("sound", JSONObject().apply { put("trackId", s.trackId); put("title", s.title); put("artist", s.artist); put("url", s.url); put("licenseLabel", s.licenseLabel); put("sourceUrl", s.sourceUrl) }) }
        put("soundVolume", e.soundVolume.toDouble()); put("coverMs", e.coverMs)
        put("sources", JSONArray(e.sources.map { s -> JSONObject().apply { put("uri", s.uri); put("durationMs", s.durationMs); put("label", s.label) } }))
        put("ramp", e.ramp.name); put("fadeIn", e.soundFadeInMs); put("fadeOut", e.soundFadeOutMs)
        put("voices", JSONArray(e.voices.map { v -> JSONObject().apply { put("id", v.id); put("path", v.path); put("startMs", v.startMs); put("durationMs", v.durationMs); put("volume", v.volume.toDouble()) } }))
        put("captions", JSONArray(e.captions.map { c -> JSONObject().apply { put("id", c.id); put("text", c.text); put("s", c.startMs); put("e", c.endMs) } }))
        put("captionStyle", e.captionStyle.name); put("quality", e.quality.name)
    }
    fun reel(o: JSONObject): ReelEdit? = runCatching {
        val dur = o.getLong("sourceDurationMs"); val d = ReelEdit(dur)
        val clips = o.optJSONArray("clips").objs().map { c -> ReelClip(c.getLong("s"), c.getLong("e"), c.optInt("src"), c.enumOr("t", ReelTransition.NONE), c.optLong("freeze")) }
        ReelEdit(
            sourceDurationMs = dur, clips = clips.ifEmpty { d.clips }, speed = o.enumOr("speed", d.speed), filter = o.enumOr("filter", d.filter),
            strength = o.f("strength", 1f), adjust = adjust(o.optJSONObject("adjust")), layers = o.optJSONArray("layers").objs().mapNotNull(::layer),
            originalVolume = o.f("originalVolume", 1f),
            sound = o.optJSONObject("sound")?.let { s -> ReelSound(s.getString("trackId"), s.optString("title"), s.optString("artist"), s.getString("url"), s.optString("licenseLabel"), s.optString("sourceUrl")) },
            soundVolume = o.f("soundVolume", d.soundVolume), coverMs = o.optLong("coverMs"),
            sources = o.optJSONArray("sources").objs().map { s -> ReelSource(s.optString("uri"), s.optLong("durationMs"), s.optString("label")) },
            ramp = o.enumOr("ramp", d.ramp), soundFadeInMs = o.optLong("fadeIn"), soundFadeOutMs = o.optLong("fadeOut"),
            voices = o.optJSONArray("voices").objs().map { v -> VoiceClip(v.getString("id"), v.getString("path"), v.optLong("startMs"), v.optLong("durationMs"), v.f("volume", 1f)) },
            captions = o.optJSONArray("captions").objs().map { c -> Caption(c.getString("id"), c.optString("text"), c.optLong("s"), c.optLong("e")) },
            captionStyle = o.enumOr("captionStyle", d.captionStyle), quality = o.enumOr("quality", d.quality)
        ).takeIf { r -> r.clips.all { it.source in 0..maxOf(0, r.sources.lastIndex) } }
    }.getOrNull()

    /** Everything a draft needs to reopen the editors: photo origins + the reel's original source and edit. */
    data class DraftEdits(
        val photos: Map<String, Pair<String, PhotoEdit>> = emptyMap(),
        val reelSource: String? = null,
        val reel: ReelEdit? = null
    ) {
        val isEmpty get() = photos.isEmpty() && reel == null
        /** Every file the edits point at (so the draft store can copy them into private storage). */
        val files: List<String> get() = photos.values.map { it.first } + listOfNotNull(reelSource) +
            reel?.sources?.map { it.uri }?.filter { it.isNotBlank() }.orEmpty() + reel?.voices?.map { it.path }.orEmpty()
        /** Rewrite every path (edited photo keys too) — used after copying. */
        fun mapFiles(f: (String) -> String) = DraftEdits(
            photos.entries.associate { (k, v) -> f(k) to (f(v.first) to v.second) },
            reelSource?.let(f),
            reel?.copy(sources = reel.sources.map { s -> if (s.uri.isBlank()) s else s.copy(uri = f(s.uri)) }, voices = reel.voices.map { it.copy(path = f(it.path)) })
        )
    }

    fun draft(d: DraftEdits): String = if (d.isEmpty) "" else JSONObject().apply {
        put("photos", JSONArray(d.photos.map { (k, v) -> JSONObject().apply { put("edited", k); put("original", v.first); put("edit", photo(v.second)) } }))
        d.reelSource?.let { put("reelSource", it) }; d.reel?.let { put("reel", reel(it)) }
    }.toString()

    fun draft(s: String): DraftEdits = if (s.isBlank()) DraftEdits() else runCatching {
        val o = JSONObject(s)
        DraftEdits(
            o.optJSONArray("photos").objs().associate { p -> p.getString("edited") to (p.getString("original") to photo(p.getJSONObject("edit"))) },
            o.optString("reelSource").takeIf { it.isNotEmpty() },
            o.optJSONObject("reel")?.let(::reel)
        )
    }.getOrDefault(DraftEdits())
}
