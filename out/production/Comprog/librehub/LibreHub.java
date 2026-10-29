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
