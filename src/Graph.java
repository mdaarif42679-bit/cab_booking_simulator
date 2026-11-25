import java.util.*;

/** City road map stored as an adjacency list. Dijkstra's algorithm finds shortest road distances. */
public class Graph {
    private final Map<String, List<Road>> adjacency = new TreeMap<>();

    /** Adds a bidirectional road. */
    public void addRoad(String from, String to, int km) {
        adjacency.computeIfAbsent(from, k -> new ArrayList<>()).add(new Road(to, km));
        adjacency.computeIfAbsent(to, k -> new ArrayList<>()).add(new Road(from, km));
    }

    public boolean hasLocation(String name) {
        return adjacency.containsKey(name);
    }

    public Set<String> getLocations() {
        return Collections.unmodifiableSet(adjacency.keySet());
    }

    public List<Road> getRoads(String location) {
        return Collections.unmodifiableList(adjacency.getOrDefault(location, new ArrayList<>()));
    }

    /** Heap entry used by Dijkstra: a location and its tentative distance. */
    private static class NodeDistance implements Comparable<NodeDistance> {
        final String node;
        final int distance;

        NodeDistance(String node, int distance) {
            this.node = node;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeDistance other) {
            return Integer.compare(this.distance, other.distance);
        }
    }

    /** Dijkstra's algorithm (PriorityQueue min-heap): shortest distance from source to every location. */
    public Map<String, Integer> shortestDistances(String source) {
        Map<String, Integer> dist = new HashMap<>();
        for (String location : adjacency.keySet()) {
            dist.put(location, Integer.MAX_VALUE);
        }
        if (!adjacency.containsKey(source)) {
            return dist;
        }
        dist.put(source, 0);
        PriorityQueue<NodeDistance> heap = new PriorityQueue<>();
        heap.add(new NodeDistance(source, 0));

        while (!heap.isEmpty()) {
            NodeDistance current = heap.poll();
            if (current.distance > dist.get(current.node)) {
                continue; // outdated heap entry
            }
            for (Road road : adjacency.get(current.node)) {
                int newDistance = current.distance + road.getKm();
                if (newDistance < dist.get(road.getTo())) {
                    dist.put(road.getTo(), newDistance);
                    heap.add(new NodeDistance(road.getTo(), newDistance));
                }
            }
        }
        return dist;
    }

    /**
     * Dijkstra's algorithm with predecessor tracking: returns the shortest ROUTE (list of locations,
     * source first, target last). Used by the GUI to draw routes. Returns an empty list if unreachable.
     * (Added for the GUI; shortestDistances() above is unchanged.)
     */
    public List<String> shortestPath(String source, String target) {
        List<String> path = new ArrayList<>();
        if (!adjacency.containsKey(source) || !adjacency.containsKey(target)) {
            return path;
        }
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        for (String location : adjacency.keySet()) {
            dist.put(location, Integer.MAX_VALUE);
        }
        dist.put(source, 0);
        PriorityQueue<NodeDistance> heap = new PriorityQueue<>();
        heap.add(new NodeDistance(source, 0));
        while (!heap.isEmpty()) {
            NodeDistance current = heap.poll();
            if (current.distance > dist.get(current.node)) {
                continue;
            }
            for (Road road : adjacency.get(current.node)) {
                int newDistance = current.distance + road.getKm();
                if (newDistance < dist.get(road.getTo())) {
                    dist.put(road.getTo(), newDistance);
                    previous.put(road.getTo(), current.node);
                    heap.add(new NodeDistance(road.getTo(), newDistance));
                }
            }
        }
        if (dist.get(target) == Integer.MAX_VALUE) {
            return path;
        }
        for (String at = target; at != null; at = previous.get(at)) {
            path.add(0, at);
        }
        return path;
    }
}
