import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class LibreHub {

    // 1. CONSTANTS AND DATA ARRAYS

    static int MAX_BOOKS = 500;
    static int MAX_USERS = 200;
    static int MAX_LOANS = 2000;
    static int LOAN_DAYS = 14;               // days a book may be kept; admin-configurable
    static int MAX_ACTIVE_LOANS = 3;         // books one member may hold at once
    static int MIN_YEAR = 1450;        // earliest accepted publication year
    static int DUE_SOON_DAYS = 3;            // "due soon" window on the dashboard
    static int MIN_PASSWORD = 6;             // minimum password length for new/changed passwords

    static String[] CATEGORIES = {"Fiction", "Non-Fiction", "Academic", "Reference", "Periodical"};

    // Role codes stored in the users file.
    static String ADMIN = "ADMIN";
    static String LIBRARIAN = "LIBRARIAN";
    static String MEMBER = "MEMBER";

    static String BOOKS_FILE = "books.txt";
    static String USERS_FILE = "users.txt";
    static String LOANS_FILE = "loans.txt";
    static String SETTINGS_FILE = "settings.txt";
    static String ACTIVITY_LOG = "activity.log";

    static String dataFolder = "data";
    static Scanner input = new Scanner(System.in);
    static boolean running = true;
    static String sessionUser = null;         // username of the logged-in account, or null

    static int today = 0;                     // the library's current date, as a day number

    // Books (parallel arrays: index i describes one book)
    static int bookCount = 0;
    static int[] bookId = new int[MAX_BOOKS];
    static String[] bookTitle = new String[MAX_BOOKS];
    static String[] bookCategory = new String[MAX_BOOKS];
    static String[] bookGenre = new String[MAX_BOOKS];
    static int[] bookYear = new int[MAX_BOOKS];
    static boolean[] bookOnLoan = new boolean[MAX_BOOKS];

    // Users
    static int userCount = 0;
    static String[] userName = new String[MAX_USERS];
    static String[] userFullName = new String[MAX_USERS];
    static String[] userPassword = new String[MAX_USERS];
    static String[] userRole = new String[MAX_USERS];

    // Loans. loanReturned is -1 while the book is still out.
    static int loanCount = 0;
    static int[] loanId = new int[MAX_LOANS];
    static int[] loanBookId = new int[MAX_LOANS];
    static String[] loanUser = new String[MAX_LOANS];
    static int[] loanBorrowed = new int[MAX_LOANS];
    static int[] loanDue = new int[MAX_LOANS];
    static int[] loanReturned = new int[MAX_LOANS];

    // 2. INPUT HELPERS

    // Reads one whole line. When input ends, stops the program loops and returns "0".
    static String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!input.hasNextLine()) {
            running = false;
            return "0";
        }
        return clean(input.nextLine());
    }

    static int askInt(String prompt) {
        String text = ask(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + text + "' is not a whole number.");
        }
    }

    // Pauses after a menu action until the user submits a blank line.
    static void pressEnterToContinue() {
        if (!running) {
            return;
        }
        while (running) {
            System.out.print("\nPress ENTER to continue...");
            if (!input.hasNextLine()) {
                running = false;
                return;
            }
            String response = input.nextLine();
            if (isBlank(response)) {
                return;
            }
            System.out.println("Please press ENTER without typing anything.");
        }
    }

    // Blank input means "keep the current value", so it becomes null.
    static String orNull(String text) {
        if (text.length() == 0) {
            return null;
        }
        return text;
    }
