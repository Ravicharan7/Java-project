import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.UUID;

/**
 * Transaction.java
 * ----------------
 * Represents ONE deposit or withdrawal.
 *
 * - Implements Serializable so that ObjectOutputStream can write it to a file
 *   (and ObjectInputStream can read it back).
 * - All fields are private (encapsulation). Only getters are provided, and the
 *   fields are final, so a transaction can never be changed after it is created
 *   (just like a real bank record).
 */
class Transaction implements Serializable {

    // Version number of this class. Java checks it when reading a saved file.
    // If the class changes and the number does not match, loading fails safely.
    private static final long serialVersionUID = 1L;

    // The two possible transaction types.
    public static final String TYPE_DEPOSIT = "Deposit";
    public static final String TYPE_WITHDRAWAL = "Withdrawal";

    private final String transactionId;      // unique ID, e.g. TXN-3F9A1C2B
    private final String type;               // "Deposit" or "Withdrawal"
    private final BigDecimal amount;         // money moved in this transaction
    private final LocalDateTime dateTime;    // when it happened (Java Date and Time API)
    private final BigDecimal balanceAfter;   // account balance after this transaction

    /**
     * Creates a new transaction. The ID and the date/time are generated here.
     */
    public Transaction(String type, BigDecimal amount, BigDecimal balanceAfter) {
        // UUID gives a random unique value; we keep the first 8 characters
        // so the ID is short and easy to read on the console.
        this.transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.type = type;
        this.amount = amount;
        this.dateTime = LocalDateTime.now();   // current date and time
        this.balanceAfter = balanceAfter;
    }

    // ---------- Getters (no setters: a transaction is read-only) ----------

    public String getTransactionId() {
        return transactionId;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }
}


/**
 * Account.java
 * ------------
 * Represents ONE customer's bank account.
 *
 * - Holds the account number, customer name, current balance and the
 *   ArrayList of all transactions of this account.
 * - Contains the deposit and withdraw operations (business rules live here).
 * - Implements Serializable, so saving an Account also saves its balance and
 *   its whole ArrayList of Transaction objects automatically.
 */
class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String accountNumber;                 // unique key of the account
    private final String customerName;
    private BigDecimal balance;                         // BigDecimal: exact money arithmetic
    private final ArrayList<Transaction> transactions;  // history of this account only

    /**
     * Creates an account. A new account starts with an EMPTY transaction history;
     * the opening balance is not counted as a deposit transaction.
     */
    public Account(String accountNumber, String customerName, BigDecimal initialBalance) {
        this.accountNumber = accountNumber;
        this.customerName = customerName;
        this.balance = initialBalance.setScale(2, RoundingMode.HALF_UP);
        this.transactions = new ArrayList<>();
    }

    // ---------- Getters ----------

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getCustomerName() {
        return customerName;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    /**
     * Returns a READ-ONLY view of the transaction list.
     * Outside classes can read the history but cannot add or remove items
     * directly (encapsulation). Only deposit() and withdraw() change it.
     */
    public List<Transaction> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    // ---------- Operations ----------

    /**
     * Deposits money into this account.
     *
     * @throws IllegalArgumentException if the amount is zero or negative
     */
    public Transaction deposit(BigDecimal amount) {
        amount = validateAmount(amount);

        balance = balance.add(amount);                       // new balance
        Transaction t = new Transaction(Transaction.TYPE_DEPOSIT, amount, balance);
        transactions.add(t);                                 // store in the ArrayList
        return t;
    }

    /**
     * Withdraws money from this account.
     *
     * @throws IllegalArgumentException if the amount is zero/negative or
     *                                  greater than the available balance
     */
    public Transaction withdraw(BigDecimal amount) {
        amount = validateAmount(amount);

        // compareTo: returns a negative number when balance < amount
        if (balance.compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Available balance is " + balance.toPlainString());
        }

        balance = balance.subtract(amount);
        Transaction t = new Transaction(Transaction.TYPE_WITHDRAWAL, amount, balance);
        transactions.add(t);
        return t;
    }

    /**
     * Rounds the amount to 2 decimal places (paise) and makes sure it is > 0.
     */
    private BigDecimal validateAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required.");
        }
        BigDecimal rounded = amount.setScale(2, RoundingMode.HALF_UP);
        if (rounded.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return rounded;
    }
}


/**
 * BankingService.java
 * -------------------
 * The "brain" of the application. It manages ALL accounts using a HashMap
 * (key = account number, value = Account object) and provides account
 * creation, lookup, deposit, withdrawal, history and sorting.
 *
 * It does NOT read from the keyboard or print menus (that is Main's job),
 * and it does NOT touch files (that is BankDataManager's job).
 */
