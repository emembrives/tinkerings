package fr.membrives.dispotrains.data;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by etienne on 04/10/14.
 */
public class Station {
    private final String name;
    private final String display;
    private final boolean working;
    private final Set<Line> lines;
    private final Set<Elevator> elevators;

    public Station(String name, String display, boolean working) {
        this.name = name;
        this.display = display;
        this.working = working;
        this.lines = new HashSet<Line>();
        this.elevators = new HashSet<Elevator>();
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }

    public boolean getWorking() {
        return working;
    }

    public void addToLine(Line line) {
        lines.add(line);
        line.addStation(this);
    }

    public void addElevator(Elevator elevator) {
        elevators.add(elevator);
        elevator.setStation(this);
    }

    public Set<Elevator> getElevators() {
        return elevators;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((display == null) ? 0 : display.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + (working ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Station other = (Station) obj;
        if (display == null) {
            if (other.display != null)
                return false;
        } else if (!display.equals(other.display))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (working != other.working)
            return false;
        return true;
    }

    public Set<Line> getLines() {
        return lines;
    }
}
