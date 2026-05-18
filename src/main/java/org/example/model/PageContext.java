package org.example.model;

public record PageContext(String page, String id, String title) {
    public static PageContext home() { return new PageContext("home", null, null); }
}
