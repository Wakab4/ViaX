package moe.pure.browser.feature.home.ui.customize

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.pure.browser.feature.home.data.HomePreferences
import moe.pure.browser.feature.home.model.HomeDesign

/** d9.t retains five mutable design objects and four ordinary MutableLiveData channels. */
internal class HomeCustomizeState(context: Context) : ViewModel() {
    val preferences = HomePreferences(context.applicationContext)
    val favoriteDesign = preferences.favoriteDesign
    val logoDesign = preferences.logoDesign
    val searchDesign = preferences.searchDesign
    val generalDesign = preferences.generalDesign
    val backgroundDesign = preferences.backgroundDesign
    private val documentValue = MutableLiveData<Boolean>()
    private val backgroundValue = MutableLiveData<Unit?>()
    private val cacheValue = MutableLiveData<Boolean>()
    private val closeValue = MutableLiveData<Boolean>()
    val document: LiveData<Boolean> = documentValue
    val background: LiveData<Unit?> = backgroundValue
    val cache: LiveData<Boolean> = cacheValue
    val close: LiveData<Boolean> = closeValue

    fun documentChanged() { documentValue.value = true }
    fun backgroundChanged() { backgroundValue.value = null }
    fun cacheCleared() { cacheValue.value = true }
    fun saveBackground(notifyDocument: Boolean = true) {
        preferences.backgroundDesign = backgroundDesign
        if (notifyDocument) documentChanged()
    }
    fun saveGeneral() { preferences.generalDesign = generalDesign; documentChanged() }
    fun saveFavorites() { preferences.favoriteDesign = favoriteDesign; documentChanged() }
    fun saveLogo() { preferences.logoDesign = logoDesign; documentChanged() }
    fun saveSearch() { preferences.searchDesign = searchDesign; documentChanged() }
    fun reset() {
        preferences.resetCustomization(HomeDesign(backgroundDesign, generalDesign, favoriteDesign, logoDesign, searchDesign))
        preferences.markDirty()
        cacheValue.value = true; documentValue.value = true; closeValue.value = true; backgroundValue.value = null
    }
}
