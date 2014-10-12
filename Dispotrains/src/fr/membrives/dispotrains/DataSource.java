package fr.membrives.dispotrains;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import fr.membrives.dispotrains.data.Elevator;
import fr.membrives.dispotrains.data.Line;
import fr.membrives.dispotrains.data.Station;

/**
 * Created by etienne on 04/10/14.
 */
public class DataSource {
    private final DatabaseHelper mHelper;
    private SQLiteDatabase mDatabase;

    public DataSource(Context context) {
        this.mHelper = new DatabaseHelper(context);
    }

    public void open() throws SQLException {
        mDatabase = mHelper.getWritableDatabase();
    }

    public void close() {
        mHelper.close();
    }

    private Line cursorToLine(Cursor cursor) {
        String network = cursor.getString(0);
        String id = cursor.getString(1);
        return new Line(id, network);
    }

    private Station cursorToStation(Cursor cursor) {
        String name = cursor.getString(0);
        String display = cursor.getString(1);
        boolean working = cursor.getInt(2) != 0;
        return new Station(name, display, working);
    }

    private Elevator cursorToElevator(Cursor cursor) {
        String id = cursor.getString(0);
        String situation = cursor.getString(1);
        String direction = cursor.getString(2);
        String description = cursor.getString(3);
        Date time = new Date(cursor.getLong(4));
        return new Elevator(id, situation, direction, description, time);
    }

    public Set<Line> getAllLines() {
        Set<Line> lines = new HashSet<Line>();

        Cursor cursor = mHelper.getAllLines();

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Line line = cursorToLine(cursor);
            lines.add(line);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return lines;
    }

    public Set<Station> getStationsPerLine(Line line) {
        Cursor cursor = mHelper.getStations(line.getId());

        Set<Station> stations = new HashSet<Station>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Station station = cursorToStation(cursor);
            stations.add(station);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return stations;
    }

    public Set<Elevator> getElevatorsPerStation(Station station) {
        Cursor cursor = mDatabase.query(DatabaseHelper.TABLE_LINES, new String[] {
                DatabaseHelper.COLUMN_ELEVATOR_ID, DatabaseHelper.COLUMN_ELEVATOR_SITUATION,
                DatabaseHelper.COLUMN_ELEVATOR_DIRECTION,
                DatabaseHelper.COLUMN_ELEVATOR_STATUS_DESC,
                DatabaseHelper.COLUMN_ELEVATOR_STATUS_TIME },
                new StringBuilder().append(DatabaseHelper.COLUMN_ELEVATOR_STATION).append("=?")
                        .toString(), new String[] { station.getName() }, null, null, null);

        Set<Elevator> elevators = new HashSet<Elevator>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            Elevator elevator = cursorToElevator(cursor);
            elevators.add(elevator);
            cursor.moveToNext();
        }
        // make sure to close the cursor
        cursor.close();
        return elevators;
    }

    public boolean addLineToDatabase(Line line) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_LINE_ID, line.getId());
        values.put(DatabaseHelper.COLUMN_LINE_NETWORK, line.getNetwork());
        return mDatabase.insertWithOnConflict(DatabaseHelper.TABLE_LINES, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public boolean deleteLineFromDatabase(Line line) {
        return mDatabase.delete(DatabaseHelper.TABLE_LINES, DatabaseHelper.COLUMN_LINE_ID + " =?",
                new String[] { line.getId() }) != 0;
    }

    public boolean addStationToDatabase(Station station) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_STATION_NAME, station.getName());
        values.put(DatabaseHelper.COLUMN_STATION_DISPLAY, station.getDisplay());
        values.put(DatabaseHelper.COLUMN_STATION_WORKING, station.getWorking() ? 1 : 0);
        if (mDatabase.insertWithOnConflict(DatabaseHelper.TABLE_STATIONS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) == -1) {
            return false;
        }
        for (Line line : station.getLines()) {
            values = new ContentValues();
            values.put(DatabaseHelper.COLUMN_STATION_NAME, station.getName());
            values.put(DatabaseHelper.COLUMN_LINE_ID, line.getId());
            if (mDatabase.insertWithOnConflict(DatabaseHelper.TABLE_LINES_STATIONS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE) == -1) {
                return false;
            }
        }
        return true;
    }

    public boolean deleteStationFromDatabase(Station station) {
        mDatabase.delete(DatabaseHelper.TABLE_LINES_STATIONS, DatabaseHelper.COLUMN_STATION_NAME
                + " =?", new String[] { station.getName() });
        mDatabase.delete(DatabaseHelper.TABLE_STATIONS, DatabaseHelper.COLUMN_STATION_NAME + " =?",
                new String[] { station.getName() });
        return true;
    }

    public boolean addElevatorToDatabase(Elevator elevator) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_ELEVATOR_ID, elevator.getId());
        values.put(DatabaseHelper.COLUMN_ELEVATOR_DIRECTION, elevator.getDirection());
        values.put(DatabaseHelper.COLUMN_ELEVATOR_SITUATION, elevator.getSituation());
        values.put(DatabaseHelper.COLUMN_ELEVATOR_STATUS_DESC, elevator.getStatusDescription());
        values.put(DatabaseHelper.COLUMN_ELEVATOR_STATUS_TIME, elevator.getStatusDate().getTime());
        values.put(DatabaseHelper.COLUMN_ELEVATOR_STATION, elevator.getStation().getName());
        if (mDatabase.insertWithOnConflict(DatabaseHelper.TABLE_ELEVATORS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE) == -1) {
            return false;
        }
        return true;
    }

    public boolean deleteElevatorFromDatabase(Elevator elevator) {
        return mDatabase.delete(DatabaseHelper.TABLE_ELEVATORS, DatabaseHelper.COLUMN_ELEVATOR_ID
                + " =?", new String[] { elevator.getId() }) != 0;
    }
}
