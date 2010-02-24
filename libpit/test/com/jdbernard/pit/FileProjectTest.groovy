package com.jdbernard.pit

import org.junit.After
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertEquals
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertNotNull
import static org.junit.Assert.assertTrue

class FileProjectTest {

    File testDir
    Project rootProj

    @Before void createTestProjects() {

        testDir = new File('testdir')
        assert !testDir.exists()
        testDir.mkdirs()

        /* TEST SUITE:
            /testdir/
                0001t5.rst
                0002b5.rst
                0003f2.rst

                subproj1/
                    0001f3.rst
                    0002b4.rst

                emptyproj/

        */

        def issueFile = new File(testDir, '0001t5.rst')
        issueFile.createNewFile()
        issueFile.write('Test Issue 1\n' +
                        '============\n\n' +
                        'This is the first test issue.')

        issueFile = new File(testDir, '0002b5.rst')
        issueFile.createNewFile()
        issueFile.write('Test Bug\n' +
                        '========\n\n' +
                        'Yeah, it is a test bug.')

        issueFile = new File(testDir, '0003c2.rst')
        issueFile.createNewFile()
        issueFile.write('Important Feature Request\n' +
                        '=========================\n\n' +
                        'Here is our sweet feature. Please implement it!')

        def subDir = new File(testDir, 'subproj1')
        subDir.mkdirs()

        issueFile = new File(subDir, '0001f3.rst')
        issueFile.createNewFile()
        issueFile.write('First feature in subproject\n' +
                        '===========================\n\n' +
                        'Please make the grubblers grobble.')

        issueFile = new File(subDir, '0002b4.rst')
        issueFile.createNewFile()
        issueFile.write('Zippners are not zippning.\n' +
                        '==========================\n\n' +
                'For some reason, the Zippners are bilperring, not zippning.')

        subDir = new File(testDir, 'emptyproj')
        subDir.mkdirs()

        rootProj = new FileProject(testDir)
    }

    @After void deleteTestProjects() {
        assert testDir.deleteDir()

        if (rootProj.source.exists())
            assert rootProj.source.deleteDir()
    }

    @Test void testConstruction() {
         Project proj = new FileProject(testDir)

        assertEquals proj.name,             'testdir'
        assertEquals proj.issues.size(),    3
        assertEquals proj.projects.size(),  2

        // Issue construction in general is under test in IssueTest
        // just check that the issues actually exists
        assertEquals proj.issues['0001'].id, '0001'
        assertEquals proj.issues['0001'].title, 'Test Issue 1'

        assertEquals proj.issues['0002'].id, '0002'
        assertEquals proj.issues['0002'].title, 'Test Bug'

        assertEquals proj.issues['0003'].id, '0003'
        assertEquals proj.issues['0003'].title, 'Important Feature Request'

        // check sub-project behaviour
        assertNotNull proj.projects.subproj1
        assertEquals proj.projects.subproj1.name,               'subproj1'
        assertEquals proj.projects.subproj1.issues.size(),      2
        assertEquals proj.projects.subproj1.projects.size(),    0
        assertEquals proj.projects.subproj1.issues['0001'].id,  '0001'
        assertEquals proj.projects.subproj1.issues['0002'].id,  '0002'
        assertEquals proj.projects.subproj1.issues['0001'].title,
            'First feature in subproject'
        assertEquals proj.projects.subproj1.issues['0002'].title,
            'Zippners are not zippning.'

        assertNotNull proj.projects.emptyproj
        assertEquals proj.projects.emptyproj.issues.size(), 0
        assertEquals proj.projects.emptyproj.projects.size(), 0
    }

    @Test void testRename() {
        assert rootProj.name == 'testdir'

        rootProj.name = 'renamedTestDir'

        assertEquals rootProj.name, 'renamedTestDir'
        assertTrue new File('renamedTestDir').exists()

        assert rootProj.source.deleteDir()
    }

    @Test void testCreateNewIssue() {

        // test correct increment of id, application of values
        def newIssue = rootProj.createNewIssue(category: Category.BUG,
            priority: 4, text: 'A newly made bug report.\n'+
                               '========================\n\n' +
                               'Testing the Project.createNewIssue() method.')

        assertEquals newIssue.id,       '0004'
        assertEquals newIssue.priority, 4
        assertEquals newIssue.text, 'A newly made bug report.\n'+
                                    '========================\n\n' +
                                    'Testing the Project.createNewIssue() method.'
        assertEquals rootProj.issues[(newIssue.id)], newIssue

        //test defaults and creation of issue in an empty project
        newIssue = rootProj.projects.emptyproj.createNewIssue()

        assertEquals newIssue.id,       '0000'
        assertEquals newIssue.priority, 5
        assertEquals newIssue.text,     'Default issue title.\n' +
                                        '====================\n'

        assertEquals rootProj.projects.emptyproj.issues[(newIssue.id)],
            newIssue

    }

}
