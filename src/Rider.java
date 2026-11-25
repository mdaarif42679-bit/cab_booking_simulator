/** A rider with a unique number, a pickup location and a drop location. */
public class Rider {
    private final int id;
    private final String pickup;
    private final String drop;

    public Rider(int id, String pickup, String drop) {
        this.id = id;
        this.pickup = pickup;
        this.drop = drop;
    }

    public int getId() { return id; }
    public String getPickup() { return pickup; }
    public String getDrop() { return drop; }
}