// 3. TEXT HELPERS AND CHECKS
    // Removes spaces from both ends, using charAt (String.trim is not in the slides).
    static String clean(String text) {
        int start = 0;
        int end = text.length();
        while (start < end && text.charAt(start) == ' ') {
            start++;
        }
        while (end > start && text.charAt(end - 1) == ' ') {
            end--;
        }
        String result = "";
        for (int i = start; i < end; i++) {
            result = result + text.charAt(i);
        }
        return result;
    }

    static boolean isBlank(String text) {
        return clean(text).length() == 0;
    }

    // True if the text contains a slash or a backslash (checked with charAt, not indexOf).
    static boolean hasSeparator(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '/'  text.charAt(i) == '\') {
                return true;
            }
        }
        return false;
    }


    //The first 'count' characters of the text.

    static String firstChars(String text, int count) {
        String result = "";
        for (int i = 0; i < count; i++) {
            result = result + text.charAt(i);
        }
        return result;
    }

    // Case-insensitive comparison, built from toLowerCase() and equals().
    static boolean sameName(String a, String b) {
        return a.toLowerCase().equals(b.toLowerCase());
    }

    static String pad(String text, int width) {
        String result = text;
        while (result.length() < width) {
            result = result + " ";
        }
        return result;
    }

    static String cut(String text, int max) {
        if (text.length() <= max) {
            return text;
        }
        return firstChars(text, max - 3) + "...";
    }

    static String dashes(int count) {
        String line = "";
        for (int i = 0; i < count; i++) {
            line = line + "-";
        }
        return line;
    }

    // Text must not be empty and must not contain ';', which separates fields in the files.

    static void checkText(String value, String field) {
        if (value == null  value.length() == 0) {
            throw new IllegalArgumentException(field + " cannot be empty.");
        }
        if (value.contains(";")) {
            throw new IllegalArgumentException(field + " cannot contain ';'.");
        }
    }

    static void checkUsername(String name) {
        if (name.length() < 3  name.length() > 20) {
            throw new IllegalArgumentException("Username must be 3-20 characters.");
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean allowed = (c >= 'a' && c <= 'z')  (c >= 'A' && c <= 'Z')
                     (c >= '0' && c <= '9')  c == '.'  c == '';
            if (!allowed) {
                throw new IllegalArgumentException("Username may only use letters, digits, '.' and ''.");
            }
        }
    }

    static void checkPassword(String password) {
        if (password == null  password.length() < MIN_PASSWORD) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD + " characters.");
        }
    }
// 4. DATE HELPERS
    static int toDayNumber(int year, int month, int day) {
        int a = (14 - month) / 12;
        int y = year + 4800 - a;
        int m = month + 12 * a - 3;
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045;
    }

    // Converts a day number back to "YYYY-MM-DD".

    static String fromDayNumber(int dayNumber) {
        int a = dayNumber + 32044;
        int b = (4 * a + 3) / 146097;
        int c = a - (146097 * b) / 4;
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        int day = e - (153 * m + 2) / 5 + 1;
        int month = m + 3 - 12 * (m / 10);
        int year = 100 * b + d - 4800 + m / 10;
        return year + "-" + twoDigits(month) + "-" + twoDigits(day);
    }

    static String twoDigits(int n) {
        if (n < 10) {
            return "0" + n;
        }
        return "" + n;
    }

    static int yearOf(int dayNumber) {
        int a = dayNumber + 32044;
        int b = (4 * a + 3) / 146097;
        int c = a - (146097 * b) / 4;
        int d = (4 * c + 3) / 1461;
        int e = c - (1461 * d) / 4;
        int m = (5 * e + 2) / 153;
        return 100 * b + d - 4800 + m / 10;
    }

    static int daysInMonth(int year, int month) {
        int[] days = {31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
        if (month == 2 && ((year % 4 == 0 && year % 100 != 0)  year % 400 == 0)) {
            return 29;
        }
        return days[month - 1];
    }

    // Parses "YYYY-MM-DD" and checks that the date exists.

    static int parseDate(String text) {
        String[] parts = text.split("-");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Date must look like YYYY-MM-DD.");
        }
        int year;
        int month;
        int day;
        try {
            year = Integer.parseInt(parts[0]);
            month = Integer.parseInt(parts[1]);
            day = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Date must look like YYYY-MM-DD.");
        }
        if (year < MIN_YEAR  month < 1  month > 12  day < 1 || day > daysInMonth(year, month)) {
            throw new IllegalArgumentException("'" + text + "' is not a valid date.");
        }
        return toDayNumber(year, month, day);
    }

    // Days from borrowing to return, or to today while the book is still out.

    static int daysOut(int k) {
        int end = loanReturned[k] == -1 ? today : loanReturned[k];
        return end - loanBorrowed[k];
    }

    // Days past the due date (0 if not late).

    static int overdueDays(int k) {
        int end = loanReturned[k] == -1 ? today : loanReturned[k];
        if (end > loanDue[k]) {
            return end - loanDue[k];
        }
        return 0;
    }

    static boolean isOverdue(int k) {
        return loanReturned[k] == -1 && overdueDays(k) > 0;
    }
