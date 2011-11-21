import com.jdbernard.pit.*
import com.jdbernard.pit.file.*

import org.parboiled.Parboiled
import org.parboiled.parserunners.ReportingParseRunner
import org.parboiled.parserunners.RecoveringParseRunner

parser = Parboiled.createParser(IssuePegParser.class)
parseRunner = new ReportingParseRunner(parser.IssueFile())
issueFile = new File('/Volumes/NO NAME/Dropbox/tasks/0015tn3.rst')
issueText = issueFile.text
result = parseRunner.run(issueText)
issueMap = result.valueStack.pop()
