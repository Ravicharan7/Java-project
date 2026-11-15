import java.io.*;
import java.util.*;

public class Main {

    static HashMap<Integer, Account> accounts = new HashMap<Integer, Account>();   // account number -> account
    static Scanner sc = new Scanner(System.in);
    static int nextTxnId = 1001;                      // id of the next transaction
    static final String DATA_FILE = "bank.dat";       // serialized file
    static final String TEXT_FILE = "bank.txt";       // normal text file (only used for comparison)
    static final double MAX_AMOUNT = 1000000000;      // largest amount allowed in one go

    public static void main(String[] args) {
        loadFromFile();   // load old accounts if the file is there

        int choice = -1;
        try {
            while (choice != 0) {
                System.out.println("\n--- BANKING SYSTEM ---");
                System.out.println("1. Create account");
                System.out.println("2. Deposit money");
                System.out.println("3. Withdraw money");
                System.out.println("4. View account details");
                System.out.println("5. Display all accounts");
                System.out.println("6. Transaction history");
                System.out.println("7. Sort transactions by date");
                System.out.println("8. Sort transactions by amount");
                System.out.println("9. Compare serialization with text file");
                System.out.println("0. Save and exit");
                choice = readInt("Enter choice: ");

                if (choice == 1) {
                    createAccount();
                } else if (choice == 2) {
                    deposit();
                } else if (choice == 3) {
                    withdraw();
                } else if (choice == 4) {
                    viewAccount();
                } else if (choice == 5) {
                    displayAll();
                } else if (choice == 6) {
                    history();
                } else if (choice == 7) {
                    sortTransactions(true);
                } else if (choice == 8) {
                    sortTransactions(false);
                } else if (choice == 9) {
                    compareStorage();
                } else if (choice == 0) {
                    // saved below
                } else {
                    System.out.println("Wrong choice");
                }
            }
        } catch (NoSuchElementException e) {
            System.out.println("\nInput closed.");     // keyboard input ended (Ctrl+D / Ctrl+Z)
        }
        saveToFile(true);
        System.out.println("Bye!");
    }

    // ---------------------------------------------------------------- menu actions

    static void createAccount() {
        int accNo = readInt("Account number: ");
        if (accNo <= 0) {
            System.out.println("Account number must be a positive number");
            return;
        }
        if (accounts.containsKey(accNo)) {
            System.out.println("Account number already exists!");
            return;
        }
        String name = readText("Customer name: ");
        double balance = readDouble("Opening balance: ");
        if (balance < 0 || balance > MAX_AMOUNT) {
            System.out.println("Opening balance must be between 0 and " + (long) MAX_AMOUNT);
            return;
        }
        accounts.put(accNo, new Account(accNo, name, round(balance)));
        System.out.println("Account created");
        saveToFile(false);
    }

    static void deposit() {
        Account a = findAccount();
        if (a == null) {
            return;
        }
        double amount = readAmount("Deposit amount: ");
        if (amount == 0) {
            return;
        }
        a.deposit(amount, nextTxnId++);
        System.out.println("Deposit successful. Balance: " + money(a.balance));
        saveToFile(false);
    }

    static void withdraw() {
        Account a = findAccount();
        if (a == null) {
            return;
        }
        double amount = readAmount("Withdrawal amount: ");
        if (amount == 0) {
            return;
        }
        if (a.withdraw(amount, nextTxnId++)) {
            System.out.println("Withdrawal successful. Balance: " + money(a.balance));
            saveToFile(false);
        } else {
            nextTxnId--;      // the id was not used
            System.out.println("Insufficient balance. Available balance: " + money(a.balance));
        }
    }

    static void viewAccount() {
        Account a = findAccount();
        if (a == null) {
            return;
        }
        System.out.println("Account number : " + a.accNo);
        System.out.println("Customer name  : " + a.name);
        System.out.println("Balance        : " + money(a.balance));
        System.out.println("Transactions   : " + a.history.size());
    }

