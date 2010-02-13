application {
    title='PitSwing'
    startupGroups = ['pit-swing']

    // Should Griffon exit when no Griffon created frames are showing?
    autoShutdown = true

    // If you want some non-standard application class, apply it here
    //frameClass = 'javax.swing.JFrame'
}
mvcGroups {
    // MVC Group for "pit-swing"
    'pit-swing' {
        model = 'PitSwingModel'
        controller = 'PitSwingController'
        view = 'PitSwingView'
    }

}
