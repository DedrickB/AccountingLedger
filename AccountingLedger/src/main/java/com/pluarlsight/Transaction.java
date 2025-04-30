package com.pluarlsight;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class Transaction implements Comparable<Transaction> {
    private LocalDate date;
    private LocalTime time;
    private String description;
    private String vendor;
    private double amount;

    // Formatter for consistency
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss

    public Transaction(LocalDate date, LocalTime time, String description, String vendor, double amount) {
        this.date = date;
        this.time = time;
        this.description = description;
        this.vendor = vendor;
        this.amount = amount;
    }

    // Getters
    public LocalDate getDate() { return date; }
    public LocalTime getTime() { return time; }
    public String getDescription() { return description; }
    public String getVendor() { return vendor; }
    public double getAmount() { return amount; }

    // Format for saving to CSV
    public String toCsvString() {
        return String.format("%s|%s|%s|%s|%.2f",
                date.format(DATE_FORMATTER),
                time.format(TIME_FORMATTER),
                description,
                vendor,
                amount);
    }

    // Format for display
    @Override
    public String toString() {
        return String.format("Date: %-10s | Time: %-8s | Desc: %-30s | Vendor: %-20s | Amount: %10.2f",
                date.format(DATE_FORMATTER),
                time.format(TIME_FORMATTER),
                description,
                vendor,
                amount);
    }

    // Compare transactions by date and time (newest first)
    @Override
    public int compareTo(Transaction other) {
        int dateCompare = other.date.compareTo(this.date); // Descending date
        if (dateCompare == 0) {
            return other.time.compareTo(this.time); // Descending time if dates are same
        }
        return dateCompare;
    }

    // Static method to parse from CSV line
    public static Transaction fromCsvString(String csvLine) {
        String[] parts = csvLine.split("\\|"); // Pipe needs escaping
        if (parts.length == 5) {
            try {
                LocalDate date = LocalDate.parse(parts[0], DATE_FORMATTER);
                LocalTime time = LocalTime.parse(parts[1], TIME_FORMATTER);
                String description = parts[2];
                String vendor = parts[3];
                double amount = Double.parseDouble(parts[4]);
                return new Transaction(date, time, description, vendor, amount);
            } catch (Exception e) {
                System.err.println("Error parsing line: " + csvLine + " - " + e.getMessage());
                return null; // Indicate failure
            }
        }
        System.err.println("Skipping malformed line: " + csvLine);
        return null; // Indicate failure
    }
}