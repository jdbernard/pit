application {
    title='PitSwing'
    startupGroups = ['pit-swing']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "com.jdbernard.pit.swing.PIT"
    'PIT' {
        model = 'com.jdbernard.pit.swing.PITModel'
        controller = 'com.jdbernard.pit.swing.PITController'
        view = 'com.jdbernard.pit.swing.PITView'
    }

    // MVC Group for "pit-swing"
    'pit-swing' {
        model = 'PitSwingModel'
        controller = 'PitSwingController'
        view = 'PitSwingView'
    }

}
