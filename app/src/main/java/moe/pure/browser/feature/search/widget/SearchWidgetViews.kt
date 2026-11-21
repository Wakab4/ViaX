package moe.pure.browser.feature.search.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import moe.pure.browser.BrowserActivity
import moe.pure.browser.R
import moe.pure.browser.feature.search.SearchController

/** Both visual variants open the same existing browser search action and carry no query. */
internal object SearchWidgetViews {
    fun update(context: Context, manager: AppWidgetManager, widgetIds: IntArray, layout: Int) {
        val intent = Intent(context, BrowserActivity::class.java)
            .setAction(SearchController.SEARCH_ACTION)
            .setPackage(context.packageName)
        val open = PendingIntent.getActivity(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        widgetIds.forEach { id ->
            if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val views = RemoteViews(context.packageName, layout)
                views.setOnClickPendingIntent(R.id.ref_search_widget_click_area, open)
                manager.updateAppWidget(id, views)
            }
        }
    }
}
