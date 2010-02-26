package com.jdbernard.pit

public enum Category {
    BUG,
    FEATURE,
    TASK

    public static Category toCategory(String s) {
        for(c in Category.values())
            if (c.name().startsWith(s.toUpperCase())) return c
        throw new IllegalArgumentException("No category matches ${s}.")
    }   

    public String getSymbol() { toString()[0].toLowerCase() }

    public String toString() { return "${name()[0]}${name()[1..-1].toLowerCase()}" }
}