class BankingService {

    // HashMap<AccountNumber, Account>: lookup by account number is very fast.
    private final HashMap<String, Account> accounts;

    // ---- Comparators (used for sorting transactions) ----
    // Comparator.comparing() takes a "key" method; the objects are then
    // ordered by that key. Transaction::getDateTime is a method reference.
    private static final Comparator<Transaction> BY_DATE =
            Comparator.comparing(Transaction::getDateTime);

    private static final Comparator<Transaction> BY_AMOUNT =
            Comparator.comparing(Transaction::getAmount);

    /**
     * @param accounts the map loaded from the file (or an empty map)
     */
    public BankingService(HashMap<String, Account> accounts) {
        this.accounts = accounts;
    }

    // ---------- Account management ----------

    /**
     * Creates a new account and stores it in the HashMap.
     *
     * @throws IllegalArgumentException for a blank/duplicate account number,
     *                                  a blank name or a negative balance
     */
    public void createAccount(String accountNumber, String customerName, BigDecimal initialBalance) {
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Account number cannot be empty.");
        }
        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name cannot be empty.");
        }
        if (initialBalance == null || initialBalance.signum() < 0) {
            throw new IllegalArgumentException("Initial balance cannot be negative.");
        }
        if (accounts.containsKey(accountNumber)) {          // duplicate check
            throw new IllegalArgumentException("Account number " + accountNumber + " already exists.");
        }
        accounts.put(accountNumber, new Account(accountNumber, customerName.trim(), initialBalance));
    }

    /** Returns true if an account with this number exists. */
    public boolean accountExists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    /**
     * Finds an account by its number.
     *
     * @throws IllegalArgumentException if no such account exists
     */
    public Account getAccount(String accountNumber) {
        Account account = accounts.get(accountNumber);       // HashMap lookup, O(1) on average
        if (account == null) {
            throw new IllegalArgumentException("Account number " + accountNumber + " does not exist.");
        }
        return account;
    }

    /**
     * Returns all accounts as a list sorted by account number
     * (a HashMap has no guaranteed order, so we sort for a neat display).
     */
    public List<Account> getAllAccounts() {
        List<Account> list = new ArrayList<>(accounts.values());
        list.sort(Comparator.comparing(Account::getAccountNumber));
        return list;
    }

    /**
     * Returns a copy of the HashMap. BankDataManager serializes this copy.
     * (The Account objects inside are the same objects, so nothing is lost.)
     */
    public HashMap<String, Account> getAccountsMap() {
        return new HashMap<>(accounts);
    }

    // ---------- Deposit and withdrawal ----------

    public Transaction deposit(String accountNumber, BigDecimal amount) {
        return getAccount(accountNumber).deposit(amount);
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount) {
        return getAccount(accountNumber).withdraw(amount);
    }

    // ---------- Transaction history and sorting ----------

    /** Returns the transactions in the original (chronological) order. */
    public List<Transaction> getTransactionHistory(String accountNumber) {
        return new ArrayList<>(getAccount(accountNumber).getTransactions());
    }

    /**
     * Returns a SORTED COPY of the transactions by date and time.
     * The original list inside the Account is never changed.
     */
    public List<Transaction> getTransactionsSortedByDate(String accountNumber, boolean ascending) {
        return sortCopy(accountNumber, ascending ? BY_DATE : BY_DATE.reversed());
    }

    /**
     * Returns a SORTED COPY of the transactions by amount.
     * The original list inside the Account is never changed.
     */
    public List<Transaction> getTransactionsSortedByAmount(String accountNumber, boolean ascending) {
        return sortCopy(accountNumber, ascending ? BY_AMOUNT : BY_AMOUNT.reversed());
    }

    /** Copies the history into a new ArrayList and sorts the COPY. */
    private List<Transaction> sortCopy(String accountNumber, Comparator<Transaction> comparator) {
        List<Transaction> copy = new ArrayList<>(getAccount(accountNumber).getTransactions());
        copy.sort(comparator);               // List.sort() with a Comparator
        return copy;
    }
}


/**
 * BankDataManager.java
 * --------------------
 * Saves and loads the whole bank (HashMap of Account objects, including every
 * Transaction) using Java object serialization.
 *
 * Saving   : Java objects -> ObjectOutputStream -> FileOutputStream -> bank-data.dat
 * Loading  : bank-data.dat -> FileInputStream -> ObjectInputStream -> Java objects
 *
 * SECURITY NOTE: deserializing data from an unknown source can be dangerous.
 * This program only reads its OWN local file (bank-data.dat), which is trusted.
 */
