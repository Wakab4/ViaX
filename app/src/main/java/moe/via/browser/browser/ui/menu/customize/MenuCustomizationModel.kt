package moe.via.browser.browser.ui.menu.customize

import moe.via.browser.browser.ui.menu.MenuAction
import moe.via.browser.browser.ui.menu.MenuPreferences

/** i8.s owns two ordered groups and moves their original wrapper objects between them. */
internal class MenuCustomizationModel(private val preferences: MenuPreferences) {
    data class Item(val action: MenuAction, var added: Boolean)
    sealed interface Row {
        data class Heading(val displayed: Boolean) : Row
        data class Action(val item: Item) : Row
    }
    private val displayed = mutableListOf<Item>()
    private val hidden = mutableListOf<Item>()
    val rows = mutableListOf<Row>()
    init { load() }
    private fun load() {
        displayed.clear(); hidden.clear()
        val remaining = MenuAction.entries.associateBy { it.id }.toMutableMap()
        for (id in preferences.displayed()) remaining.remove(id)?.let { displayed += Item(it, true) }
        for (id in preferences.hidden()) remaining.remove(id)?.let { hidden += Item(it, false) }
        remaining.toSortedMap().values.forEach { hidden += Item(it, false) }
        ensureSettings(); rebuild()
    }
    private fun ensureSettings() {
        hidden.firstOrNull { it.action == MenuAction.SETTINGS }?.let { hidden.remove(it); it.added = true; displayed += it; return }
        if (displayed.none { it.action == MenuAction.SETTINGS }) displayed += Item(MenuAction.SETTINGS, true)
    }
    private val boundary get() = displayed.size + 1
    fun item(position: Int): Item? = (rows.getOrNull(position) as? Row.Action)?.item
    private fun position(item: Item): Int = displayed.indexOf(item).takeIf { it >= 0 }?.plus(1)
        ?: hidden.indexOf(item).takeIf { it >= 0 }?.let { displayed.size + 2 + it } ?: -1
    fun toggle(item: Item): Int {
        if (item.action == MenuAction.SETTINGS) return -1
        if (displayed.remove(item)) { item.added = false; hidden.add(0, item) }
        else if (hidden.remove(item)) { item.added = true; displayed.add(item) } else return -1
        rebuild(); return position(item)
    }
    fun move(item: Item, destination: Int): Int {
        if (position(item) == -1 || destination !in rows.indices || destination == 0) return -1
        if (item.action == MenuAction.SETTINGS && destination >= boundary) return -1
        val fromVisible = displayed.remove(item)
        val fromHidden = !fromVisible && hidden.remove(item)
        if (!fromVisible && !fromHidden) return -1
        when {
            destination == boundary -> if (fromVisible) { item.added = true; displayed.add(item) }
                else { item.added = false; hidden.add(0, item) }
            destination > 0 && destination < boundary -> { item.added = true; displayed.add((destination - 1).coerceIn(0, displayed.size), item) }
            destination > boundary && destination < rows.size -> { item.added = false; hidden.add((destination - boundary - 1).coerceIn(0, hidden.size), item) }
            else -> { item.added = true; displayed.add(item) }
        }
        rebuild(); return position(item)
    }
    private fun rebuild() {
        rows.clear(); rows.add(Row.Heading(true)); displayed.forEach { rows.add(Row.Action(it)) }
        rows.add(Row.Heading(false)); hidden.forEach { rows.add(Row.Action(it)) }
    }
    fun reset() { preferences.reset(); load() }
    fun save() { ensureSettings(); preferences.save(displayed.map { it.action.id }, hidden.map { it.action.id }) }
}
