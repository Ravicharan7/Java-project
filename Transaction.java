import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

// Transaction class - stores details of one deposit or withdrawal
// Serializable is needed to save objects in a file
public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;

    int id;                  // transaction id
    String type;             // "Deposit" or "Withdrawal"
    double amount;           // money moved in this transaction
    Date date;               // date and time of the transaction
    double balanceAfter;     // balance of the account after this transaction

    // used for a new transaction (date is the current date and time)
    public Transaction(int id, String type, double amount, double balanceAfter) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.date = new Date();
        this.balanceAfter = balanceAfter;
    }

    // used when a transaction is read back from the text file (date is already known)
    public Transaction(int id, String type, double amount, Date date, double balanceAfter) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.date = date;
        this.balanceAfter = balanceAfter;
    }

    // date in the form 15-11-2026 21:30:05.123
    public String getDateText() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS").format(date);
    }
}
