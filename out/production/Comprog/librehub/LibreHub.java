/*
 * LibreHub | Library Book System
 * Developed by: Group 1
 * Pacada
 * Fiel
 * Bolo
 * Pantaleon
 * Corpus
 */


package librehub;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
public class LibreHub {

    // File where the book database is stored (created in the folder the program runs from)
    private static final String DATA_FILE = "books.txt";

    static class Book {
        final String title;
        final String category;
        final int year;
        String borrower;

        Book(String title, String category, int year) {
            this.title = title;
            this.category = category;
            this.year = year;
        }
    }

    public static void main(String[] args) {
        Scanner input = new Scanner(System.in);

        // BOOK DATABASE
        // Author: Fiel & Bolo

        createDataFileIfMissing();

        int choice = 0;

        do {
            System.out.println("\nLIBRARY BOOK SYSTEM");
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
                case 1 -> viewAllBooks();
                case 2 -> searchBooks(input);
                case 3 -> System.out.println("Total Books: " + countBooks());
                case 4 -> circulationMenu(input);
                case 5 -> System.out.println("Thank you for using the system!");
                default -> System.out.println("Invalid choice!");
            }

        } while (choice != 5);

        input.close();
    }

    // FILE HANDLING
    // Author: Fiel, Pacada, & Bolo

    /*
     * The book database lives just in the DATA_FILE. Each line is just one book:
     *     title|category|year|borrower
     * The borrower field is empty when the book is available
     * Every operation reads or rewrites the file directly
     */

    // Creates DATA_FILE with the starting catalog if it does not exist yet.


    private static void createDataFileIfMissing() {
        File file = new File(DATA_FILE);
        if (file.exists()) {
            return;
        }

        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            writer.println("Harry Potter|Fantasy|1997|");
            writer.println("The Hobbit|Fantasy|1937|");
            writer.println("Naruto|Fiction|1999|");
            writer.println("The Great Gatsby|Classic|1925|");
            writer.println("To Kill a Mockingbird|Classic|1960|");
            writer.println("The Hunger Games|Adventure|2008|");
            writer.println("Atomic Habits|Self-Help|2018|");
            writer.println("Death Note|Fiction|2003|");
            writer.println("The Little Prince|Fantasy|1943|");
            writer.println("Percy Jackson|Adventure|2005|");
        } catch (IOException e) {
            showFileError(e);
        }
    }

    // Turns one line of the file into a Book, or returns null if the line is not valid.


    private static Book parseBook(String line) {
        if (line.isBlank()) {
            return null;
        }

        String[] parts = line.split("\\|", -1);
        if (parts.length < 3) {
            return null;
        }

        try {
            Book book = new Book(parts[0], parts[1], Integer.parseInt(parts[2]));
            if (parts.length > 3 && !parts[3].isEmpty()) {
                book.borrower = parts[3];
            }
            return book;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // Turns a Book back into one line of the file.

    private static String toLine(Book book) {
        return book.title + "|" + book.category + "|" + book.year + "|"
                + (book.borrower == null ? "" : book.borrower);
    }

    // Counts the valid books in the file.
    private static int countBooks() {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (parseBook(line) != null) {
                    count++;
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
        return count;
    }

    // Reads the book with the given 1-based number from the file, or returns null.


    private static Book findBookByNumber(int number) {
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            int current = 0;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null) {
                    current++;
                    if (current == number) {
                        return book;
                    }
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
        return null;
    }


    /*
     * Sets the borrower of the book with the given 1-based number and rewrites the file.
     * Pass null as the borrower to mark the book as returned.
     * The new contents go to a temporary file first, then replace DATA_FILE.
     */


    private static void updateBorrower(int number, String borrower) {
        File data = new File(DATA_FILE);
        File temp = new File(DATA_FILE + ".tmp");

        try (BufferedReader reader = new BufferedReader(new FileReader(data));
             PrintWriter writer = new PrintWriter(new FileWriter(temp))) {
            String line;
            int current = 0;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book == null) {
                    continue;
                }
                current++;
                if (current == number) {
                    book.borrower = borrower;
                }
                writer.println(toLine(book));
            }
        } catch (IOException e) {
            showFileError(e);
            return;
        }

        try {
            Files.move(temp.toPath(), data.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            showFileError(e);
        }
    }

    private static void showFileError(IOException e) {
        System.out.println("File error (" + DATA_FILE + "): " + e.getMessage());
    }

    // VIEW
    // Author: Pacada

    private static void viewAllBooks() {
        System.out.println("\n===== BOOK CATALOG =====");
        printHeader();

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null) {
                    number++;
                    printRow(number, book);
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
    }

    // SEARCH
    // Author: Pacada

    private static void searchBooks(Scanner input) {
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

        int total = countMatches(option, keyword);

        if (total == 0) {
            System.out.println("No books found matching \"" + keyword + "\".");
            return;
        }

        System.out.println("\nFound " + total + " book(s) matching \"" + keyword + "\":");
        printHeader();

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null && matches(book, option, keyword)) {
                    number++;
                    printRow(number, book);
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
    }

    // Counts how many books in the file match the search.

    private static int countMatches(String option, String keyword) {
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null && matches(book, option, keyword)) {
                    count++;
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
        return count;
    }

    /*
     * Checks one book against the search.
     * Text searches ignore case and surrounding spaces; year searches need an exact match.
     */


    private static boolean matches(Book book, String option, String keyword) {
        String needle = keyword.toLowerCase(Locale.ROOT);

        return switch (option) {
            case "1" -> book.title.toLowerCase(Locale.ROOT).contains(needle);
            case "2" -> book.category.toLowerCase(Locale.ROOT).contains(needle);
            case "3" -> String.valueOf(book.year).equals(keyword);
            default -> false;
        };
    }

    // CIRCULATION (borrow / return)
    // Author: Bolo

    private static void circulationMenu(Scanner input) {
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
                case 1 -> borrowBook(input);
                case 2 -> returnBook(input);
                case 3 -> viewBorrowingRecords();
                case 4 -> System.out.println("Returning to main menu...");
                default -> System.out.println("Invalid choice!");
            }

        } while (choice != 4);
    }

    private static void borrowBook(Scanner input) {
        printBookStatus();

        System.out.print("Enter Book Number: ");
        int number = readBookNumber(input, countBooks());
        if (number < 0) {
            System.out.println("Invalid book number!");
            return;
        }

        Book book = findBookByNumber(number);
        if (book == null) {
            System.out.println("Invalid book number!");
            return;
        }

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

        updateBorrower(number, name);
        System.out.println("Borrow successful!");
    }

    private static void returnBook(Scanner input) {
        printBookStatus();

        System.out.print("Enter Book Number: ");
        int number = readBookNumber(input, countBooks());
        if (number < 0) {
            System.out.println("Invalid book number!");
            return;
        }

        Book book = findBookByNumber(number);
        if (book == null) {
            System.out.println("Invalid book number!");
            return;
        }

        if (book.borrower == null) {
            System.out.println("Book is not borrowed!");
            return;
        }

        updateBorrower(number, null);
        System.out.println("Return successful!");
    }

    private static void viewBorrowingRecords() {
        System.out.println("\n=== BORROWING RECORDS ===");
        boolean any = false;

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null && book.borrower != null) {
                    System.out.println(book.title + " - " + book.borrower);
                    any = true;
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }

        if (!any) {
            System.out.println("No books are currently borrowed.");
        }
    }

    // Reads a 1-based book number. Returns the number, or -1 if it is not valid.
    private static int readBookNumber(Scanner input, int count) {
        if (!input.hasNextInt()) {
            input.nextLine();
            return -1;
        }
        int number = input.nextInt();
        input.nextLine();
        return (number >= 1 && number <= count) ? number : -1;
    }

    private static void printBookStatus() {
        System.out.println("\nBOOK DATA");

        try (BufferedReader reader = new BufferedReader(new FileReader(DATA_FILE))) {
            String line;
            int number = 0;
            while ((line = reader.readLine()) != null) {
                Book book = parseBook(line);
                if (book != null) {
                    number++;
                    System.out.println(number + ". " + book.title + " - "
                            + (book.borrower == null ? "Available" : "Borrowed"));
                }
            }
        } catch (IOException e) {
            showFileError(e);
        }
    }

    // DISPLAY

    private static void printHeader() {
        System.out.printf("%-5s %-25s %-15s %-10s%n", "No.", "Title", "Category", "Year");
    }

    private static void printRow(int number, Book book) {
        System.out.printf("%-5d %-25s %-15s %-10d%n",
                number, book.title, book.category, book.year);
    }
}
