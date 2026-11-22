package moe.via.browser.feature.home.ui.favorites

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import moe.via.browser.feature.home.model.HomeFavorite

/** qa.f1: the two child pages share selection and results, without retaining their views. */
internal class HomeFavoritesPanelState : ViewModel() {
    private val pageValue = MutableLiveData<Int>()
    private val selectedValue = MutableLiveData<HomeFavorite>()
    private val savedValue = MutableLiveData<HomeFavorite>()
    private val closeValue = MutableLiveData<Boolean>()
    val page: LiveData<Int> = pageValue
    val selected: LiveData<HomeFavorite> = selectedValue
    val saved: LiveData<HomeFavorite> = savedValue
    val close: LiveData<Boolean> = closeValue
    var passedCount = 0

    fun edit(item: HomeFavorite?) {
        if (item != null) selectedValue.value = item
        pageValue.value = 1
    }

    fun grid(item: HomeFavorite? = null) {
        if (item != null) savedValue.value = item
        pageValue.value = 0
    }

    fun finish(value: Boolean) { closeValue.value = value }
}
