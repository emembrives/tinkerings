package fr.membrives.dispotrains;

/**
 * A line
 */
public class Line {
    private final String id;
    private final String network;

    public Line(String id, String network) {
        this.id = id;
        this.network = network;
    }

    public String getNetwork() {
        return network;
    }

    public String getId() {
        return id;
    }
}
