package com.ayane.advancementssearch;

import java.util.Locale;

public enum SearchByType {

    EVERYWHERE,
    TITLE,
    DESCRIPTION,
    ICON;

    private static final String QUERY_SEPARATOR = ":";

    public static SearchByType map(String suggestion) {
        for (SearchByType type : SearchByType.values()) {
            if (type.name().equalsIgnoreCase(suggestion)) {
                return type;
            }
        }
        return EVERYWHERE;
    }

    public static SearchByType findByMask(String query) {
        for (SearchByType type : SearchByType.values()) {
            if (type != EVERYWHERE) {
                String prefix = type.name() + QUERY_SEPARATOR;
                if (query.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT))) {
                    return type;
                }
            }
        }
        return EVERYWHERE;
    }

    public static String getQueryWithoutMask(String query) {
        SearchByType type = findByMask(query);
        if (type == EVERYWHERE) {
            return query;
        }
        String prefix = type.name() + QUERY_SEPARATOR;
        return query.substring(prefix.length());
    }

    public static String addMaskToQuery(String query, SearchByType type) {
        if (type == EVERYWHERE) {
            return query;
        }
        return type.name().toLowerCase(Locale.ROOT) + QUERY_SEPARATOR + query;
    }
}
