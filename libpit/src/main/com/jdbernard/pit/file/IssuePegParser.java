package com.jdbernard.pit.file;

import java.util.HashMap;
import java.util.Map;

import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.*;

@BuildParseTree
public class IssuePegParser extends BaseParser<Object> {

    public Rule IssueFile() {
        return Sequence(push(makeNode()),
            Title(), Body(), Optional(PropertyBlock())); }

    Rule Title() {
        return Sequence(
            OneOrMore(NOT_EOL), addToNode("title", match()), EOL,
            HorizontalRule(), EOL,
            ZeroOrMore(SPACE), EOL); }

    Rule Body() { return Sequence(OneOrMore(Sequence(
        TestNot(PropertyBlock()), ANY)), addToNode("body", match())); }

    Rule PropertyBlock() {
        return Sequence(push(makeNode()),
            HorizontalRule(), OneOrMore(EOL), TableSeparator(), EOL,
            OneOrMore(PropertyDefinition()), TableSeparator(),
            addToNode("extProperties", pop())); }

    Rule PropertyDefinition() {
        return Sequence(
            PropertyKey(), push(match()), COLON,
            PropertyValue(), push(match()), EOL,
            swap(), addToNode(popAsString().trim(), popAsString().trim())); }

    Rule PropertyKey() { return OneOrMore(Sequence(TestNot(COLON), NOT_EOL)); }

    Rule PropertyValue() { return OneOrMore(NOT_EOL); }

    Rule TableSeparator() {
        return Sequence(OneOrMore(SEPARATOR_CHAR), OneOrMore(SPACE),
            OneOrMore(SEPARATOR_CHAR)); }

    Rule HorizontalRule() {
        return Sequence(SEPARATOR_CHAR, SEPARATOR_CHAR, SEPARATOR_CHAR, 
            OneOrMore(SEPARATOR_CHAR)); }

    Rule EOL = Ch('\n');
    Rule NOT_EOL = Sequence(TestNot(EOL), ANY);
    Rule SEPARATOR_CHAR = AnyOf("\"'`~:-_=+*^#<>");
    Rule SPACE = AnyOf(" \t");
    Rule COLON = Ch(':');

    Map makeNode() { return new HashMap(); }

    boolean addToNode(Object key, Object val) {
        Map node = (Map) pop();
        node.put(key, val);
        push(node);
        return true; }

    String popAsString() { return (String) pop(); }
}
