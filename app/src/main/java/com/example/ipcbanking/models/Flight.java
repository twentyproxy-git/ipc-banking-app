package com.example.ipcbanking.models;

import java.io.Serializable;
import java.util.Map;

public class Flight implements Serializable {
    private String flightId;
    private String airlineId;
    private String airlineName; // This will be populated from the airline document
    private String airlineLogoUrl; // This will be populated from the airline document
    private String flightNumber;
    private String departureAirport;
    private String arrivalAirport;
    private String departureTime;
    private String arrivalTime;
    private int durationMinutes;
    private String promoBadge;
    private boolean isDirectFlight;
    private Map<String, FlightClass> classes;

    public Flight() {}

    // Getters and Setters
    public String getFlightId() { return flightId; }
    public void setFlightId(String flightId) { this.flightId = flightId; }

    public String getAirlineId() { return airlineId; }
    public void setAirlineId(String airlineId) { this.airlineId = airlineId; }

    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }

    public String getAirlineLogoUrl() { return airlineLogoUrl; }
    public void setAirlineLogoUrl(String airlineLogoUrl) { this.airlineLogoUrl = airlineLogoUrl; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getDepartureAirport() { return departureAirport; }
    public void setDepartureAirport(String departureAirport) { this.departureAirport = departureAirport; }

    public String getArrivalAirport() { return arrivalAirport; }
    public void setArrivalAirport(String arrivalAirport) { this.arrivalAirport = arrivalAirport; }

    public String getDepartureTime() { return departureTime; }
    public void setDepartureTime(String departureTime) { this.departureTime = departureTime; }

    public String getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(String arrivalTime) { this.arrivalTime = arrivalTime; }

    public int getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }

    public String getPromoBadge() { return promoBadge; }
    public void setPromoBadge(String promoBadge) { this.promoBadge = promoBadge; }

    public boolean isDirectFlight() { return isDirectFlight; }
    public void setDirectFlight(boolean directFlight) { isDirectFlight = directFlight; }

    public Map<String, FlightClass> getClasses() { return classes; }
    public void setClasses(Map<String, FlightClass> classes) { this.classes = classes; }

    public static class FlightClass implements Serializable {
        private String name;
        private double price;
        private int totalSeats;
        private int availableSeats;

        public FlightClass() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public double getPrice() { return price; }
        public void setPrice(double price) { this.price = price; }

        public int getTotalSeats() { return totalSeats; }
        public void setTotalSeats(int totalSeats) { this.totalSeats = totalSeats; }

        public int getAvailableSeats() { return availableSeats; }
        public void setAvailableSeats(int availableSeats) { this.availableSeats = availableSeats; }
    }
}
