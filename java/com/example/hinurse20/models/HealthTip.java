package com.example.hinurse20.models;

public class HealthTip {
    public final String title;
    public final String description;
    public final Integer imageRes;
    public final String link;

    public HealthTip(String title, String description, Integer imageRes, String link) {
        this.title = title;
        this.description = description;
        this.imageRes = imageRes;
        this.link = link;
    }
}
