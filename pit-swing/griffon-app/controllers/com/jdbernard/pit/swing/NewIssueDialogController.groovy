package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status

class NewIssueDialogController {
    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {
        // this method is called after model and view are injected
    }

    def show = {
        view.titleTextField.text = ""
        view.categoryComboBox.selectedItem = Category.BUG
        view.statusComboBox.selectedItem = Status.NEW
        view.prioritySpinner.setValue(5)
        view.dialog.visible = true
    }
}
