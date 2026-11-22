package moe.via.browser.feature.ai.text.table

import io.noties.markwon.AbstractMarkwonPlugin
import io.noties.markwon.MarkwonVisitor
import org.commonmark.ext.gfm.tables.*
import org.commonmark.node.Node
import org.commonmark.parser.Parser

/** o6.b's GFM collector; row layout is owned by AiTableRowSpan. Based on Markwon (Apache-2.0). */
internal class AiTablePlugin(private val style: AiTableStyle) : AbstractMarkwonPlugin() {
    private var pending: MutableList<AiTableCell>? = null
    private var header = false
    private var rows = 0
    override fun configureParser(builder: Parser.Builder) { builder.extensions(listOf(TablesExtension.create())) }
    override fun beforeRender(node: Node) { pending = null; header = false; rows = 0 }
    override fun configureVisitor(builder: MarkwonVisitor.Builder) {
        builder.on(TableBlock::class.java) { visitor, block ->
            visitor.blockStart(block)
            val start = visitor.length(); visitor.visitChildren(block); visitor.setSpans(start, AiTableBlockSpan()); visitor.blockEnd(block)
        }.on(TableBody::class.java) { visitor, body -> visitor.visitChildren(body); rows = 0 }
            .on(TableRow::class.java) { visitor, row -> visitRow(visitor, row) }
            .on(TableHead::class.java) { visitor, head -> visitRow(visitor, head) }
            .on(TableCell::class.java) { visitor, cell ->
                val start = visitor.length(); visitor.visitChildren(cell)
                val cells = pending ?: ArrayList<AiTableCell>(2).also { pending = it }
                val alignment = when (cell.alignment) { TableCell.Alignment.CENTER -> 1; TableCell.Alignment.RIGHT -> 2; else -> 0 }
                cells.add(AiTableCell(alignment, visitor.builder().removeFromEnd(start))); header = cell.isHeader
            }
    }
    private fun visitRow(visitor: MarkwonVisitor, node: Node) {
        val start = visitor.length(); visitor.visitChildren(node)
        val cells = pending ?: return
        val text = visitor.builder()
        val newline = text.length > 0 && text[text.length - 1] != '\n'
        if (newline) visitor.forceNewLine()
        text.append('\u00a0')
        val span = AiTableRowSpan(style, cells, header, rows % 2 == 1)
        rows = if (header) 0 else rows + 1
        visitor.setSpans(if (newline) start + 1 else start, span); pending = null
    }
}
