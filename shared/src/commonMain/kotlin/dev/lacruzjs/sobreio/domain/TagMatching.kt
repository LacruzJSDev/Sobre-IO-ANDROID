package dev.lacruzjs.sobreio.domain

import dev.lacruzjs.sobreio.domain.model.Tag

/** The longest a tag name can be (`tags.name` is `varchar(50)`). */
const val MAX_TAG_NAME_LENGTH = 50

/** The shortest key on which a one-letter difference still counts as "close": «gas» and «gap» are different words. */
private const val MIN_CLOSE_KEY_LENGTH = 4

private val whitespace = Regex("\\s+")

/** Letters with a diacritic and what they become, the same idea as the database's `unaccent`. */
private val accentFold: Map<Char, String> = buildMap {
    fun fold(plain: String, accented: String) = accented.forEach { put(it, plain) }
    fold("a", "àáâãäåāăąǎ")
    fold("c", "çćĉċč")
    fold("d", "ďđ")
    fold("e", "èéêëēĕėęě")
    fold("g", "ĝğġģ")
    fold("h", "ĥħ")
    fold("i", "ìíîïĩīĭįıǐ")
    fold("j", "ĵ")
    fold("k", "ķ")
    fold("l", "ĺļľŀł")
    fold("n", "ñńņňŉ")
    fold("o", "òóôõöōŏőøǒ")
    fold("r", "ŕŗř")
    fold("s", "śŝşš")
    fold("t", "ţťŧ")
    fold("u", "ùúûüũūŭůűųǔ")
    fold("w", "ŵ")
    fold("y", "ýÿŷ")
    fold("z", "źżž")
    put('ß', "ss")
    put('æ', "ae")
    put('œ', "oe")
}

/** The name as it is stored: trimmed, with inner whitespace collapsed to one space. */
fun normalizeTagName(name: String): String = name.trim().replace(whitespace, " ")

/**
 * The comparison key of a tag name, a mirror of the database's `private.tag_key`: trimmed, inner whitespace collapsed,
 * accents removed and lower case. «Súper», «super» and « SUPER » share one key, so they are the same tag.
 */
fun normalizeTagKey(name: String): String = buildString {
    normalizeTagName(name).lowercase().forEach { c -> append(accentFold[c] ?: c.toString()) }
}

/** True when the two keys are equal or differ by one inserted, deleted or replaced letter. */
internal fun isWithinOneEdit(a: String, b: String): Boolean {
    if (a == b) return true
    if (kotlin.math.abs(a.length - b.length) > 1) return false
    val (shorter, longer) = if (a.length <= b.length) a to b else b to a
    var i = 0
    while (i < shorter.length && shorter[i] == longer[i]) i++
    return if (shorter.length == longer.length) {
        // One replaced letter
        shorter.substring(i + 1) == longer.substring(i + 1)
    } else {
        // One extra letter in the longer one
        shorter.substring(i) == longer.substring(i + 1)
    }
}

/**
 * What the tag picker shows for a search text: the tags whose key contains it ([rows]), a [suggestion] when a tag is
 * the same one or a close one («¿Quisiste decir…?») and [createName] when the text can become a new tag. An exact key
 * match never offers to create, because the database would refuse the duplicate.
 */
data class TagPickerOptions(
    val rows: List<Tag>,
    val suggestion: Tag?,
    val createName: String?,
)

/** See [TagPickerOptions]. [tags] are the live tags of the group; matching ignores case, accents and extra spaces. */
fun tagPickerOptions(tags: List<Tag>, query: String): TagPickerOptions {
    val key = normalizeTagKey(query)
    if (key.isEmpty()) return TagPickerOptions(rows = tags, suggestion = null, createName = null)
    val keys = tags.associateWith { normalizeTagKey(it.name) }
    val rows = tags.filter { keys.getValue(it).contains(key) }
    val exact = tags.firstOrNull { keys.getValue(it) == key }
    val close = exact ?: if (key.length >= MIN_CLOSE_KEY_LENGTH) {
        tags.firstOrNull { keys.getValue(it).length >= MIN_CLOSE_KEY_LENGTH && isWithinOneEdit(keys.getValue(it), key) }
    } else {
        null
    }
    val name = normalizeTagName(query)
    return TagPickerOptions(
        rows = rows,
        suggestion = close,
        createName = name.takeIf { exact == null && it.length <= MAX_TAG_NAME_LENGTH },
    )
}
