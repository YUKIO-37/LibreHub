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
// 5. LOOKUPS

    static int findUser(String name) {
        for (int i = 0; i < userCount; i++) {
            if (sameName(userName[i], name)) {
                return i;
            }
        }
        return -1;
    }

    static int findBook(int id) {
        for (int i = 0; i < bookCount; i++) {
            if (bookId[i] == id) {
                return i;
            }
        }
        return -1;
    }

    static int findLoan(int id) {
        for (int k = 0; k < loanCount; k++) {
            if (loanId[k] == id) {
                return k;
            }
        }
        return -1;
    }

    static String roleOf(String username) {
        int i = findUser(username);
        if (i < 0) {
            return "";
        }
        return userRole[i];
    }

    static boolean isStaff(String role) {
        return ADMIN.equals(role) || LIBRARIAN.equals(role);
    }

    static String roleLabel(String role) {
        if (ADMIN.equals(role)) {
            return "Administrator";
        } else if (LIBRARIAN.equals(role)) {
            return "Staff";
        }
        return "Member";
    }

    static void requireStaff(String actor) {
        if (!isStaff(roleOf(actor))) {
            throw new IllegalArgumentException("This action is restricted to library staff.");
        }
    }

    static void requireAdmin(String actor) {
        if (!ADMIN.equals(roleOf(actor))) {
            throw new IllegalArgumentException("This action is restricted to administrators.");
        }
    }

    static int nextBookId() {
        int max = 0;
        for (int i = 0; i < bookCount; i++) {
            if (bookId[i] > max) {
                max = bookId[i];
            }
        }
        for (int k = 0; k < loanCount; k++) {   // loans keep old book IDs out of reuse
            if (loanBookId[k] > max) {
                max = loanBookId[k];
            }
        }
        return max + 1;
    }

    static int nextLoanId() {
        int max = 0;
        for (int k = 0; k < loanCount; k++) {
            if (loanId[k] > max) {
                max = loanId[k];
            }
        }
        return max + 1;
    }

    // 6. FILE STORAGE (load and save)

    static File fileNamed(String name) {
        return new File(dataFolder + "/" + name);
    }

    static String backupsPath() {
        return dataFolder + "/backups";
    }

    static void loadAll() throws IOException {
        File folder = new File(dataFolder);
        if (!folder.exists()) {
            folder.mkdir();
        }
        loadSettings();
        loadUsers();
        loadBooks();
        loadLoans();
    }

    static void loadSettings() throws IOException {
        File file = fileNamed(SETTINGS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line = reader.readLine();
            if (line != null && !isBlank(line)) {
                today = Integer.parseInt(clean(line));
            }
            // Older settings files contain only the date. Missing values keep their defaults.
            LOAN_DAYS = readOptionalSetting(reader, LOAN_DAYS, 1, 365, "loan period");
            MAX_ACTIVE_LOANS = readOptionalSetting(reader, MAX_ACTIVE_LOANS, 1, 20, "maximum active loans");
            DUE_SOON_DAYS = readOptionalSetting(reader, DUE_SOON_DAYS, 0, 60, "due-soon window");
            MIN_PASSWORD = readOptionalSetting(reader, MIN_PASSWORD, 4, 32, "minimum password length");
        } catch (NumberFormatException e) {
            throw new IOException("corrupt " + SETTINGS_FILE);
        }
    }

    static int readOptionalSetting(BufferedReader reader, int current, int minimum, int maximum, String name)
            throws IOException {
        String line = reader.readLine();
        if (line == null || isBlank(line)) {
            return current;
        }
        int value;
        try {
            value = Integer.parseInt(clean(line));
        } catch (NumberFormatException e) {
            throw new IOException("corrupt " + name + " in " + SETTINGS_FILE);
        }
        if (value < minimum || value > maximum) {
            throw new IOException("invalid " + name + " in " + SETTINGS_FILE);
        }
        return value;
    }

    static void loadUsers() throws IOException {
        File file = fileNamed(USERS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isBlank(line)) {
                    continue;
                }
                String[] f = line.split(";");
                if (f.length != 4) {
                    throw new IOException("corrupt record in " + USERS_FILE + ": " + line);
                }
                userName[userCount] = f[0];
                userFullName[userCount] = f[1];
                userPassword[userCount] = f[2];
                userRole[userCount] = f[3];
                userCount++;
            }
        }
    }

    static void loadBooks() throws IOException {
        File file = fileNamed(BOOKS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isBlank(line)) {
                    continue;
                }
                String[] f = line.split(";");
                if (f.length != 6) {
                    throw new IOException("corrupt record in " + BOOKS_FILE + ": " + line);
                }
                bookId[bookCount] = Integer.parseInt(f[0]);
                bookTitle[bookCount] = f[1];
                bookCategory[bookCount] = f[2];
                bookGenre[bookCount] = f[3];
                bookYear[bookCount] = Integer.parseInt(f[4]);
                bookOnLoan[bookCount] = f[5].equals("1");
                bookCount++;
            }
        } catch (NumberFormatException e) {
            throw new IOException("corrupt number in " + BOOKS_FILE);
        }
    }

    static void loadLoans() throws IOException {
        File file = fileNamed(LOANS_FILE);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (isBlank(line)) {
                    continue;
                }
                String[] f = line.split(";");
                if (f.length != 6) {
                    throw new IOException("corrupt record in " + LOANS_FILE + ": " + line);
                }
                loanId[loanCount] = Integer.parseInt(f[0]);
                loanBookId[loanCount] = Integer.parseInt(f[1]);
                loanUser[loanCount] = f[2];
                loanBorrowed[loanCount] = Integer.parseInt(f[3]);
                loanDue[loanCount] = Integer.parseInt(f[4]);
                loanReturned[loanCount] = Integer.parseInt(f[5]);
                loanCount++;
            }
        } catch (NumberFormatException e) {
            throw new IOException("corrupt number in " + LOANS_FILE);
        }
    }

    static void saveAll() throws IOException {
        saveSettings();
        saveUsers();
        saveBooks();
        saveLoans();
    }

    static void saveSettings() throws IOException {
        try (FileWriter writer = new FileWriter(fileNamed(SETTINGS_FILE))) {
            writer.write(today + "\n");
            writer.write(LOAN_DAYS + "\n");
            writer.write(MAX_ACTIVE_LOANS + "\n");
            writer.write(DUE_SOON_DAYS + "\n");
            writer.write(MIN_PASSWORD + "\n");
        }
    }

    static void saveUsers() throws IOException {
        try (FileWriter writer = new FileWriter(fileNamed(USERS_FILE))) {
            for (int i = 0; i < userCount; i++) {
                writer.write(userName[i] + ";" + userFullName[i] + ";" + userPassword[i] + ";" + userRole[i] + "\n");
            }
        }
    }

    static void saveBooks() throws IOException {
        try (FileWriter writer = new FileWriter(fileNamed(BOOKS_FILE))) {
            for (int i = 0; i < bookCount; i++) {
                int onLoan = bookOnLoan[i] ? 1 : 0;
                writer.write(bookId[i] + ";" + bookTitle[i] + ";" + bookCategory[i] + ";"
                        + bookGenre[i] + ";" + bookYear[i] + ";" + onLoan + "\n");
            }
        }
    }

    static void saveLoans() throws IOException {
        try (FileWriter writer = new FileWriter(fileNamed(LOANS_FILE))) {
            for (int k = 0; k < loanCount; k++) {
                writer.write(loanId[k] + ";" + loanBookId[k] + ";" + loanUser[k] + ";"
                        + loanBorrowed[k] + ";" + loanDue[k] + ";" + loanReturned[k] + "\n");
            }
        }
    }

    // 7. ACTIVITY LOG

    static void audit(String text) throws IOException {
        try (FileWriter writer = new FileWriter(fileNamed(ACTIVITY_LOG), true)) {
            writer.write(fromDayNumber(today) + " " + text + "\n");
        }
    }

    /** View the activity log. Admin only. */
    static void viewActivityLog(String actor) throws IOException {
        requireAdmin(actor);
        File file = fileNamed(ACTIVITY_LOG);
        System.out.println("==================== Activity Log ====================");
        if (!file.exists()) {
            System.out.println("No activity has been recorded yet.");
        } else {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        }
        System.out.println("=======================================================");
        audit("VIEW_ACTIVITY_LOG by " + actor);
    }

    // Display and update library-wide settings. Admin only.

    static void systemSettingsScreen(String actor) throws IOException {
        requireAdmin(actor);
        System.out.println("==================== System Settings ====================");
        System.out.println("Library date                 : " + fromDayNumber(today));
        System.out.println("Loan period                  : " + LOAN_DAYS + " days");
        System.out.println("Maximum active loans/member  : " + MAX_ACTIVE_LOANS);
        System.out.println("Days in due-soon window      : " + DUE_SOON_DAYS);
        System.out.println("Minimum password length      : " + MIN_PASSWORD);
        System.out.println("==========================================================");

        String changeDate = ask("Change library date? (y/n)");
        if (changeDate.toLowerCase().startsWith("y")) {
            setDate(actor, ask("New library date (YYYY-MM-DD)"));
        }

        String changePolicies = ask("Update circulation/security settings? (y/n)");
        if (changePolicies.toLowerCase().startsWith("y")) {
            System.out.println("Leave a setting blank to keep its current value.");
            int newLoanDays = readSettingInput("Loan period in days [" + LOAN_DAYS + "]", LOAN_DAYS, 1, 365);
            int newMaxLoans = readSettingInput("Maximum active loans per member [" + MAX_ACTIVE_LOANS + "]",
                    MAX_ACTIVE_LOANS, 1, 20);
            int newDueSoon = readSettingInput("Due-soon window in days [" + DUE_SOON_DAYS + "]",
                    DUE_SOON_DAYS, 0, 60);
            int newMinPassword = readSettingInput("Minimum password length [" + MIN_PASSWORD + "]",
                    MIN_PASSWORD, 4, 32);

            LOAN_DAYS = newLoanDays;
            MAX_ACTIVE_LOANS = newMaxLoans;
            DUE_SOON_DAYS = newDueSoon;
            MIN_PASSWORD = newMinPassword;
            saveSettings();
            audit("UPDATE_SYSTEM_SETTINGS by " + actor);
            System.out.println("System settings updated. New loan rules apply to future transactions.");
        }
    }

    static int readSettingInput(String prompt, int current, int minimum, int maximum) {
        String entered = ask(prompt);
        if (entered.length() == 0) {
            return current;
        }
        int value;
        try {
            value = Integer.parseInt(entered);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Enter a whole number.");
        }
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException("Value must be between " + minimum + " and " + maximum + ".");
        }
        return value;
    }

    // Count lines in the activity log for the administrative report.
    static int activityCount() throws IOException {
        File file = fileNamed(ACTIVITY_LOG);
        if (!file.exists()) {
            return 0;
        }
        int count = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            while (reader.readLine() != null) {
                count++;
            }
        }
        return count;
    }

    // Administrative overview of users, collection, loans, and recorded activity.

    static void administrativeReport(String actor) throws IOException {
        requireAdmin(actor);
        int admins = 0;
        int librarians = 0;
        int members = 0;
        for (int i = 0; i < userCount; i++) {
            if (ADMIN.equals(userRole[i])) {
                admins++;
            } else if (LIBRARIAN.equals(userRole[i])) {
                librarians++;
            } else if (MEMBER.equals(userRole[i])) {
                members++;
            }
        }

        int availableBooks = 0;
        int booksOnLoan = 0;
        for (int i = 0; i < bookCount; i++) {
            if (bookOnLoan[i]) {
                booksOnLoan++;
            } else {
                availableBooks++;
            }
        }

        int activeLoans = 0;
        int overdueLoans = 0;
        int returnedLoans = 0;
        for (int k = 0; k < loanCount; k++) {
            if (loanReturned[k] == -1) {
                activeLoans++;
                if (isOverdue(k)) {
                    overdueLoans++;
                }
            } else {
                returnedLoans++;
            }
        }

        System.out.println("================ Administrative Report ================");
        System.out.println("Report date       : " + fromDayNumber(today));
        System.out.println("[Accounts]");
        System.out.println("  Administrators  : " + admins);
        System.out.println("  Librarians      : " + librarians);
        System.out.println("  Members         : " + members);
        System.out.println("  Total users     : " + userCount);
        System.out.println("[Collection]");
        System.out.println("  Total books     : " + bookCount);
        System.out.println("  Available       : " + availableBooks);
        System.out.println("  On loan         : " + booksOnLoan);
        System.out.println("[Circulation]");
        System.out.println("  Active loans    : " + activeLoans);
        System.out.println("  Overdue loans   : " + overdueLoans);
        System.out.println("  Returned loans  : " + returnedLoans);
        System.out.println("  All loan records: " + loanCount);
        System.out.println("[System activity]");
        System.out.println("  Log entries     : " + activityCount());
        System.out.println("=======================================================");
        audit("VIEW_ADMIN_REPORT by " + actor);
    }

    // Copy a text file line by line.
    static void copyTextFile(File source, File target) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(source))) {
            try (FileWriter writer = new FileWriter(target)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line + "\n");
                }
            }
        }
    }

    // Create a numbered backup folder containing all library data files.
    static void createBackup(String actor) throws IOException {
        requireAdmin(actor);
        saveAll();
        audit("CREATE_BACKUP by " + actor);

        File backupRoot = new File(backupsPath());
        if (!backupRoot.exists() && !backupRoot.mkdir()) {
            throw new IOException("Could not create the backups folder.");
        }
        int number = 1;
        String backupFolder = backupsPath() + "/backup_" + number;
        while (new File(backupFolder).exists()) {
            number++;
            backupFolder = backupsPath() + "/backup_" + number;
        }
        if (!new File(backupFolder).mkdir()) {
            throw new IOException("Could not create a backup folder.");
        }

        String[] names = {BOOKS_FILE, USERS_FILE, LOANS_FILE, SETTINGS_FILE, ACTIVITY_LOG};
        for (int i = 0; i < names.length; i++) {
            File source = fileNamed(names[i]);
            if (source.exists()) {
                copyTextFile(source, new File(backupFolder + "/" + names[i]));
            }
        }
        System.out.println("Backup created: " + backupFolder);
    }

    // Reset counters and reload the data files after restoring a backup.
    static void reloadAll() throws IOException {
        bookCount = 0;
        userCount = 0;
        loanCount = 0;
        today = 0;
        loadAll();
    }

    // Restore a selected backup after first making a safety backup of current data.
    static void restoreBackup(String actor, String backupName) throws IOException {
        requireAdmin(actor);
        if (!backupName.startsWith("backup_") || hasSeparator(backupName) || backupName.length() <= 7) {
            throw new IllegalArgumentException("Enter a valid backup folder name, such as backup_1.");
        }
        for (int i = 7; i < backupName.length(); i++) {
            if (backupName.charAt(i) < '0' || backupName.charAt(i) > '9') {
                throw new IllegalArgumentException("Enter a valid backup folder name, such as backup_1.");
            }
        }
        String backupFolder = backupsPath() + "/" + backupName;
        if (!new File(backupFolder).exists()) {
            throw new IllegalArgumentException("That backup folder does not exist.");
        }

        String[] required = {BOOKS_FILE, USERS_FILE, LOANS_FILE, SETTINGS_FILE};
        for (int i = 0; i < required.length; i++) {
            if (!new File(backupFolder + "/" + required[i]).exists()) {
                throw new IllegalArgumentException("The selected backup is incomplete: missing " + required[i] + ".");
            }
        }

        createBackup(actor); // Preserve the current data before restoring older records.
        for (int i = 0; i < required.length; i++) {
            copyTextFile(new File(backupFolder + "/" + required[i]), fileNamed(required[i]));
        }
        File savedLog = new File(backupFolder + "/" + ACTIVITY_LOG);
        if (savedLog.exists()) {
            copyTextFile(savedLog, fileNamed(ACTIVITY_LOG));
        }
        reloadAll();
        if (!ADMIN.equals(roleOf(actor))) {
            sessionUser = null;
        }
        audit("RESTORE_BACKUP " + backupName + " by " + actor);
        System.out.println("Backup '" + backupName + "' restored successfully.");
        if (sessionUser == null) {
            System.out.println("The restored accounts changed your access. Please log in again.");
        }
    }

    // Administrator menu option for creating or restoring a backup.
    static void backupRestoreScreen(String actor) throws IOException {
        requireAdmin(actor);
        System.out.println("1. Create a backup");
        System.out.println("2. Restore a backup");
        System.out.println("0. Cancel");
        int choice = askInt("Choose");
        if (choice == 1) {
            createBackup(actor);
        } else if (choice == 2) {
            // Backups are numbered backup_1, backup_2, ... so list them until a number is missing.
            if (new File(backupsPath() + "/backup_1").exists()) {
                System.out.println("Available backups:");
            }
            boolean found = false;
            int n = 1;
            while (new File(backupsPath() + "/backup_" + n).exists()) {
                System.out.println("  backup_" + n);
                found = true;
                n++;
            }
            if (!found) {
                System.out.println("No backups are available.");
                return;
            }
            String backupName = ask("Backup folder to restore (example: backup_1)");
            if (!"YES".equals(ask("Type YES to restore '" + backupName + "'"))) {
                System.out.println("Restore cancelled.");
                return;
            }
            restoreBackup(actor, backupName);
        } else if (choice != 0) {
            System.out.println("Invalid option.");
        }
    }
 // 8. LOGIN AND LOGOUT

    static void login() throws IOException {
        String name = ask("Username");
        String password = ask("Password");
        int i = findUser(name);
        if (i < 0 || !userPassword[i].equals(password)) {
            audit("LOGIN_FAILED " + name);
            // Same message for unknown user and wrong password, so usernames are not revealed.
            throw new IllegalArgumentException("Invalid username or password.");
        }
        sessionUser = userName[i];
        audit("LOGIN " + userName[i] + " role=" + userRole[i]);
        System.out.println("Logged in as " + userFullName[i] + " (" + roleLabel(userRole[i]) + ").");
    }

    static void logout() throws IOException {
        audit("LOGOUT " + sessionUser);
        System.out.println("Logged out.");
        sessionUser = null;
    }

    // 9. USER CRED

    // CREATE. actor == null means self-registration, which only creates members.
    static void createUser(String actor, String username, String fullName, String password, String role)
            throws IOException {
        if (actor == null) {
            if (!MEMBER.equals(role)) {
                throw new IllegalArgumentException("Self-registration only creates member accounts.");
            }
        } else {
            requireStaff(actor);
            if (!MEMBER.equals(role) && !ADMIN.equals(roleOf(actor))) {
                throw new IllegalArgumentException("Only administrators can create librarian or administrator accounts.");
            }
        }
        checkUsername(username);
        checkText(fullName, "Full name");
        checkPassword(password);
        if (findUser(username) >= 0) {
            throw new IllegalArgumentException("Username '" + username + "' is already taken.");
        }
        if (userCount >= MAX_USERS) {
            throw new IllegalArgumentException("The user list is full.");
        }
        userName[userCount] = username;
        userFullName[userCount] = fullName;
        userPassword[userCount] = password;
        userRole[userCount] = role;
        userCount++;
        saveUsers();
        audit("CREATE_USER " + username + " role=" + role + " by "
                + (actor == null ? "self-registration" : actor));
        System.out.println("Created " + roleLabel(role) + " account '" + username + "'.");
    }

    // GET one user. Admins can view anyone; librarians can view members; members can view themselves.
    static void viewUser(String actor, String username) {
        int i = findUser(username);
        if (i < 0) {
            throw new IllegalArgumentException("No user named '" + username + "'.");
        }
        String actorRole = roleOf(actor);
        if (ADMIN.equals(actorRole)) {
            // Administrators may view every account.
        } else if (LIBRARIAN.equals(actorRole)) {
            if (!MEMBER.equals(userRole[i])) {
                throw new IllegalArgumentException("Librarians can only view member accounts.");
            }
        } else if (!sameName(userName[i], actor)) {
            throw new IllegalArgumentException("You can only view your own account.");
        }
        System.out.println("Username : " + userName[i]);
        System.out.println("Name     : " + userFullName[i]);
        System.out.println("Role     : " + roleLabel(userRole[i]));
    }

    /** GET all users. Admin only. */
    static void listUsers(String actor) {
        requireAdmin(actor);
        System.out.println(pad("Username", 18) + " " + pad("Full name", 26) + " Role");
        for (int i = 0; i < userCount; i++) {
            System.out.println(pad(userName[i], 18) + " " + pad(cut(userFullName[i], 26), 26)
                    + " " + roleLabel(userRole[i]));
        }
    }

    /*
     * UPDATE a user. Null arguments keep the current value.
     *  - anyone may edit their own name and password (a password change needs the current password)
     *  - staff may edit members; librarians cannot edit other staff or admins
     *  - only admins may change roles, and never their own
     */

    static void updateUser(String actor, String username, String newName, String newPassword,
                           String currentPassword, String newRole) throws IOException {
        int i = findUser(username);
        if (i < 0) {
            throw new IllegalArgumentException("No user named '" + username + "'.");
        }
        boolean self = sameName(userName[i], actor);
        String actorRole = roleOf(actor);
        if (!self) {
            requireStaff(actor);
            if (LIBRARIAN.equals(actorRole) && !MEMBER.equals(userRole[i])) {
                throw new IllegalArgumentException("Librarians can only update member accounts.");
            }
        }

        // Check everything first, so a failed check changes nothing.
        String name = userFullName[i];
        if (newName != null) {
            checkText(newName, "Full name");
            name = newName;
        }
        String password = userPassword[i];
        if (newPassword != null) {
            if (self && !userPassword[i].equals(currentPassword)) {
                throw new IllegalArgumentException("Current password is incorrect.");
            }
            checkPassword(newPassword);
            password = newPassword;
        }
        String role = userRole[i];
        if (newRole != null && !newRole.equals(role)) {
            if (!ADMIN.equals(actorRole)) {
                throw new IllegalArgumentException("Only administrators can change roles.");
            }
            if (self) {
                throw new IllegalArgumentException("You cannot change your own role.");
            }
            role = newRole;
        }

        userFullName[i] = name;
        userPassword[i] = password;
        userRole[i] = role;
        saveUsers();
        audit("UPDATE_USER " + userName[i] + " by " + actor);
        System.out.println("Account '" + userName[i] + "' updated.");
    }

    // DELETES a user.
    static void deleteUser(String actor, String username) throws IOException {
        requireAdmin(actor);
        int i = findUser(username);
        if (i < 0) {
            throw new IllegalArgumentException("No user named '" + username + "'.");
        }
        if (sameName(userName[i], actor)) {
            throw new IllegalArgumentException("You cannot delete your own account.");
        }
        for (int k = 0; k < loanCount; k++) {
            if (loanReturned[k] == -1 && sameName(loanUser[k], userName[i])) {
                throw new IllegalArgumentException("'" + userName[i] + "' still has books on loan. Process the returns first.");
            }
        }
        for (int j = i; j < userCount - 1; j++) {   // close the gap
            userName[j] = userName[j + 1];
            userFullName[j] = userFullName[j + 1];
            userPassword[j] = userPassword[j + 1];
            userRole[j] = userRole[j + 1];
        }
        userCount--;
        saveUsers();
        audit("DELETE_USER " + username + " by " + actor);
        System.out.println("User '" + username + "' deleted.");
    }

    // CHANGE a user's role. Admin only, and administrators cannot change their own role.
    static void changeUserRole(String actor, String username, String newRole) throws IOException {
        requireAdmin(actor);
        int i = findUser(username);
        if (i < 0) {
            throw new IllegalArgumentException("No user named '" + username + "'.");
        }
        if (sameName(userName[i], actor)) {
            throw new IllegalArgumentException("You cannot change your own role.");
        }
        if (!MEMBER.equals(newRole) && !LIBRARIAN.equals(newRole) && !ADMIN.equals(newRole)) {
            throw new IllegalArgumentException("Unknown role.");
        }
        String oldRole = userRole[i];
        userRole[i] = newRole;
        saveUsers();
        audit("CHANGE_ROLE " + userName[i] + " from=" + oldRole + " to=" + newRole + " by " + actor);
        System.out.println("Role for '" + userName[i] + "' changed to " + roleLabel(newRole) + ".");
    }

   // 10. BOOK CRUD AND SEARCH

    static int categoryIndex(String text) {
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (sameName(CATEGORIES[i], clean(text))) {
                return i;
            }
        }
        String choices = "";
        for (int i = 0; i < CATEGORIES.length; i++) {
            if (i > 0) {
                choices = choices + ", ";
            }
            choices = choices + CATEGORIES[i];
        }
        throw new IllegalArgumentException("Unknown category '" + text + "'. Choose from: " + choices);
    }

    static int parseYear(String text) {
        int year;
        try {
            year = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Year must be a whole number.");
        }
        int latest = yearOf(today);
        if (year < MIN_YEAR || year > latest) {
            throw new IllegalArgumentException("Year must be between " + MIN_YEAR + " and " + latest + ".");
        }
        return year;
    }

    // CREATE a book. Staff only.
    static void addBook(String actor, String title, String category, String genre, String yearText)
            throws IOException {
        requireStaff(actor);
        checkText(title, "Title");
        String cat = CATEGORIES[categoryIndex(category)];
        checkText(genre, "Genre");
        int year = parseYear(yearText);
        if (bookCount >= MAX_BOOKS) {
            throw new IllegalArgumentException("The book list is full.");
        }
        int id = nextBookId();
        bookId[bookCount] = id;
        bookTitle[bookCount] = title;
        bookCategory[bookCount] = cat;
        bookGenre[bookCount] = genre;
        bookYear[bookCount] = year;
        bookOnLoan[bookCount] = false;
        bookCount++;
        saveBooks();
        audit("CREATE_BOOK #" + id + " " + title + " by " + actor);
        System.out.println("Created book #" + id + " '" + title + "'.");
    }

    // GET a book by ID.
    static void viewBook(int id) {
        int i = findBook(id);
        if (i < 0) {
            throw new IllegalArgumentException("No book with ID " + id + ".");
        }
        System.out.println("ID       : " + bookId[i]);
        System.out.println("Title    : " + bookTitle[i]);
        System.out.println("Category : " + bookCategory[i]);
        System.out.println("Genre    : " + bookGenre[i]);
        System.out.println("Year     : " + bookYear[i]);
        System.out.println("Status   : " + (bookOnLoan[i] ? "On loan" : "Available"));
    }

    //UPDATE a book. Null arguments keep the current value. Staff only.
    static void updateBook(String actor, int id, String title, String category, String genre, String yearText)
            throws IOException {
        requireStaff(actor);
        int i = findBook(id);
        if (i < 0) {
            throw new IllegalArgumentException("No book with ID " + id + ".");
        }
        String newTitle = bookTitle[i];
        if (title != null) {
            checkText(title, "Title");
            newTitle = title;
        }
        String newCategory = bookCategory[i];
        if (category != null) {
            newCategory = CATEGORIES[categoryIndex(category)];
        }
        String newGenre = bookGenre[i];
        if (genre != null) {
            checkText(genre, "Genre");
            newGenre = genre;
        }
        int newYear = bookYear[i];
        if (yearText != null) {
            newYear = parseYear(yearText);
        }
        bookTitle[i] = newTitle;
        bookCategory[i] = newCategory;
        bookGenre[i] = newGenre;
        bookYear[i] = newYear;
        saveBooks();
        audit("UPDATE_BOOK #" + id + " by " + actor);
        System.out.println("Book #" + id + " updated.");
    }

    // DELETE a book. Staff only. Not allowed while it is on loan.
    static void deleteBook(String actor, int id) throws IOException {
        requireStaff(actor);
        int i = findBook(id);
        if (i < 0) {
            throw new IllegalArgumentException("No book with ID " + id + ".");
        }
        if (bookOnLoan[i]) {
            throw new IllegalArgumentException("'" + bookTitle[i] + "' is on loan and cannot be deleted.");
        }
        for (int j = i; j < bookCount - 1; j++) {   // close the gap
            bookId[j] = bookId[j + 1];
            bookTitle[j] = bookTitle[j + 1];
            bookCategory[j] = bookCategory[j + 1];
            bookGenre[j] = bookGenre[j + 1];
            bookYear[j] = bookYear[j + 1];
            bookOnLoan[j] = bookOnLoan[j + 1];
        }
        bookCount--;
        saveBooks();
        audit("DELETE_BOOK #" + id + " by " + actor);
        System.out.println("Book #" + id + " deleted.");
    }

    static void printBookHeader() {
        System.out.println(pad("ID", 5) + " " + pad("Title", 30) + " " + pad("Category", 14) + " "
                + pad("Genre", 18) + " " + pad("Year", 6) + " Status");
        System.out.println(dashes(88));
    }

    static void printBookRow(int i) {
        System.out.println(pad("" + bookId[i], 5) + " " + pad(cut(bookTitle[i], 30), 30) + " "
                + pad(bookCategory[i], 14) + " " + pad(cut(bookGenre[i], 18), 18) + " "
                + pad("" + bookYear[i], 6) + " " + (bookOnLoan[i] ? "On loan" : "Available"));
    }

    /*
     * SEARCH. A blank keyword lists all books. The keyword is matched against title,
     * genre, category and year, ignoring case. Returns how many books were printed.
     */

    static int listBooks(String keyword, boolean availableOnly) {
        String k = clean(keyword).toLowerCase();
        int shown = 0;
        printBookHeader();
        for (int i = 0; i < bookCount; i++) {
            if (availableOnly && bookOnLoan[i]) {
                continue;
            }
            boolean match = k.length() == 0
                    || bookTitle[i].toLowerCase().contains(k)
                    || bookGenre[i].toLowerCase().contains(k)
                    || bookCategory[i].toLowerCase().contains(k)
                    || ("" + bookYear[i]).contains(k);
            if (match) {
                printBookRow(i);
                shown++;
            }
        }
        if (shown == 0) {
            System.out.println("  (no books found)");
        }
        return shown;
    }
