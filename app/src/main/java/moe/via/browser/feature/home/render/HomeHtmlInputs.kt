package moe.via.browser.feature.home.render

import moe.via.browser.feature.home.model.HomeFavoriteTile

/** r8.d.h reads these values only after the HTML dirty/file gate. */
data class HomeHtmlInputs(val logoContent: String?, val favorites: List<HomeFavoriteTile>)
