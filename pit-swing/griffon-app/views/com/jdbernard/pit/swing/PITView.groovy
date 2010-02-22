package com.jdbernard.pit.swing

import net.miginfocom.swing.MigLayout

frame = application(title:'Personal Issue Tracker',
  locationRelativeTo: null,
  //size:[320,480],
  pack:true,
  //location:[50,50],
  locationByPlatform:true,
  iconImage: imageIcon('/griffon-icon-48x48.png').image,
  iconImages: [imageIcon('/griffon-icon-48x48.png').image,
               imageIcon('/griffon-icon-32x32.png').image,
               imageIcon('/griffon-icon-16x16.png').image]
) {
    // MENU GOES HERE
    panel(layout: new MigLayout('insets 5 5 5 5')) {
        scrollPane()
    }
}
