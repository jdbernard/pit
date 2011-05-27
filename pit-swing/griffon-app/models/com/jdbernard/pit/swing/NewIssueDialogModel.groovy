package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Status
import groovy.beans.Bindable

class NewIssueDialogModel {
    @Bindable boolean accept
    String text
    Category category
    Status status
    int priority
}
