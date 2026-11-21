package moe.pure.browser.feature.scripts.runtime

/** i6.j0.k keeps only ASCII letters, digits and underscores in generated JS identifiers. */
internal object NativeGmNames {
    fun identifier(value: String) = value.filter { it in 'A'..'Z' || it in 'a'..'z' || it in '0'..'9' || it == '_' }
    fun prompt(secret: String) = identifier("NP_$secret")
}
