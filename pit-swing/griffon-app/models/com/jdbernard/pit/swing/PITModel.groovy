package com.jdbernard.pit.swing

import com.jdbernard.pit.Category
import com.jdbernard.pit.Filter
import com.jdbernard.pit.Issue
import com.jdbernard.pit.Project
import com.jdbernard.pit.Status
import groovy.beans.Bindable

class PITModel {
    @Bindable Project rootProject

    // cache the ListModels
    def projectListModels = [:]

    // filter for projects and issues
    Filter filter = new Filter(categories: [],
        status: [Status.NEW, Status.VALIDATION_REQUIRED])

    @Bindable Project popupProject = null

    @Bindable Project selectedProject = null

    @Bindable Issue popupIssue = null

    // configurable exntension points
    // ==============================

    @Bindable def issueListRenderer

    // map of category -> issue template
    def templates = [:]

}
