/*
 * LibreHub - Library Book System
 * Checked by: Pacada
 */
package librehub;

import java.util.*;

public class LibreHub {

    static class Book {
        final String title;
        final String category;
        final int year;
        String borrower; // null means the book is available - Pacada

        Book(String title, String category, int year) {
            this.title = title;
            this.category = category;
            this.year = year;
        }
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // BOOK DATABASE
        // Fiel & Bolo
        List<Book> books = new ArrayList<>();
        books.add(new Book("Harry Potter", "Fantasy", 1997));
        books.add(new Book("The Hobbit", "Fantasy", 1937));
        books.add(new Book("Naruto", "Fiction", 1999));
        books.add(new Book("The Great Gatsby", "Classic", 1925));
        books.add(new Book("To Kill a Mockingbird", "Classic", 1960));
        books.add(new Book("The Hunger Games", "Adventure", 2008));
        books.add(new Book("Atomic Habits", "Self-Help", 2018));
        books.add(new Book("Death Note", "Fiction", 2003));
        books.add(new Book("The Little Prince", "Fantasy", 1943));
        books.add(new Book("Percy Jackson", "Adventure", 2005));

        int choice = 0;

        do {
            System.out.println("\n===== LIBRARY BOOK SYSTEM =====");
            System.out.println("1. View All Books");
            System.out.println("2. Search Book");
            System.out.println("3. View Total Books");
            System.out.println("4. Circulation (Borrow / Return)");
            System.out.println("5. Exit");
            System.out.print("Enter choice: ");

            if (!input.hasNextInt()) {
                System.out.println("Invalid input!");
                input.nextLine();
                continue;
            }

            choice = input.nextInt();
            input.nextLine();

            switch (choice) {
                case 1 -> viewAllBooks(books);
                case 2 -> searchBooks(input, books);
                case 3 -> System.out.println("Total Books: " + books.size());
                case 4 -> circulationMenu(input, books);
                case 5 -> System.out.println("Thank you for using the system!");
                default -> System.out.println("Invalid choice!");
            }

        } while (choice != 5);

        input.close();
    }


    private static void viewAllBooks(List<Book> books) {
        System.out.println("\n===== BOOK CATALOG =====");
        printTable(books);
    }
    
    // SEARCH
    // Pacada

    private static void searchBooks(Scanner input, List<Book> books) {
        System.out.println("\n----- SEARCH BOOKS -----");
        System.out.println("1. By Title (partial match)");
        System.out.println("2. By Category (partial match)");
        System.out.println("3. By Year (exact match)");
        System.out.print("Search by: ");
        String option = input.nextLine().trim();

        if (!option.equals("1") && !option.equals("2") && !option.equals("3")) {
            System.out.println("Invalid search option!");
            return;
        }

        System.out.print("Enter keyword: ");
        String keyword = input.nextLine().trim();

        if (keyword.isEmpty()) {
            System.out.println("Search keyword cannot be empty!");
            return;
        }

        List<Book> results = findBooks(books, option, keyword);

        if (results.isEmpty()) {
            System.out.println("No books found matching \"" + keyword + "\".");
        } else {
            System.out.println("\nFound " + results.size()
                    + " book(s) matching \"" + keyword + "\":");
            printTable(results);
        }
    }

    static List<Book> findBooks(List<Book> books, String option, String keyword) {
        List<Book> results = new ArrayList<>();
        String needle = keyword.toLowerCase(Locale.ROOT);

        for (Book book : books) {
            boolean match = switch (option) {
                case "1" -> book.title.toLowerCase(Locale.ROOT).contains(needle);
                case "2" -> book.category.toLowerCase(Locale.ROOT).contains(needle);
                case "3" -> String.valueOf(book.year).equals(keyword);
                default -> false;
            };

            if (match) {
                results.add(book);
            }
        }

        
        return results;
    }
    private static void circulationMenu(Scanner input, List<Book> books) {
        int choice = 0;

        do {
            System.out.println("\n=== DIGILIB: CIRCULATION ===");
            System.out.println("[1] Borrow Book");
            System.out.println("[2] Return Book");
            System.out.println("[3] View Records");
            System.out.println("[4] Back to Main Menu");
            System.out.print("Choice: ");

            if (!input.hasNextInt()) {
                System.out.println("Invalid input!");
                input.nextLine();
                continue;
            }

            choice = input.nextInt();
            input.nextLine();

            switch (choice) {
                case 1 -> borrowBook(input, books);
                case 2 -> returnBook(input, books);
                case 3 -> viewBorrowingRecords(books);
                case 4 -> System.out.println("Returning to main menu...");
                default -> System.out.println("Invalid choice!");
            }

        } while (choice != 4);
    }

    private static void borrowBook(Scanner input, List<Book> books) {
        printBookStatus(books);

        System.out.print("Enter Book Number: ");
        int index = readBookNumber(input, books.size());
        if (index < 0) {
            System.out.println("Invalid book number!");
            return;
        }

        Book book = books.get(index);
        if (book.borrower != null) {
            System.out.println("Book already borrowed!");
            return;
        }

        System.out.print("Student Name: ");
        String name = input.nextLine().trim();

        if (name.isEmpty()) {
            System.out.println("Name cannot be empty!");
            return;
        }

        book.borrower = name;
        System.out.println("Borrow successful!");
    }

    private static void returnBook(Scanner input, List<Book> books) {
        printBookStatus(books);

        System.out.print("Enter Book Number: ");
        int index = readBookNumber(input, books.size());
        if (index < 0) {
            System.out.println("Invalid book number!");
            return;
        }

        Book book = books.get(index);
        if (book.borrower == null) {
            System.out.println("Book is not borrowed!");
            return;
        }

        book.borrower = null;
        System.out.println("Return successful!");
    }

    private static void viewBorrowingRecords(List<Book> books) {
        System.out.println("\n=== BORROWING RECORDS ===");
        boolean any = false;

        for (Book book : books) {
            if (book.borrower != null) {
                System.out.println(book.title + " - " + book.borrower);
                any = true;
            }
        }

        if (!any) {
            System.out.println("No books are currently borrowed.");
        }
    }
