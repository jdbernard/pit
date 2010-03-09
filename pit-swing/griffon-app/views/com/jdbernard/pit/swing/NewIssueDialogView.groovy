package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import java.awt.GridBagConstraints as GBC
import javax.swing.DefaultComboBoxModel

dialog = dialog(title: 'New Task...', modal: true, pack: true,
    locationRelativeTo: null) {

    gridBagLayout()

    label('Title/Summary:',
        constraints: gbc(gridx: 0, gridy: 0, gridwidth: 3,
            insets: [5, 5, 0, 5], fill: GBC.HORIZONTAL))
    titleTextField = textField(
        constraints: gbc(gridx: 0, gridy: 1, gridwidth: 3,
            insets: [0, 10, 0, 5], fill: GBC.HORIZONTAL),
        keyTyped: { model.text = titleTextField.text })

    label('Category:', 
        constraints: gbc(gridx: 0, gridy: 2, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    categoryComboBox = comboBox(
        constraints: gbc(gridx: 1, gridy: 2, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: new DefaultComboBoxModel(Category.values()),
        editable: false,
        itemStateChanged: { model.category = categoryComboBox.selectedValue })

    label('Status:',
        constraints: gbc(gridx: 0, gridy: 3, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    statusComboBox = comboBox(
        constraints: gbc(gridx: 1, gridy: 3, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: new DefaultComboBoxModel(Status.values()),
        editable: false,
        itemStateChanged: { model.status = statusComboBox.selectedValue })

    label('Priority (0-9, 0 is highest priority):',
        constraints: gbc(gridx: 0, gridy: 4, insets: [5, 5, 0, 0],
            fill: GBC.HORIZONTAL))
    prioritySpinner = spinner(
        constraints: gbc( gridx: 1, gridy: 4, insets: [5, 5, 0, 5],
            fill: GBC.HORIZONTAL),
        model: spinnerNumberModel(maximum: 9, minimum: 0),
        stateChanged: { model.priority = prioritySpinner.value })

    button('Cancel',
        actionPerformed: {
            model.accept = false
            dialog.visible = false
        },
        constraints: gbc(gridx: 0, gridy: 5, insets: [5, 5, 5, 5],
            anchor: GBC.EAST))
    button('Create Issue',
        actionPerformed: {
            model.accept = true
            dialog.visible = false
        },
        constraints: gbc(gridx: 1, gridy: 5, insets: [5, 5, 5, 5],
            anchor: GBC.WEST))
}
