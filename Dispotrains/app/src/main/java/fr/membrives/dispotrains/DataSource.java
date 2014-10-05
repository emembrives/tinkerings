package fr.membrives.dispotrains;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

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
        boolean hasProblem = cursor.getInt(2) != 0;
        return new Station(name, display, hasProblem);
    }

    private Elevator cursorToElevator(Cursor cursor) {
        String id = cursor.getString(0);
        String situation = cursor.getString(1);
        String direction = cursor.getString(2);
        String description = cursor.getString(3);
        Date time = new Date(cursor.getLong(4));
        return new Elevator(id, situation, direction, description, time);
    }

    List<Line> getAllLines() {
        List<Line> lines = new ArrayList<Line>();

        Cursor cursor = mDatabase.query(DatabaseHelper.TABLE_LINES, new String[] {
                DatabaseHelper.COLUMN_LINE_NETWORK, DatabaseHelper.COLUMN_LINE_ID }, null, null,
                null, null, null);

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

    List<Station> getStationsPerLine(Line line) {
        String rawQuery = new StringBuilder().append("SELECT ")
                .append(DatabaseHelper.COLUMN_STATION_NAME).append(", ")
                .append(DatabaseHelper.COLUMN_STATION_DISPLAY).append(" from ")
                .append(DatabaseHelper.TABLE_STATIONS).append(" INNER JOIN ")
                .append(DatabaseHelper.TABLE_LINES_STATIONS).append(" ON ")
                .append(DatabaseHelper.TABLE_STATIONS).append(".")
                .append(DatabaseHelper.COLUMN_STATION_NAME).append("=")
                .append(DatabaseHelper.TABLE_LINES_STATIONS).append(".")
                .append(DatabaseHelper.COLUMN_STATION_NAME).append(" WHERE ")
                .append(DatabaseHelper.TABLE_LINES_STATIONS).append(".")
                .append(DatabaseHelper.COLUMN_LINE_ID).append("=?").toString();
        Cursor cursor = mDatabase.rawQuery(rawQuery, new String[] { line.getId() });

        List<Station> stations = new ArrayList<Station>();
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

    List<Elevator> getElevatorsPerStation(Station station) {
        Cursor cursor = mDatabase.query(DatabaseHelper.TABLE_LINES, new String[] {
                DatabaseHelper.COLUMN_ELEVATOR_ID, DatabaseHelper.COLUMN_ELEVATOR_SITUATION,
                DatabaseHelper.COLUMN_ELEVATOR_DIRECTION,
                DatabaseHelper.COLUMN_ELEVATOR_STATUS_DESC,
                DatabaseHelper.COLUMN_ELEVATOR_STATUS_TIME },
                new StringBuilder().append(DatabaseHelper.COLUMN_ELEVATOR_STATION).append("=?")
                        .toString(), new String[] { station.getName() }, null, null, null);

        List<Elevator> elevators = new ArrayList<Elevator>();
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
}
