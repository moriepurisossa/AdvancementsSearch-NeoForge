package com.ayane.advancementssearch;

public enum HighlightType {

    WIDGET,
    OBTAINED_STATUS;

    public static HighlightType map(String suggestion) {
        for (HighlightType type : HighlightType.values()) {
            if (type.name().equalsIgnoreCase(suggestion)) {
                return type;
            }
        }
        return WIDGET;
    }
}
