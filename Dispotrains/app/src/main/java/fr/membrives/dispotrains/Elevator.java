package fr.membrives.dispotrains;

import java.util.Date;

/**
 * Created by etienne on 04/10/14.
 */
public class Elevator {
    private final String id;
    private final String situation;
    private final String direction;
    private final String statusDescription;
    private final Date statusDate;

    public Elevator(String id, String situation, String direction, String statusDescription, Date statusDate) {
        this.id = id;
        this.situation = situation;
        this.direction = direction;
        this.statusDescription = statusDescription;
        this.statusDate = statusDate;
    }
}
