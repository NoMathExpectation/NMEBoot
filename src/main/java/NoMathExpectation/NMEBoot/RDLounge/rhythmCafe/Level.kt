package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class Level(
    val approval: Int,
    val artist: String,
    val artist_tokens: List<String>,
    val authors: List<String>,
    val description: String,
    val description_ct: String,
    val difficulty: Int,
    val has_classics: Boolean,
    val has_freetimes: Boolean,
    val has_freezeshots: Boolean,
    val has_holds: Boolean,
    val has_oneshots: Boolean,
    val has_skipshots: Boolean,
    val has_squareshots: Boolean,
    val has_window_dance: Boolean,
    val hue: Float,
    val icon: String,
    val id: String,
    val image: String,
    val indexed: Long? = null,
    val last_updated: Long,
    val max_bpm: Float,
    val min_bpm: Float,
    val seizure_warning: Boolean,
    val rdlevel_sha1: String,
    val single_player: Boolean,
    val song: String,
    val source: String,
    val source_iid: String,
    val tags: List<String>,
    val thumb: String,
    val two_player: Boolean,
    val url: String,
    val url2: String
) {
    fun getDifficulty() = when (difficulty) {
        0 -> "简单"
        1 -> "普通"
        2 -> "困难"
        3 -> "噩梦"
        else -> difficulty.toString()
    }

    fun peerReviewed() = when (approval) {
        in Int.MIN_VALUE..-1 -> "x"
        in 0..9 -> "-"
        in 10..Int.MAX_VALUE -> "√"
        else -> approval.toString()
    }
}
