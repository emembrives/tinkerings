package fr.membrives.dispotrains;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by etienne on 04/10/14.
 */
public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLE_LINES = "lines";
    public static final String TABLE_LINES_STATIONS = "line_stations";
    public static final String TABLE_STATIONS = "stations";
    public static final String TABLE_ELEVATORS = "elevators";

    public static final String COLUMN_LINE_NETWORK = "network";
    public static final String COLUMN_LINE_ID = "_id";

    public static final String COLUMN_STATION_NAME = "name";
    public static final String COLUMN_STATION_DISPLAY = "display";
    public static final String COLUMN_STATION_WORKING = "working";

    public static final String COLUMN_ELEVATOR_ID = "_id";
    public static final String COLUMN_ELEVATOR_SITUATION = "situation";
    public static final String COLUMN_ELEVATOR_DIRECTION = "direction";
    public static final String COLUMN_ELEVATOR_STATUS_DESC = "status_desc";
    public static final String COLUMN_ELEVATOR_STATUS_TIME = "status_time";
    public static final String COLUMN_ELEVATOR_STATION = "station";

    private static final String DATABASE_NAME = "dispotrains.db";
    private static final int DATABASE_VERSION = 1;

    // Database creation sql statement
    private static final String DATABASE_CREATE_LINE = "create table " + TABLE_LINES + "("
            + COLUMN_LINE_ID + " TEXT primary key, " + COLUMN_LINE_NETWORK + " TEXT not null);";
    private static final String DATABASE_CREATE_STATION = "create table " + TABLE_STATIONS + "("
            + COLUMN_STATION_NAME + " TEXT primary key, " + COLUMN_STATION_DISPLAY
            + " TEXT not null, " + COLUMN_STATION_WORKING + " INTEGER not null);";
    private static final String DATABASE_CREATE_LINE_STATIONS = "create table "
            + TABLE_LINES_STATIONS + "(" + COLUMN_LINE_ID + " TEXT not null, "
            + COLUMN_STATION_NAME + " TEXT not null, FOREIGN KEY(" + COLUMN_STATION_NAME
            + ") REFERENCES " + TABLE_STATIONS + "(" + COLUMN_STATION_NAME + "), FOREIGN KEY("
            + COLUMN_LINE_ID + ") REFERENCES " + TABLE_LINES + "(" + COLUMN_LINE_ID + "));";
    private static final String DATABASE_CREATE_ELEVATORS = "create table " + TABLE_ELEVATORS + "("
            + COLUMN_ELEVATOR_ID + " TEXT primary key, " + COLUMN_ELEVATOR_DIRECTION
            + " TEXT not null, " + COLUMN_ELEVATOR_SITUATION + " TEXT not null, "
            + COLUMN_ELEVATOR_STATUS_DESC + " TEXT, " + COLUMN_ELEVATOR_STATUS_TIME + " INTEGER, "
            + COLUMN_ELEVATOR_STATION + " TEXT not null, FOREIGN KEY(" + COLUMN_ELEVATOR_STATION
            + ") REFERENCES " + TABLE_STATIONS + "(" + COLUMN_STATION_NAME + ") );";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase database) {
        database.execSQL(DATABASE_CREATE_LINE);
        database.execSQL(DATABASE_CREATE_STATION);
        database.execSQL(DATABASE_CREATE_LINE_STATIONS);
        database.execSQL(DATABASE_CREATE_ELEVATORS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(DatabaseHelper.class.getName(), "Upgrading database from version " + oldVersion
                + " to " + newVersion + ", which will destroy all old data");
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ELEVATORS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINES_STATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_STATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_LINES);
        onCreate(db);
    }

    public Cursor getAllLines() {
        Cursor cursor = this.getReadableDatabase().query(DatabaseHelper.TABLE_LINES,
                new String[] { DatabaseHelper.COLUMN_LINE_NETWORK, DatabaseHelper.COLUMN_LINE_ID },
                null, null, null, null, null);
        return cursor;
    }

    public Cursor getStations(String lineId) {
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
        return getReadableDatabase().rawQuery(rawQuery, new String[] { lineId });
    }

    public Cursor getElevators(String stationName) {
        return getReadableDatabase().query(
                DatabaseHelper.TABLE_LINES,
                new String[] { DatabaseHelper.COLUMN_ELEVATOR_ID,
                        DatabaseHelper.COLUMN_ELEVATOR_SITUATION,
                        DatabaseHelper.COLUMN_ELEVATOR_DIRECTION,
                        DatabaseHelper.COLUMN_ELEVATOR_STATUS_DESC,
                        DatabaseHelper.COLUMN_ELEVATOR_STATUS_TIME },
                new StringBuilder().append(DatabaseHelper.COLUMN_ELEVATOR_STATION).append("=?")
                        .toString(), new String[] { stationName }, null, null, null);
    }
}