package com.jdbernard.pit

public enum Status {
    REASSIGNED('a'),
    REJECTED('j'),
    NEW('n'),
    RESOLVED('s'),
    VALIDATION_REQUIRED('v')

    String symbol

    protected Status(String s) { symbol = s }

    public static Status toStatus(String str) {
        // Try to match based on symbol
        def match = Status.values().find {it.symbol.equalsIgnoreCase(str)}
        if (match) { return match }

        // No match on the symbol, look for the status name (or abbreviations)
        match = Status.values().findAll {
            it.name().startsWith(str.toUpperCase()) }

        // No matching status, oops.
        if (match.size() == 0) {
            throw new IllegalArgumentException("No status matches '${str}'") }

        // More than one matching status, oops.
        else if (match.size() > 1) {
            throw new IllegalArgumentException("Request string is" +
                " ambigous, '${str}' could represent any of ${match}.")}

        // Only one matching status, yay!
        else { return match[0] }}

    public String toString() {
        def words = name().split("_")
        String result = ""
        words.each { result += "${it[0]}${it[1..-1].toLowerCase()} " }
        return result[0..-2]
    }
}
