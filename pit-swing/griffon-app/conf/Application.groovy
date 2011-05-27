application {
    title = 'PitSwing'
    startupGroups = ['PIT']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "ProjectPanel"
    'ProjectPanel' {
        model = 'com.jdbernard.pit.swing.ProjectPanelModel'
        view = 'com.jdbernard.pit.swing.ProjectPanelView'
        controller = 'com.jdbernard.pit.swing.ProjectPanelController'
    }

    // MVC Group for "NewIssueDialog"
    'NewIssueDialog' {
        model = 'com.jdbernard.pit.swing.NewIssueDialogModel'
        view = 'com.jdbernard.pit.swing.NewIssueDialogView'
        controller = 'com.jdbernard.pit.swing.NewIssueDialogController'
    }

    // MVC Group for "PIT"
    'PIT' {
        model = 'com.jdbernard.pit.swing.PITModel'
        view = 'com.jdbernard.pit.swing.PITView'
        controller = 'com.jdbernard.pit.swing.PITController'
    }

}
