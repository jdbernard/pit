package com.jdbernard.pit

public enum Category {
    BUG,
    FEATURE,
    TASK,
    CLOSED

    public static Category toCategory(String s) {
        for(c in Category.values())
            if (c.toString().startsWith(s.toUpperCase())) return c
        throw new IllegalArgumentException("No category matches ${s}.")
    }   
}