    static void displayAll() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts");
            return;
        }
        // a HashMap has no fixed order, so the account numbers are sorted first
        ArrayList<Integer> numbers = new ArrayList<Integer>(accounts.keySet());
        Collections.sort(numbers);

        System.out.printf("%-10s %-20s %14s %13s%n", "Account", "Name", "Balance", "Transactions");
        for (int n : numbers) {
            Account a = accounts.get(n);
            System.out.printf("%-10d %-20s %14s %13d%n", a.accNo, a.name, money(a.balance), a.history.size());
        }
    }

    static void history() {
        Account a = findAccount();
        if (a == null) {
            return;
        }
        printTransactions(a, a.history);
    }

    // sortByDate = true sorts by date, false sorts by amount
    static void sortTransactions(boolean sortByDate) {
        Account a = findAccount();
        if (a == null) {
            return;
        }
        System.out.println("1. Ascending");
        System.out.println("2. Descending");
        int order = readInt("Enter choice: ");
        if (order != 1 && order != 2) {
            System.out.println("Wrong choice");
            return;
        }

        // sort a COPY, so the original history keeps its order
        ArrayList<Transaction> copy = new ArrayList<Transaction>(a.history);
        Comparator<Transaction> cmp;
        if (sortByDate) {
            cmp = new DateComparator();
        } else {
            cmp = new AmountComparator();
        }
        if (order == 1) {
            Collections.sort(copy, cmp);
        } else {
            Collections.sort(copy, Collections.reverseOrder(cmp));
        }
        printTransactions(a, copy);
    }

    // ---------------------------------------------------------------- serialization

    // Serialization - save the whole HashMap (accounts and their transactions) in a file
    static void saveToFile(boolean show) {
        try {
            OutputStream fileOut = new FileOutputStream(DATA_FILE);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(accounts);
            out.close();
            if (show) {
                System.out.println("Saved " + accounts.size() + " accounts in " + DATA_FILE);
            }
        } catch (IOException e) {
            System.out.println("Error while saving: " + e);
        }
    }

    // Deserialization - read the HashMap back from the file
    static void loadFromFile() {
        HashMap<Integer, Account> loaded = readObjectFile();
        if (loaded == null) {
            // if the file exists but cannot be read, keep it as a backup so it is not overwritten
            File f = new File(DATA_FILE);
            if (f.exists() && f.renameTo(new File(DATA_FILE + ".bad"))) {
                System.out.println("The unreadable file was renamed to " + DATA_FILE + ".bad");
            }
            return;
        }
        accounts = loaded;
        // continue transaction ids after the biggest old id
        for (Account a : accounts.values()) {
            for (Transaction t : a.history) {
                if (t.id >= nextTxnId) {
                    nextTxnId = t.id + 1;
                }
            }
        }
        System.out.println("Loaded " + accounts.size() + " accounts from file");
    }

    // returns the HashMap stored in bank.dat, or null if the file is missing or cannot be read
    @SuppressWarnings("unchecked")
    static HashMap<Integer, Account> readObjectFile() {
        File f = new File(DATA_FILE);
        if (!f.exists()) {
            return null;
        }
        try {
            InputStream fileIn = new FileInputStream(f);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            HashMap<Integer, Account> data = (HashMap<Integer, Account>) in.readObject();
            in.close();
            return data;
        } catch (Exception e) {
            System.out.println("Error while loading: " + e);
            return null;
        }
    }

    // ---------------------------------------------------------------- serialization vs text file

    // Writes the same data in serialized form and as a normal text file, reads both back
    // and compares them. In the text file every value is written as text and has to be
    // converted back with our own code (see readTextFile).
    static void compareStorage() {
        if (accounts.isEmpty()) {
            System.out.println("No accounts to save");
            return;
        }
        saveToFile(false);
        if (!writeTextFile()) {
            return;
        }
        HashMap<Integer, Account> fromObjects = readObjectFile();
        HashMap<Integer, Account> fromText = readTextFile();
        if (fromObjects == null || fromText == null) {
            return;
        }

        long objectSize = new File(DATA_FILE).length();
        long textSize = new File(TEXT_FILE).length();

        System.out.println();
        System.out.printf("%-16s %-10s %14s %10s %14s%n", "Storage", "File", "Size (bytes)", "Accounts", "Transactions");
        System.out.printf("%-16s %-10s %14d %10d %14d%n", "Serialization", DATA_FILE, objectSize,
                fromObjects.size(), countTransactions(fromObjects));
        System.out.printf("%-16s %-10s %14d %10d %14d%n", "Text file", TEXT_FILE, textSize,
                fromText.size(), countTransactions(fromText));

        System.out.println();
        System.out.println("Advantages of serialization over text file storage:");
        System.out.println("1. One call (writeObject / readObject) saves and restores the whole HashMap,");
        System.out.println("   every Account and the ArrayList of Transactions inside it.");
        System.out.println("   The text file needs our own format, writing code and parsing code.");
        System.out.println("2. Data types are kept (int, double, Date). In a text file everything is text");
        System.out.println("   and must be converted back, and bad text can cause errors.");
        System.out.println("3. The link between an account and its transactions is restored automatically.");
        System.out.println("4. Less code, so fewer mistakes (a name with the separator | would break the text file).");
        System.out.println("Limits of serialization:");
        System.out.println("1. The file is not readable by humans and only Java programs can read it.");
        System.out.println("2. If the classes are changed, old files may not load.");
        if (objectSize > textSize) {
            System.out.println("3. Here the serialized file is " + (objectSize - textSize)
                    + " bytes bigger, because it also stores class information.");
        } else {
            System.out.println("3. Here the serialized file is not bigger than the text file.");
        }
    }

    // writes accounts and transactions as text lines:
    //   A|accNo|name|balance
    //   T|accNo|id|type|amount|time in milliseconds|balanceAfter
    static boolean writeTextFile() {
        try {
            FileWriter fw = new FileWriter(TEXT_FILE);
            for (Account a : accounts.values()) {
                fw.write("A|" + a.accNo + "|" + a.name.replace("|", " ") + "|" + a.balance + "\n");
                for (Transaction t : a.history) {
                    fw.write("T|" + a.accNo + "|" + t.id + "|" + t.type + "|" + t.amount + "|"
                            + t.date.getTime() + "|" + t.balanceAfter + "\n");
                }
            }
            fw.close();
            return true;
        } catch (IOException e) {
            System.out.println("Error while writing text file: " + e);
            return false;
        }
    }

    // reads bank.txt line by line and builds the accounts again
    static HashMap<Integer, Account> readTextFile() {
        HashMap<Integer, Account> data = new HashMap<Integer, Account>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(TEXT_FILE));
            String line;
            while ((line = br.readLine()) != null) {
                String[] p = line.split("\\|");
                if (p[0].equals("A")) {
                    int accNo = Integer.parseInt(p[1]);
                    data.put(accNo, new Account(accNo, p[2], Double.parseDouble(p[3])));
                } else if (p[0].equals("T")) {
                    Account a = data.get(Integer.parseInt(p[1]));
                    a.history.add(new Transaction(Integer.parseInt(p[2]), p[3], Double.parseDouble(p[4]),
                            new Date(Long.parseLong(p[5])), Double.parseDouble(p[6])));
                }
            }
            br.close();
            return data;
        } catch (Exception e) {
            System.out.println("Error while reading text file: " + e);
            return null;
        }
    }

    static int countTransactions(HashMap<Integer, Account> data) {
        int count = 0;
        for (Account a : data.values()) {
            count += a.history.size();
        }
        return count;
    }

    // ---------------------------------------------------------------- helper methods

    // asks for an account number and returns that account (null if it does not exist)
    static Account findAccount() {
        int accNo = readInt("Account number: ");
        Account a = accounts.get(accNo);       // HashMap gives the account directly
        if (a == null) {
            System.out.println("Account not found");
        }
        return a;
    }

    static void printTransactions(Account a, ArrayList<Transaction> list) {
        System.out.println("Account " + a.accNo + " (" + a.name + ")");
        if (list.isEmpty()) {
            System.out.println("No transactions");
            return;
        }
        System.out.printf("%-6s %-11s %12s %-23s %14s%n", "Id", "Type", "Amount", "Date & Time", "Balance After");
        for (Transaction t : list) {
            System.out.printf("%-6d %-11s %12s %-23s %14s%n", t.id, t.type, money(t.amount),
                    t.getDateText(), money(t.balanceAfter));
        }
    }

    // reads an amount that must be more than 0 (returns 0 after printing an error)
    static double readAmount(String prompt) {
        double amount = round(readDouble(prompt));
        if (amount <= 0) {
            System.out.println("Amount must be at least 0.01");
            return 0;
        }
        if (amount > MAX_AMOUNT) {
            System.out.println("Amount is too large (maximum " + (long) MAX_AMOUNT + ")");
            return 0;
        }
        return amount;
    }

    // keeps 2 decimal places
    static double round(double x) {
        return Math.round(x * 100) / 100.0;
    }

    // 6500.5 -> Rs. 6500.50
    static String money(double x) {
        return String.format("Rs. %.2f", x);
    }

    // The read methods ask again until the user types something valid.
    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = sc.nextLine().trim();
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a whole number");
            }
        }
    }

    static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = sc.nextLine().trim().replace(",", "");     // allow 5,000
            try {
                double value = Double.parseDouble(text);
                if (!Double.isNaN(value) && !Double.isInfinite(value)) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // falls to the message below
            }
            System.out.println("Please enter a number such as 2500 or 2500.50");
        }
    }

    static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = sc.nextLine().trim();
            if (!text.isEmpty()) {
                return text;
            }
            System.out.println("This cannot be empty");
        }
    }
}
