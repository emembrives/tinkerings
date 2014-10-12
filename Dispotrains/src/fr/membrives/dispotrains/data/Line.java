package fr.membrives.dispotrains.data;

import java.util.HashSet;
import java.util.Set;

/**
 * A line
 */
public class Line {
    private final String id;
    private final String network;
    private final Set<Station> stations;

    public Line(String id, String network) {
        this.id = id;
        this.network = network;
        this.stations = new HashSet<Station>();
    }

    public String getNetwork() {
        return network;
    }

    public String getId() {
        return id;
    }

    public void addStation(Station station) {
        stations.add(station);
    }

    public Set<Station> getStations() {
        return stations;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((network == null) ? 0 : network.hashCode());
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
        Line other = (Line) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (network == null) {
            if (other.network != null)
                return false;
        } else if (!network.equals(other.network))
            return false;
        return true;
    }
}
