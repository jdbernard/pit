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
        Status retVal = null
        for(status in Status.values())  {
            if (status.symbol.equalsIgnoreCase(str) ||
                status.name().startsWith(str.toUpperCase())) {

                if (retVal != null)
                    throw new IllegalArgumentException("Request string is" +
                        " ambigous, '${str}' could represent ${retVal} or " +
                        "${status}, possibly others.")

                retVal = status
            }
        }

        if (retVal == null)
            throw new IllegalArgumentException("No status matches '${str}'")

        return retVal
    }

    public String toString() {
        def words = name().split("_")
        String result = ""
        words.each { result += "${it[0]}${it[1..-1].toLowerCase()} " }
        return result[0..-2]
    }
}
