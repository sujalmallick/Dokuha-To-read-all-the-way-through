package core.platform

/**
 * Normalizes a string using Unicode Normalization Form KC (NFKC) so
 * full-width characters, compatibility ligatures, and decomposed forms
 * map to a single canonical representation.
 */
expect fun normalizeUnicodeNfkc(input: String): String