// 11. BORROWING AND RETURNING

    /*
     
Borrow rules:
members may only borrow for themselves; staff may borrow for any member
the book must exist and be on the shelf
the borrower may not have overdue books
the borrower may hold at most MAX_ACTIVE_LOANS books
*/
static void borrow(String actor, String borrower, int bookNumber) throws IOException {
   int u = findUser(borrower);
   if (u < 0) {
       throw new IllegalArgumentException("No user named '" + borrower + "'.");}
   if (MEMBER.equals(roleOf(actor)) && !sameName(userName[u], actor)) {
       throw new IllegalArgumentException("Members can only borrow books for themselves.");}
   int b = findBook(bookNumber);
   if (b < 0) {
       throw new IllegalArgumentException("No book with ID " + bookNumber + ".");}
   if (bookOnLoan[b]) {
       throw new IllegalArgumentException("'" + bookTitle[b] + "' is already on loan.");}
   int open = 0;
   boolean overdue = false;
   for (int k = 0; k < loanCount; k++) {
       if (loanReturned[k] == -1 && sameName(loanUser[k], userName[u])) {
           open++;
           if (isOverdue(k)) {
               overdue = true;}}}
   if (overdue) {
       throw new IllegalArgumentException(userFullName[u] + " has overdue books and must return them first.");}
   if (open >= MAX_ACTIVE_LOANS) {
       throw new IllegalArgumentException(userFullName[u] + " already has the maximum of "+ MAX_ACTIVE_LOANS + " active loans.");}
   if (loanCount >= MAX_LOANS) {
       throw new IllegalArgumentException("The loan list is full.");}
   int id = nextLoanId();
   loanId[loanCount] = id;
   loanBookId[loanCount] = bookNumber;
   loanUser[loanCount] = userName[u];
   loanBorrowed[loanCount] = today;
   loanDue[loanCount] = today + LOAN_DAYS;
   loanReturned[loanCount] = -1;
   loanCount++;
   bookOnLoan[b] = true;
   saveBooks();
   saveLoans();
   audit("BORROW loan #" + id + " book #" + bookNumber + " by " + userName[u]);
   System.out.println("Loan #" + id + " created. Due " + fromDayNumber(today + LOAN_DAYS)+ " (" + LOAN_DAYS + " days).");}

    static void returnLoan(String actor, int id) throws IOException {
        int k = findLoan(id);
        if (k < 0) {
            throw new IllegalArgumentException("No loan with ID " + id + ".");
        }
        if (loanReturned[k] != -1) {
            throw new IllegalArgumentException("Loan #" + id + " has already been returned.");
        }
        if (MEMBER.equals(roleOf(actor)) && !sameName(loanUser[k], actor)) {
            throw new IllegalArgumentException("You can only return your own loans.");
        }
        int late = overdueDays(k);
        loanReturned[k] = today;
        int b = findBook(loanBookId[k]);
        if (b >= 0) {
            bookOnLoan[b] = false;
        }
        saveBooks();
        saveLoans();
        audit("RETURN loan #" + id + " by " + actor);
        if (late > 0) {
            System.out.println("Returned. It was " + late + " day(s) overdue.");
        } else {
            System.out.println("Returned on time.");
        }
    }
