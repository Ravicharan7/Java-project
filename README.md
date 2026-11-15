# Banking Transaction History and Account Management System

A console-based Java application that manages customer bank accounts, records every deposit and withdrawal as a transaction, sorts the transaction history, and saves all data using Java object serialization.

**Done by**

- M. Nishindra Reddy (160125737106)
- P. Ravi Charan Sagar (160125737116)

CBIT, Department of Information Technology

## Concepts used

`HashMap` · `ArrayList` · `Comparator` · Object Serialization (`ObjectOutputStream`) · Object Deserialization (`ObjectInputStream`) · `InputStream` / `OutputStream`

## Files

| File | Purpose |
|---|---|
| [`Main.java`](Main.java) | Menu, input handling, HashMap of accounts, saving/loading, serialization vs text-file comparison |
| [`Account.java`](Account.java) | Account number, name, balance and its `ArrayList` of transactions; `deposit()` and `withdraw()` |
| [`Transaction.java`](Transaction.java) | One deposit or withdrawal: id, type, amount, date and time, balance after |
| [`DateComparator.java`](DateComparator.java) | Sorts transactions by date and time |
| [`AmountComparator.java`](AmountComparator.java) | Sorts transactions by amount |

## Menu

```
1. Create account
2. Deposit money
3. Withdraw money
4. View account details
5. Display all accounts
6. Transaction history
7. Sort transactions by date
8. Sort transactions by amount
9. Compare serialization with text file
0. Save and exit
```

## How to compile and run

```
javac *.java
java Main
```

Run the commands from the folder that contains the files. The program creates `bank.dat` in that folder to store the data.

## Features

- Accounts are kept in a `HashMap<Integer, Account>` (key = account number), so an account is found directly.
- Each account keeps its transactions in an `ArrayList<Transaction>` in the order they happened.
- Transactions can be sorted by date or by amount, in ascending or descending order, using `Comparator` classes. The sorting is done on a copy, so the original history is not changed.
- All accounts and transactions are saved to `bank.dat` with `ObjectOutputStream` and loaded back with `ObjectInputStream` when the program starts. Data is saved after every change and on exit.
- Option 9 writes the same data to a text file (`bank.txt`), reads both files back and compares them, to show the advantages of serialization over text-based storage.
- Input checks: duplicate or non-positive account numbers, empty names, negative opening balance, amounts below 0.01, non-numeric input, and withdrawals larger than the balance are all rejected.
- If `bank.dat` is damaged, it is renamed to `bank.dat.bad` instead of being overwritten.

## Sample output

```
--- BANKING SYSTEM ---
Enter choice: 1
Account number: 1001
Customer name: Nishindra Reddy
Opening balance: 5000
Account created

Enter choice: 2
Account number: 1001
Deposit amount: 2500.50
Deposit successful. Balance: Rs. 7500.50

Enter choice: 2
Account number: 1001
Deposit amount: -100
Amount must be at least 0.01

Enter choice: 3
Account number: 1001
Withdrawal amount: 50000
Insufficient balance. Available balance: Rs. 6500.50

Enter choice: 7
Account number: 1001
1. Ascending
2. Descending
Enter choice: 2
Account 1001 (Nishindra Reddy)
Id     Type              Amount Date & Time              Balance After
1003   Withdrawal    Rs. 300.25 15-11-2026 22:18:27.095    Rs. 6200.25
1002   Withdrawal   Rs. 1000.00 15-11-2026 22:18:27.093    Rs. 6500.50
1001   Deposit      Rs. 2500.50 15-11-2026 22:18:27.087    Rs. 7500.50

Enter choice: 9

Storage          File         Size (bytes)   Accounts   Transactions
Serialization    bank.dat              683          2              3
Text file        bank.txt              215          2              3
```

The complete run is in [`sample_run.txt`](sample_run.txt).

## Serialization vs text file

**Advantages of serialization**

1. One call (`writeObject` / `readObject`) saves and restores the whole HashMap, every `Account` and the `ArrayList` of `Transaction` objects inside it. A text file needs its own format, writing code and parsing code.
2. Data types (`int`, `double`, `Date`) are kept. In a text file everything is text and must be converted back.
3. The link between an account and its transactions is restored automatically.
4. Less code, so fewer mistakes.

**Limits of serialization**

1. The file is not human-readable and only Java programs can read it.
2. If the classes are changed, old files may not load.
3. For small data the serialized file can be bigger than the text file, because it also stores class information (683 bytes against 215 bytes in the run above).
