package com.jdbernard.pit.file

import org.joda.time.DateMidnight
import org.joda.time.DateTime

public enum ExtendedPropertyHelp {

    DATE(/^\d{4}-\d{2}-\d{2}$/, DateMidnight,
        { v -> DateMidnight.parse(v) },
        { d -> d.toString("YYYY-MM-dd") }),
    DATETIME(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}$/, DateTime,
        { v -> DateTime.parse(v) },
        { d -> d.toString("YYYY-MM-dd'T'HH:mm:ss") }),
    INTEGER(/^\d+$/, Integer,
        { v -> v as Integer },
        { i -> i as String });

    String pattern;
    Class klass;
    def parseFun, formatFun;

    public ExtendedPropertyHelp(String pattern, Class klass, def parseFun,
    def formatFun) {
        this.pattern = pattern
        this.klass = klass
        this.parseFun = parseFun
        this.formatFun = formatFun }

    public boolean matches(String prop) { return prop ==~ pattern }

    public static Object parse(String value) {
        def result = null
        ExtendedPropertyHelp.values().each { propType ->
            if (propType.matches(value)) { result = propType.parseFun(value) }}

        return result ?: value }

    public static String format(def object) {
        def result = null
        ExtendedPropertyHelp.values().each { propType ->
            if (!result && propType.klass.isInstance(object)) {
                result = propType.formatFun(object) }}

        return result ?: object.toString() }
}
