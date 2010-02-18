package com.jdbernard.pit

import org.junit.After
import org.junit.Before
import org.junit.Test
import static org.junit.Assert.assertTrue
import static org.junit.Assert.assertFalse

class ProjectTest {

    File testDir
    Project rootProj

    @Before void createTestProjects() {
        testDir = new File('testdir')
        testDir.mkdirs()

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

        issueFile = new File(testDir, '0003f2.rst')
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

        rootProj = new Project(testDir)
    }

    @After void deleteTestProjects() {
        testDir.delete()
    }

    @Test void testConstruction() {
         Project proj = new Project(testDir, null)

        assertTrue proj.name == 'testdir'
        assertTrue proj.issues.size() == 3
        assertTrue proj.projects.size() == 1

        // Issue construction in general is under test in IssueTest
        // just check that the issues actually exists
        assertTrue proj.issues['0001'].id == '0001'
        assertTrue proj.issues['0001'].title == 'Test Issue 1'

        assertTrue proj.issues['0002'].id == '0002'
        assertTrue proj.issues['0002'].title == 'Test Bug'

        assertTrue proj.issues['0003'].id == '0003'
        assertTrue proj.issues['0003'].title == 'Important Feature Request'

        // check sub-project behaviour
        assertTrue proj.projects.subproj1 != null
        assertTrue proj.projects.subproj1.name == 'subproj1'
        assertTrue proj.projects.subproj1.issues.size() == 2
        assertTrue proj.projects.subproj1.projects.size() == 0
        assertTrue proj.projects.subproj1.issues['0001'].id == '0001'
        assertTrue proj.projects.subproj1.issues['0001'].title == 'First feature in subproject'
        assertTrue proj.projects.subproj1.issues['0002'].id == '0002'
        assertTrue proj.projects.subproj1.issues['0002'].title == 'Zippners are not zippning.'
    }

    @Test void testRename() {
        assert rootProj.name == 'testdir'

        rootProj.rename('renamedTestDir')

        assertTrue rootProj.name == 'renamedTestDir'
        assertTrue new File('renamedTestDir').exists()
    }

    /*@Test void testEachIssue() {
        def expectedList = [rootProj.issues['0001'],
            rootProj.issues['0002'], rootProj.issues['0003']]

        // sort using default ordering (ids ascending)
        def actualList = []
        rootProj.eachIssue { actualList << it }

        assertArrayEquals expectedList, actualList

        // sort using reverse ordering (ids descending)
        expectedList = expectedList.reverse()
        actualList = []

        rootProj.eachIssue(
            new Filter(issueSorter: { -(it.id.toInteger()) }))
            { actualList << it }
    }*/

}
