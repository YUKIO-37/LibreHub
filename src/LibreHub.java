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