// 12. LOAN TRACKER AND OVERDUE REPORT

    static String titleOfBook(int bookNumber) {
        int b = findBook(bookNumber);
        if (b < 0) {
            return "(deleted book)";
        }
        return bookTitle[b];
    }

    static void printLoanHeader() {
        System.out.println(pad("Loan", 5) + " " + pad("Book", 28) + " " + pad("Borrower", 12) + " "
                + pad("Borrowed", 10) + " " + pad("Due", 10) + " " + pad("Returned", 10) + " "
                + pad("Days out", 9) + " Overdue");
        System.out.println(dashes(100));
    }

    static void printLoanRow(int k) {
        String returned = "-";
        if (loanReturned[k] != -1) {
            returned = fromDayNumber(loanReturned[k]);
        }
        System.out.println(pad("" + loanId[k], 5) + " " + pad(cut(titleOfBook(loanBookId[k]), 28), 28) + " "
                + pad(cut(loanUser[k], 12), 12) + " " + pad(fromDayNumber(loanBorrowed[k]), 10) + " "
                + pad(fromDayNumber(loanDue[k]), 10) + " " + pad(returned, 10) + " "
                + pad("" + daysOut(k), 9) + " " + overdueDays(k));
    }

    // Loan tracker. Staff see every loan; members see their own.

    static void loanTracker(String actor) {
        boolean staff = isStaff(roleOf(actor));
        int shown = 0;
        printLoanHeader();
        for (int k = 0; k < loanCount; k++) {
            if (staff || sameName(loanUser[k], actor)) {
                printLoanRow(k);
                shown++;
            }
        }
        if (shown == 0) {
            System.out.println("  (no loans found)");
        }
    }
    // Open loans past their due date. Staff only.

    static void overdueReport(String actor) {
        requireStaff(actor);
        int shown = 0;
        printLoanHeader();
        for (int k = 0; k < loanCount; k++) {
            if (isOverdue(k)) {
                printLoanRow(k);
                shown++;
            }
        }
        if (shown == 0) {
            System.out.println("  No overdue loans.");
        }
    }
