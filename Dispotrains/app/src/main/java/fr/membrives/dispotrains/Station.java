package fr.membrives.dispotrains;

/**
 * Created by etienne on 04/10/14.
 */
public class Station {
    private final String name;
    private final String display;
    private final boolean hasProblem;

    public Station(String name, String display, boolean hasProblem) {
        this.name = name;
        this.display = display;
        this.hasProblem = hasProblem;
    }

    public String getName() {
        return name;
    }

    public String getDisplay() {
        return display;
    }

    public boolean getHasProblem() {
        return hasProblem;
    }
}
