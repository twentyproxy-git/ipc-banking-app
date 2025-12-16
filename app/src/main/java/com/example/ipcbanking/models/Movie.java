package com.example.ipcbanking.models;

public class Movie {
    private String movieId;
    private String title;
    private String posterUrl;
    private String genre;
    private String rating;
    private String duration;
    private String releaseDate;
    private String language;
    private String synopsis;

    public Movie() {}

    public Movie(String title, int posterResId) {
        this.title = title;
        // This constructor is likely outdated, but we'll keep it for now
    }

    public String getMovieId() {
        return movieId;
    }

    public String getTitle() {
        return title;
    }

    public String getPosterUrl() {
        return posterUrl;
    }

    public String getGenre() {
        return genre;
    }

    public String getRating() {
        return rating;
    }

    public String getDuration() {
        return duration;
    }

    public String getReleaseDate() {
        return releaseDate;
    }

    public String getLanguage() {
        return language;
    }

    public String getSynopsis() {
        return synopsis;
    }
}