class BankDataManager {

    private final String fileName;

    public BankDataManager(String fileName) {
        this.fileName = fileName;
    }

    /**
     * SERIALIZATION: writes the whole HashMap to the file.
     * try-with-resources closes both streams automatically, even on errors.
     *
     * @return true if saved successfully, false if an error occurred
     */
    public boolean saveData(HashMap<String, Account> accounts) {
        try (OutputStream fileOut = new FileOutputStream(fileName);
             ObjectOutputStream objectOut = new ObjectOutputStream(fileOut)) {

            objectOut.writeObject(accounts);   // writes the map, accounts and transactions
            return true;

        } catch (IOException e) {
            System.out.println("Error: could not save data - " + e.getMessage());
            return false;
        }
    }

    /**
     * DESERIALIZATION: reads the HashMap back from the file.
     * - If the file does not exist -> returns an empty HashMap (first run).
     * - If the file is corrupted/incompatible -> prints a warning, keeps the
     *   bad file as a backup and returns an empty HashMap.
     */
    public HashMap<String, Account> loadData() {
        File file = new File(fileName);

        if (!file.exists()) {
            System.out.println("No saved data file found. Starting with an empty bank.");
            return new HashMap<>();
        }

        try (InputStream fileIn = new FileInputStream(file);
             ObjectInputStream objectIn = new ObjectInputStream(fileIn)) {

            Object data = objectIn.readObject();   // returns a plain Object

            // Check what we got instead of blindly casting it.
            if (!(data instanceof HashMap)) {
                throw new InvalidObjectException("File does not contain the expected data.");
            }

            HashMap<String, Account> loaded = new HashMap<>();
            for (Map.Entry<?, ?> entry : ((HashMap<?, ?>) data).entrySet()) {
                if (entry.getKey() instanceof String && entry.getValue() instanceof Account) {
                    loaded.put((String) entry.getKey(), (Account) entry.getValue());
                } else {
                    throw new InvalidObjectException("File contains unexpected objects.");
                }
            }
            return loaded;

        } catch (IOException | ClassNotFoundException e) {
            // IOException covers StreamCorruptedException, InvalidClassException (version mismatch), etc.
            System.out.println("Warning: saved data could not be read (" + e.getMessage() + ").");
            keepBackupOfBadFile(file);
            return new HashMap<>();
        }
    }

    /**
     * Renames an unreadable file to "<name>.corrupt" so that the next
     * auto-save does not overwrite it.
     */
    private void keepBackupOfBadFile(File file) {
        File backup = new File(fileName + ".corrupt");
        if (backup.exists()) {
            backup.delete();
        }
        if (file.renameTo(backup)) {
            System.out.println("The unreadable file was renamed to " + backup.getName()
                    + ". Starting with an empty bank.");
        } else {
            System.out.println("Starting with an empty bank.");
        }
    }
}


/**
 * Main.java
 * ---------
 * Entry point of the application. Shows the menu, reads the user's input,
 * calls BankingService for the real work and prints the results.
 */
public class Main {

    private static final String DATA_FILE = "bank-data.dat";
    // The rupee sign. If your console shows "?" or strange characters instead of it,
    // change this to "Rs. " and recompile.
    private static final String RUPEE = "₹";

    // Format used to display a LocalDateTime, e.g. 15-11-2026 11:36:05.123
    // (milliseconds are shown so that the order of quick transactions is visible)
    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss.SSS");

    private static final Scanner scanner = new Scanner(System.in);
    private static final NumberFormat moneyFormat = createMoneyFormat();

    private static BankingService bankingService;
    private static BankDataManager dataManager;

    public static void main(String[] args) {
        useUtf8Output();   // so that the rupee sign prints correctly

        // 1. Load saved data (deserialization) when the program starts.
        dataManager = new BankDataManager(DATA_FILE);
        bankingService = new BankingService(dataManager.loadData());
        System.out.println("Accounts loaded: " + bankingService.getAllAccounts().size());

        // 2. Show the menu again and again until the user chooses Exit.
        boolean running = true;
        try {
            while (running) {
                showMenu();
                int choice = readChoice("Enter your choice: ");

                switch (choice) {
                    case 1:  createAccount();            break;
                    case 2:  depositMoney();             break;
                    case 3:  withdrawMoney();            break;
                    case 4:  viewAccountDetails();       break;
                    case 5:  displayAllAccounts();       break;
                    case 6:  viewTransactionHistory();   break;
                    case 7:  sortTransactions(true);     break;   // true  = by date
                    case 8:  sortTransactions(false);    break;   // false = by amount
                    case 9:  saveData(true);             break;
                    case 10: running = false;            break;
                    default: System.out.println("Invalid choice. Please enter a number from 1 to 10.");
                }
            }
        } catch (NoSuchElementException e) {
            // Input stream ended (for example Ctrl+D / Ctrl+Z). Exit cleanly.
            System.out.println("\nInput closed. Exiting.");
        }

        // 3. Save before exiting.
        saveData(false);
        System.out.println("Thank you for using the Banking Management System. Goodbye!");
    }

