package com.jdbernard.pit.swing

import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

public class IssueTableCellRenderer extends DefaultTableCellRenderer {

    public static List textColors = [
        Color.getHSBColor(0f, 1f, 0.8f),    // 0 - Highest priority
        Color.getHSBColor(0f, 1f, 0.6f),    // 1 - High priority
        Color.getHSBColor(0f, 1f, 0.4f),
        Color.getHSBColor(0f, 1f, 0.2f),    // 3 - Medium-high priority
        Color.getHSBColor(0f, 0f, 0f),
        Color.getHSBColor(0f, 0f, 0f),      // 5 - Medium priority
        Color.getHSBColor(0f, 0f, 0.1f),
        Color.getHSBColor(0f, 0f, 0.2f),    // 7 - Medium-low priority
        Color.getHSBColor(0f, 0f, 0.4f),
        Color.getHSBColor(0f, 0f, 0.6f),    // 9 - Lowest priority
    ]

    private Font boldFont
    private Font plainFont

    public IssueTableCellRenderer() {
        super()
        plainFont = getFont()
        boldFont = plainFont.deriveFont(Font.BOLD)
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected,
            hasFocus, row, col)

        // set row color
        int modelRow = table.convertRowIndexToModel(row)
        int priority = table.model.getValueAt(modelRow, 2)[1..-4].toInteger()
        if (priority <= 3) setFont(boldFont)
        else setFont(plainFont)
        setForeground(textColors[priority])

        if (value instanceof String) {
            if (col > 2) {
                setHorizontalAlignment(JLabel.LEADING)
            } else {
                setText("<html><tt>" + getText() + "</tt></html>")
                setHorizontalAlignment(JLabel.TRAILING)
            }

        }

        return this
    }

}
