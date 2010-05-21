package com.jdbernard.pit.swing

import java.awt.Component
import java.awt.Dimension
import javax.swing.Icon
import javax.swing.JLabel
import javax.swing.JTable
import javax.swing.table.DefaultTableCellRenderer

public class IssueTableCellRenderer extends DefaultTableCellRenderer {

    public Component getTableCellRendererComponent(JTable table, Object value,
    boolean isSelected, boolean hasFocus, int row, int col) {
        super.getTableCellRendererComponent(table, value, isSelected,
            hasFocus, row, col)

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
