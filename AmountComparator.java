import java.util.Comparator;

// sorts transactions by amount (smallest first)
public class AmountComparator implements Comparator<Transaction> {
    public int compare(Transaction a, Transaction b) {
        if (a.amount < b.amount) return -1;
        if (a.amount > b.amount) return 1;
        return a.id - b.id;          // same amount: smaller id first
    }
}
