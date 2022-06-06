package NoMathExpectation.NMEBoot.RDLounge.rhythmCafe

import kotlinx.serialization.Serializable

@Serializable
internal data class MatchedField(val field: String,
                                 val indices: List<Int>? = null,
                                 @Serializable(with = MatchedTokensDeserializer::class)
                                 val matched_tokens: List<List<String>>,
                                 val snippet: String? = null,
                                 val snippets: List<String>? = null)