/** One road entry in the adjacency list: the destination location and the distance in km. */
public class Road {
    private final String to;
    private final int km;

    public Road(String to, int km) {
        this.to = to;
        this.km = km;
    }

    public String getTo() { return to; }
    public int getKm() { return km; }
}
