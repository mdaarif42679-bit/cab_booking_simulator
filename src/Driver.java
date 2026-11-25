/** A cab driver with a current location, availability and the rider currently being served. */
public class Driver {
    private final String id;
    private String location;
    private boolean available = true;
    private Rider currentRider;

    public Driver(String id, String location) {
        this.id = id;
        this.location = location;
    }

    public String getId() { return id; }
    public String getLocation() { return location; }
    public boolean isAvailable() { return available; }
    public Rider getCurrentRider() { return currentRider; }

    /** Marks the driver busy with the given rider. */
    public void assign(Rider rider) {
        this.currentRider = rider;
        this.available = false;
    }

    /** Moves the driver to the drop location and makes the driver available again. */
    public void finishTrip() {
        if (currentRider != null) {
            this.location = currentRider.getDrop();
        }
        this.currentRider = null;
        this.available = true;
    }
}
