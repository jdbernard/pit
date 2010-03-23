package com.jdbernard.pit.swing



import org.fest.swing.fixture.*
import griffon.test.FestSwingTestCase

import javax.swing.JDialog

class NewIssueDialogTestTests extends FestSwingTestCase {
    // instance variables:
    // app    - current application
    // window - value returned from initWindow()
    //          defaults to app.appFrames[0]

    JDialog newIssueDialog

    void testSomething() {

    }

    protected void onSetUp() throws Exception {
        println app.appFrames
    }

    protected void onTearDown() throws Exception { }

    /*
    protected FrameFixture initWindow() {
      return new FrameFixture(app.appFrames[0])
    }
    */
}
