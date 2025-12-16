package com.example.ipcbanking.models;

public class Showtime {
    private String showtimeId;
    private String movieId;
    private String cinemaId;
    private String cinemaName;
    private String dateTime;
    private String format;
    private String availability;
    private String auditorium;

    // Default constructor for Firestore
    public Showtime() {}

    // Getters
    public String getShowtimeId() {
        return showtimeId;
    }

    public String getMovieId() {
        return movieId;
    }

    public String getCinemaId() {
        return cinemaId;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getFormat() {
        return format;
    }

    public String getAvailability() {
        return availability;
    }

    public String getAuditorium() {
        return auditorium;
    }

    // Method to extract time from dateTime string
    public String getTime() {
        if (dateTime != null && dateTime.contains("T")) {
            String timePart = dateTime.substring(dateTime.indexOf("T") + 1);
            if (timePart.length() > 5) {
                return timePart.substring(0, 5); // Return HH:mm
            }
            return timePart;
        }
        return ""; // Return empty if format is incorrect
    }
}
