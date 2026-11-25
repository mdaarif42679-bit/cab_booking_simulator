/** One entry of the trip history. */
public class TripRecord {
    public static final String IN_PROGRESS = "IN PROGRESS";
    public static final String COMPLETED = "COMPLETED";

    private final int riderId;
    private final String driverId;
    private final String pickup;
    private final String drop;
    private final int driverToPickupKm;
    private final int tripKm;
    private final int fare;
    private String status = IN_PROGRESS;

    public TripRecord(int riderId, String driverId, String pickup, String drop,
                      int driverToPickupKm, int tripKm, int fare) {
        this.riderId = riderId;
        this.driverId = driverId;
        this.pickup = pickup;
        this.drop = drop;
        this.driverToPickupKm = driverToPickupKm;
        this.tripKm = tripKm;
        this.fare = fare;
    }

    public int getRiderId() { return riderId; }
    public String getDriverId() { return driverId; }
    public String getPickup() { return pickup; }
    public String getDrop() { return drop; }
    public int getDriverToPickupKm() { return driverToPickupKm; }
    public int getTripKm() { return tripKm; }
    public int getFare() { return fare; }
    public String getStatus() { return status; }
    public void markCompleted() { this.status = COMPLETED; }
}
