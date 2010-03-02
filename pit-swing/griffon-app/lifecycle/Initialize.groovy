/*
 * This script is executed inside the EDT, so be sure to
 * call long running code in another thread.
 *
 * You have the following options
 * - SwingBuilder.doOutside { // your code  }
 * - Thread.start { // your code }
 * - SwingXBuilder.withWorker( start: true ) {
 *      onInit { // initialization (optional, runs in current thread) }
 *      work { // your code }
 *      onDone { // finish (runs inside EDT) }
 *   }
 *
 * You have the following options to run code again inside EDT
 * - SwingBuilder.doLater { // your code }
 * - SwingBuilder.edt { // your code }
 * - SwingUtilities.invokeLater { // your code }
 */

import groovy.swing.SwingBuilder
import griffon.util.GriffonPlatformHelper
import griffon.util.GriffonApplicationHelper

GriffonPlatformHelper.tweakForNativePlatform(app)
SwingBuilder.lookAndFeel('mac', 'org.pushingpixels.substance.api.skin.SubstanceCremeCoffeeLookAndFeel', 'nimbus', ['metal', [boldFonts: false]])

// make config directory
def confDir = new File(System.getProperty('user.home'), '.pit')
if (!confDir.exists()) confDir.mkdirs()
// find or create configuration file
def swingConf = new File(confDir, 'pit-swing.groovy')
if (!swingConf.exists()) swingConf.createNewFile()
// run config
GriffonApplicationHelper.runScriptInsideEDT(swingConf.canonicalPath, app)
