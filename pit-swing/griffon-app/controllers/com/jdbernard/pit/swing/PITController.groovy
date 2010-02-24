package com.jdbernard.pit.swing

import com.jdbernard.pit.FileProject

class PITController {
    // these will be injected by Griffon
    def model
    def view

    void mvcGroupInit(Map args) {
        model.rootProject = new FileProject(new File('.'))
    }

    /*
    def action = { evt = null ->
    }
    */
}