    /** Makes System.out write UTF-8 text (needed to print the rupee sign). */
    private static void useUtf8Output() {
        try {
            System.setOut(new PrintStream(System.out, true, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            // UTF-8 is always supported; if not, we simply keep the default output.
        }
    }

    // =====================================================================
    //                              MENU
    // =====================================================================

    private static void showMenu() {
        System.out.println();
        System.out.println("========================================");
        System.out.println("       BANKING MANAGEMENT SYSTEM");
        System.out.println("========================================");
        System.out.println();
        System.out.println("1. Create New Account");
        System.out.println("2. Deposit Money");
        System.out.println("3. Withdraw Money");
        System.out.println("4. View Account Details");
        System.out.println("5. Display All Accounts");
        System.out.println("6. View Transaction History");
        System.out.println("7. Sort Transactions by Date");
        System.out.println("8. Sort Transactions by Amount");
        System.out.println("9. Save Account Data");
        System.out.println("10. Exit Application");
        System.out.println();
        System.out.println("========================================");
        System.out.println();
    }

    // =====================================================================
    //                        MENU ACTIONS
    // =====================================================================

    /** Option 1: create a new account. */
    private static void createAccount() {
        System.out.println("\n--- Create New Account ---");
        String accountNumber = readText("Enter account number: ");

        // Stop early if the number is already used.
        if (bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " already exists.");
            return;
        }

        String name = readText("Enter customer name: ");
        BigDecimal initialBalance = readAmount("Enter initial balance: ");
        if (initialBalance == null) {
            return;   // readAmount has already printed the error
        }

        try {
            bankingService.createAccount(accountNumber, name, initialBalance);
            System.out.println("Account created successfully for " + name.trim() + ".");
            autoSave();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /** Option 2: deposit money. */
    private static void depositMoney() {
        System.out.println("\n--- Deposit Money ---");
        String accountNumber = readText("Enter account number: ");
        if (!bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist.");
            return;
        }

        BigDecimal amount = readAmount("Enter deposit amount: ");
        if (amount == null) {
            return;
        }

        try {
            Transaction t = bankingService.deposit(accountNumber, amount);
            System.out.println("Deposit successful. Transaction ID: " + t.getTransactionId());
            System.out.println("Updated balance: " + money(t.getBalanceAfter()));
            autoSave();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /** Option 3: withdraw money. */
    private static void withdrawMoney() {
        System.out.println("\n--- Withdraw Money ---");
        String accountNumber = readText("Enter account number: ");
        if (!bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist.");
            return;
        }

        BigDecimal amount = readAmount("Enter withdrawal amount: ");
        if (amount == null) {
            return;
        }

        try {
            Transaction t = bankingService.withdraw(accountNumber, amount);
            System.out.println("Withdrawal successful. Transaction ID: " + t.getTransactionId());
            System.out.println("Updated balance: " + money(t.getBalanceAfter()));
            autoSave();
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /** Option 4: show one account's details (including the current balance). */
    private static void viewAccountDetails() {
        System.out.println("\n--- Account Details ---");
        String accountNumber = readText("Enter account number: ");
        if (!bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist.");
            return;
        }

        Account account = bankingService.getAccount(accountNumber);
        System.out.println();
        System.out.println("Account Number  : " + account.getAccountNumber());
        System.out.println("Customer Name   : " + account.getCustomerName());
        System.out.println("Current Balance : " + money(account.getBalance()));
        System.out.println("Transactions    : " + account.getTransactions().size());
    }

    /** Option 5: list every account. */
    private static void displayAllAccounts() {
        System.out.println("\n--- All Accounts ---");
        List<Account> allAccounts = bankingService.getAllAccounts();

        if (allAccounts.isEmpty()) {
            System.out.println("No accounts available.");
            return;
        }

        System.out.printf("%-15s %-20s %18s %13s%n",
                "Account No.", "Customer Name", "Balance", "Transactions");
        System.out.println("---------------------------------------------------------------------");
        for (Account account : allAccounts) {
            System.out.printf("%-15s %-20s %18s %13d%n",
                    account.getAccountNumber(),
                    account.getCustomerName(),
                    money(account.getBalance()),
                    account.getTransactions().size());
        }
    }

    /** Option 6: show the full history in the order the transactions happened. */
    private static void viewTransactionHistory() {
        System.out.println("\n--- Transaction History ---");
        String accountNumber = readText("Enter account number: ");
        if (!bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist.");
            return;
        }

        List<Transaction> history = bankingService.getTransactionHistory(accountNumber);
        printTransactions(accountNumber, history, "Original order (oldest first)");
    }

    /**
     * Options 7 and 8: sort transactions by date (sortByDate = true)
     * or by amount (sortByDate = false).
     */
    private static void sortTransactions(boolean sortByDate) {
        System.out.println(sortByDate ? "\n--- Sort Transactions by Date ---"
                                      : "\n--- Sort Transactions by Amount ---");
        String accountNumber = readText("Enter account number: ");
        if (!bankingService.accountExists(accountNumber)) {
            System.out.println("Error: Account number " + accountNumber + " does not exist.");
            return;
        }

        // Small sorting menu
        System.out.println();
        System.out.println("1. Ascending Order");
        System.out.println("2. Descending Order");
        int order = readChoice("Enter your choice: ");
        if (order != 1 && order != 2) {
            System.out.println("Invalid choice. Please enter 1 or 2.");
            return;
        }
        boolean ascending = (order == 1);

        List<Transaction> sorted;
        String heading;
        if (sortByDate) {
            sorted = bankingService.getTransactionsSortedByDate(accountNumber, ascending);
            heading = "Sorted by date - " + (ascending ? "oldest to newest" : "newest to oldest");
        } else {
            sorted = bankingService.getTransactionsSortedByAmount(accountNumber, ascending);
            heading = "Sorted by amount - " + (ascending ? "lowest to highest" : "highest to lowest");
        }
        printTransactions(accountNumber, sorted, heading);
    }

    /** Option 9 (and the save done on exit): serialize everything to bank-data.dat. */
    private static void saveData(boolean showMessage) {
        boolean saved = dataManager.saveData(bankingService.getAccountsMap());
        if (saved) {
            System.out.println("Data saved successfully to " + DATA_FILE + ".");
        } else if (showMessage) {
            System.out.println("Failed to save data.");
        }
    }

    /** Silent save after every change (account created, deposit, withdrawal). */
    private static void autoSave() {
        if (dataManager.saveData(bankingService.getAccountsMap())) {
            System.out.println("(Data saved automatically)");
        }
    }

    // =====================================================================
    //                       DISPLAY HELPERS
    // =====================================================================

    /** Prints a list of transactions as a table. */
    private static void printTransactions(String accountNumber, List<Transaction> list, String heading) {
        Account account = bankingService.getAccount(accountNumber);
        System.out.println();
        System.out.println("Account: " + account.getAccountNumber() + " (" + account.getCustomerName() + ")");
        System.out.println(heading);

        if (list.isEmpty()) {
            System.out.println("No transactions found for this account.");
            return;
        }

        System.out.println();
        System.out.printf("%-3s %-13s %-12s %15s %-23s %15s%n",
                "No", "Txn ID", "Type", "Amount", "Date & Time", "Balance After");
        System.out.println("-------------------------------------------------------------------------------------");
        int number = 1;
        for (Transaction t : list) {
            System.out.printf("%-3d %-13s %-12s %15s %-23s %15s%n",
                    number++,
                    t.getTransactionId(),
                    t.getType(),
                    money(t.getAmount()),
                    t.getDateTime().format(DATE_FORMAT),
                    money(t.getBalanceAfter()));
        }
    }

    /** Formats an amount like the rupee sign followed by 6,500.00 */
    private static String money(BigDecimal amount) {
        return RUPEE + moneyFormat.format(amount);
    }

    private static NumberFormat createMoneyFormat() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN"));
        format.setMinimumFractionDigits(2);
        format.setMaximumFractionDigits(2);
        return format;
    }

    // =====================================================================
    //                       INPUT HELPERS
    // =====================================================================

    /** Prints a prompt and returns the trimmed line the user typed. */
    private static String readText(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }

    /** Reads a menu number. Returns -1 if the user typed something that is not a number. */
    private static int readChoice(String prompt) {
        String text = readText(prompt);
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /**
     * Reads a money amount as BigDecimal. Returns null (after printing an
     * error) if the text is not a valid number.
     */
    private static BigDecimal readAmount(String prompt) {
        String text = readText(prompt).replace(",", "");   // allow 5,000
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            System.out.println("Error: Invalid amount. Please enter a number such as 2500 or 2500.50.");
            return null;
        }
    }
}
