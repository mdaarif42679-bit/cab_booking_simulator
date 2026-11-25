# Cab Booking Simulator (PS No. 86) - Graphical Edition

Data Structures and Algorithms project: **Graph (adjacency list), Dijkstra's algorithm, PriorityQueue (min-heap), FIFO Queue.**
Standard Java only (Swing). Tested with JDK 21; the code uses only Java 8-level language features.

The original console program is **unchanged and still works**. The Swing dashboard is a new layer on top of the
same simulation logic (`CabSimulatorManager`).

## Run on Windows (3 steps)
1. Install a **JDK** (not only a JRE). Check in Command Prompt: `javac -version`
2. Unzip the project.
3. **Double-click `run.bat`**. It compiles the code and opens the dashboard.

| File | What it does |
|---|---|
| `run.bat` | Compile + open the **graphical dashboard** |
| `run-console.bat` | Original console menu version |
| `compile.bat` | Compile only |
| `test.bat` | Compile + run all tests (console tests and GUI tests) |

Linux/macOS: `./run.sh`, `./test.sh`.

## How to use the dashboard
* **Book a cab:** choose *Pickup* and *Drop* (or click two locations on the map), watch the route preview, press **Book Cab**.
  The nearest free driver (by road distance, Dijkstra + min-heap) is assigned. If both drivers are busy, the rider joins the FIFO waiting queue.
* **Complete a trip:** choose the busy driver under *Complete Trip* and press **Complete Trip**. The cab drives along the
  route to the drop location, becomes available, and the waiting queue is served automatically (front first).
* **Run Demo:** one click replays **Test Case 4** from the presentation (B-C, B-D, A-C, then D1 completes).
* **Reset:** starts a fresh simulation (D1 at A, D2 at D, empty queue).
* Click a **driver** (on the map or the Driver Dashboard) to focus that trip; hover nodes and cars for details.

## Panels
* **Live City Map** - roads and distances are drawn from the real `Graph`; cars are drawn at the drivers' real locations;
  dashed line = driver to pickup, solid line = passenger trip, green PICKUP / red DROP tags.
* **Driver Dashboard** - ID, location, Available/Busy, assigned rider, pickup distance, destination.
* **Live Trip Information** - rider, driver, driver-to-pickup distance, shortest trip distance, route, fare (Rs. 15 per km).
* **Waiting Queue (FIFO)**, **Booking History**, **Simulator Activity Log** (the real console messages of the simulator).
All panels read the same simulator state, so they always agree.

## Expected results from the presentation (verified by `test.bat`)
| Case | Result |
|---|---|
| B to C | D1 (2 km away), trip 3 km, Rs. 45 |
| A to B | D1 (0 km away), trip 2 km, Rs. 30 |
| Z to C | Location Z not found |
| B-C, B-D, A-C, D1 completes | D1 Rs. 45, D2 7 km Rs. 105, Rider 3 queued, then D1 (5 km) Rs. 75 |

## Project structure
```
src/
  Road, Graph, Driver, Rider, TripRecord, CabSimulatorManager, Main, TestRunner   original logic + console (unchanged*)
  SimulationController   bridge: GUI -> real simulator (adds no logic)
  GuiMain                entry point of the GUI
  CabDashboardFrame      window;   CabDashboardPanel   all dashboard panels
  MapPanel               custom-painted city map (Java2D)
  UiKit, Theme           custom dark-theme components, colours, fonts
  GuiTestRunner          tests of the GUI layer
```
\* Only change to existing logic: `Graph.shortestPath()` was **added** (returns the route, not only distances) so the map can
draw routes. `shortestDistances()` and everything else are untouched.

## Notes
* The car animation plays only when a trip is completed (the car drives pickup -> drop, ending at the real new location).
  Booking never moves a car artificially: positions always equal the simulator state.
* In-memory only (no database), as in the original project.
