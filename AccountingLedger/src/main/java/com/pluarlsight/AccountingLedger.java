package com.pluarlsight;

import java.io.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

public class AccountingLedger {

    private static final String FILE_NAME = "transactions.csv";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_TIME; // HH:mm:ss. Optional: .truncatedTo(ChronoUnit.SECONDS);

    private static ArrayList<Transaction> allTransactions = new ArrayList<>();
    private static Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        loadTransactions(); // Load existing transactions at start
        showHomeScreen();
        scanner.close(); // Close scanner when exiting
    }

    // --- File Handling ---

    public static void loadTransactions() {
        allTransactions.clear(); // Clear current list before loading
        File file = new File(FILE_NAME);

        // Create file with header if it doesn't exist
        if (!file.exists()) {
            try {
                file.createNewFile();
            
                System.out.println("transactions.csv created.");
            } catch (IOException e) {
                System.err.println("Error creating transactions file: " + e.getMessage());
                return; // Cant proceed without file
            }
        }

        // Read transactions
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // check to avoid empty lines or a potential header
                if (!line.trim().isEmpty() && line.contains("|")) {
                    Transaction transaction = Transaction.fromCsvString(line);
                    if (transaction != null) {
                        allTransactions.add(transaction);
                    }
                }
            }
            // Sort transactions newest first after loading
            Collections.sort(allTransactions);
        } catch (IOException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }

    public static void saveTransaction(Transaction transaction) {
        // Append new transaction to file
        try (PrintWriter writer = new PrintWriter(new FileWriter(FILE_NAME, true))) { // true for append
            writer.println(transaction.toCsvString());
        } catch (IOException e) {
            System.err.println("Error saving transaction: " + e.getMessage());
        }
        // Add to in-memory list and re-sort
        allTransactions.add(transaction);
        Collections.sort(allTransactions);
    }


    // --- Screens ---

    public static void showHomeScreen() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Home ---");
            System.out.println("D) Add Deposit");
            System.out.println("P) Make Payment (Debit)");
            System.out.println("L) Ledger");
            System.out.println("X) Exit");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim().toUpperCase();

            switch (choice) {
                case "D":
                    addTransaction(true); // true for deposit
                    break;
                case "P":
                    addTransaction(false); // false for payment
                    break;
                case "L":
                    showLedgerScreen();
                    break;
                case "X":
                    running = false;
                    System.out.println("Exiting application. Goodbye!");
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    public static void showLedgerScreen() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Ledger ---");
            System.out.println("A) All");
            System.out.println("D) Deposits");
            System.out.println("P) Payments");
            System.out.println("R) Reports");
            System.out.println("H) Home");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim().toUpperCase();

            switch (choice) {
                case "A":
                    displayTransactions(allTransactions, "All Transactions");
                    break;
                case "D":
                    displayTransactions(
                            allTransactions.stream()
                                    .filter(t -> t.getAmount() > 0)
                                    .collect(Collectors.toList()),
                            "Deposits"
                    );
                    break;
                case "P":
                    displayTransactions(
                            allTransactions.stream()
                                    .filter(t -> t.getAmount() < 0)
                                    .collect(Collectors.toList()),
                            "Payments"
                    );
                    break;
                case "R":
                    showReportsScreen();
                    break; // Reports screen will loop until back is chosen
                case "H":
                    running = false; // Go back to Home screen loop
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }

    public static void showReportsScreen() {
        boolean running = true;
        while (running) {
            System.out.println("\n--- Reports ---");
            System.out.println("1) Month To Date");
            System.out.println("2) Previous Month");
            System.out.println("3) Year To Date");
            System.out.println("4) Previous Year");
            System.out.println("5) Search by Vendor");
            System.out.println("0) Back");
            System.out.print("Choose an option: ");
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1": runMonthToDateReport(); break;
                case "2": runPreviousMonthReport(); break;
                case "3": runYearToDateReport(); break;
                case "4": runPreviousYearReport(); break;
                case "5": runVendorSearchReport(); break;
                case "0":
                    running = false; // Go back to Ledger screen
                    break;
                default:
                    System.out.println("Invalid option. Please try again.");
            }
        }
    }


    // --- Transaction Input & Display ---

    public static void addTransaction(boolean isDeposit) {
        System.out.println("\nEnter details for the " + (isDeposit ? "Deposit" : "Payment") + ":");
        LocalDate date = LocalDate.now(); // Automatically use today's date
        LocalTime time = LocalTime.now(); // Automatically use current time

        System.out.print("Description: ");
        String description = scanner.nextLine().trim();
        System.out.print("Vendor: ");
        String vendor = scanner.nextLine().trim();
        System.out.print("Amount: ");
        double amount = 0;
        try {
            amount = Double.parseDouble(scanner.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount. Transaction cancelled.");
            return;
        }

        // Ensure payment amounts are negative
        if (!isDeposit && amount > 0) {
            amount = -amount;
        }
        // Ensure deposit amounts are positive
        if (isDeposit && amount < 0) {
            amount = Math.abs(amount); // Make it positive
        }

        Transaction newTransaction = new Transaction(date, time, description, vendor, amount);
        saveTransaction(newTransaction);
        System.out.println( (isDeposit ? "Deposit" : "Payment") + " added successfully.");
    }

    public static void displayTransactions(List<Transaction> transactions, String title) {
        System.out.println("\n--- " + title + " ---");
        if (transactions.isEmpty()) {
            System.out.println("No transactions found.");
        } else {
            // Header
            System.out.printf("%-10s | %-8s | %-30s | %-20s | %10s%n",
                    "Date", "Time", "Description", "Vendor", "Amount");
            System.out.println("-".repeat(90)); // Separator line

            // Data (List should already be sorted newest first)
            for (Transaction t : transactions) {
                System.out.println(t); // Uses the toString() method
            }
        }
        System.out.println("--- End of " + title + " ---");
    }


    // --- Report Logic ---

    private static void runMonthToDateReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfMonth = today.withDayOfMonth(1);
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> !t.getDate().isBefore(startOfMonth) && !t.getDate().isAfter(today))
                .collect(Collectors.toList()); // Already sorted newest first
        displayTransactions(filtered, "Month To Date Report");
    }

    private static void runPreviousMonthReport() {
        LocalDate today = LocalDate.now();
        YearMonth previousMonth = YearMonth.from(today).minusMonths(1);
        LocalDate start = previousMonth.atDay(1);
        LocalDate end = previousMonth.atEndOfMonth();

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                .collect(Collectors.toList());
        displayTransactions(filtered, "Previous Month Report");
    }

    private static void runYearToDateReport() {
        LocalDate today = LocalDate.now();
        LocalDate startOfYear = today.withDayOfYear(1);
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> !t.getDate().isBefore(startOfYear) && !t.getDate().isAfter(today))
                .collect(Collectors.toList());
        displayTransactions(filtered, "Year To Date Report");
    }

    private static void runPreviousYearReport() {
        int previousYear = LocalDate.now().getYear() - 1;
        LocalDate start = LocalDate.of(previousYear, Month.JANUARY, 1);
        LocalDate end = LocalDate.of(previousYear, Month.DECEMBER, 31);

        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> !t.getDate().isBefore(start) && !t.getDate().isAfter(end))
                .collect(Collectors.toList());
        displayTransactions(filtered, "Previous Year Report (" + previousYear + ")");
    }

    private static void runVendorSearchReport() {
        System.out.print("Enter Vendor name to search for: ");
        String searchVendor = scanner.nextLine().trim();
        if (searchVendor.isEmpty()) {
            System.out.println("Vendor name cannot be empty.");
            return;
        }

        // Case-insensitive search
        String lowerCaseVendor = searchVendor.toLowerCase();
        List<Transaction> filtered = allTransactions.stream()
                .filter(t -> t.getVendor().toLowerCase().contains(lowerCaseVendor))
                .collect(Collectors.toList());

        displayTransactions(filtered, "Vendor Search Results for '" + searchVendor + "'");
    }
}
