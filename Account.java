import java.io.Serializable;
import java.util.ArrayList;

// Account class - stores details of one customer account and its transactions
// Serializable is needed to save the account (with its transaction list) in a file
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    int accNo;
    String name;
    double balance;
    ArrayList<Transaction> history = new ArrayList<Transaction>();   // all transactions of this account

    public Account(int accNo, String name, double balance) {
        this.accNo = accNo;
        this.name = name;
        this.balance = balance;
    }

    // adds money to the balance and records a Deposit transaction
    public void deposit(double amount, int txnId) {
        balance = Main.round(balance + amount);
        history.add(new Transaction(txnId, "Deposit", amount, balance));
    }

    // takes money from the balance and records a Withdrawal transaction
    // returns false (and changes nothing) if the balance is not enough
    public boolean withdraw(double amount, int txnId) {
        if (amount > balance) {
            return false;
        }
        balance = Main.round(balance - amount);
        history.add(new Transaction(txnId, "Withdrawal", amount, balance));
        return true;
    }
}
