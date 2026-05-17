package com.fco.platform.common.util;

public class CitationUtils {

    public static class CitationData {
        private String author;
        private String publicationYear;
        private String title;
        private String source;
        private String pages;
        private String url;

        public String getAuthor() { return author; }
        public void setAuthor(String author) { this.author = author; }
        public String getPublicationYear() { return publicationYear; }
        public void setPublicationYear(String publicationYear) { this.publicationYear = publicationYear; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getPages() { return pages; }
        public void setPages(String pages) { this.pages = pages; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
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
