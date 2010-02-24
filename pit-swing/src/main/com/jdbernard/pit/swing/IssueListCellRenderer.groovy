package com.jdbernard.pit.swing

import java.awt.Component
import javax.swing.Icon
import javax.swing.JList
import javax.swing.DefaultListCellRenderer

public class IssueListCellRenderer extends DefaultListCellRenderer {

    Map<Category, Icon> issueIcons
    
    public Component getListCellRendererComponent(JList list, Object value,
    int index, boolean selected, boolean hasFocus) {
        def component = super.getListCellRendererComponent(list, value, index,
            selected, hasFocus)
        if (issueIcons[(value.category)])
            component.setIcon(issueIcons[(value.category)])
        component.text = "<html><tt>${value.id} (${value.priority}): </tt>" +
            "${value.title}</html>"
        return component
    }
}
