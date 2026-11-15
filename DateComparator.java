import java.util.Comparator;

// sorts transactions by date and time (oldest first)
public class DateComparator implements Comparator<Transaction> {
    public int compare(Transaction a, Transaction b) {
        int r = a.date.compareTo(b.date);
        if (r == 0) {
            return a.id - b.id;      // same time: smaller id first
        }
        return r;
    }
}
