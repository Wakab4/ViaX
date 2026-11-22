package moe.via.browser.feature.search.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import moe.via.browser.R

class SearchWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        super.onUpdate(context, manager, widgetIds)
        SearchWidgetViews.update(context, manager, widgetIds, R.layout.ref_search_widget)
    }
}
