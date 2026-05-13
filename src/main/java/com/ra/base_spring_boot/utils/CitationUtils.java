package com.ra.base_spring_boot.utils;

import lombok.Builder;
import lombok.Data;

public class CitationUtils {

    @Data
    @Builder
    public static class CitationData {
        private String author;
        private String publicationYear;
        private String title;
        private String source;
        private String pages;
        private String url;
    }

    public static String format(CitationData data, String style) {
        if ("MLA".equalsIgnoreCase(style)) {
            return String.format("%s. \"%s.\" %s, %s, %s.",
                    defaultIfEmpty(data.getAuthor(), "Unknown Author"),
                    defaultIfEmpty(data.getTitle(), "Unknown Title"),
                    defaultIfEmpty(data.getSource(), "Unknown Source"),
                    defaultIfEmpty(data.getPublicationYear(), "n.d."),
                    defaultIfEmpty(data.getPages(), ""));
        } else {
            return String.format("%s (%s). %s. %s, %s.",
                    defaultIfEmpty(data.getAuthor(), "Unknown Author"),
                    defaultIfEmpty(data.getPublicationYear(), "n.d."),
                    defaultIfEmpty(data.getTitle(), "Unknown Title"),
                    defaultIfEmpty(data.getSource(), "Unknown Source"),
                    defaultIfEmpty(data.getPages(), ""));
        }
    }

    private static String defaultIfEmpty(String value, String defaultValue) {
        return (value == null || value.trim().isEmpty()) ? defaultValue : value;
    }
}
