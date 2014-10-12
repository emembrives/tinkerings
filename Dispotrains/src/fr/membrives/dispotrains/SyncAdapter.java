package fr.membrives.dispotrains;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.joda.time.format.ISODateTimeFormat;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;
import android.util.Log;
import fr.membrives.dispotrains.data.Elevator;
import fr.membrives.dispotrains.data.Line;
import fr.membrives.dispotrains.data.Station;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final String TAG = "f.m.d.SyncAdapter";
    // Global variables
    // Define a variable to contain a content resolver instance
    private final ContentResolver mContentResolver;
    private final DataSource mSource;

    /**
     * Set up the sync adapter
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        /*
         * If your app uses a content resolver, get an instance of it from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mSource = new DataSource(context);
    }

    /**
     * Set up the sync adapter. This form of the constructor maintains compatibility with Android
     * 3.0 and later platform versions
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        /*
         * If your app uses a content resolver, get an instance of it from the incoming Context
         */
        mContentResolver = context.getContentResolver();
        mSource = new DataSource(context);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        Log.d(TAG, "onPerformSync");
        mSource.open();
        Set<Line> lines = new HashSet<Line>();
        try {
            URL dispotrains = new URL("http://dispotrains.membrives.fr/app/GetLines/");
            InputStream linesData = dispotrains.openStream();
            JSONArray jsonLineList = new JSONArray(IOUtils.toString(linesData));
            Log.d(TAG, "Lines downloaded");
            for (int i = 0; i < jsonLineList.length(); i++) {
                JSONObject jsonLine = jsonLineList.getJSONObject(i);
                Line line = processLine(jsonLine);
                for (Station station : line.getStations()) {
                    URL stationURL = new URL("http://dispotrains.membrives.fr/app/GetStation/"
                            + URLEncoder.encode(station.getName(), "UTF-8").replace("+", "%20")
                                    .replace("%2F", "/") + "/");
                    Log.d(TAG,
                            "Station downloaded: " + station.getName() + " from "
                                    + stationURL.getPath());
                    InputStream stationData = stationURL.openStream();
                    JSONObject jsonStation = new JSONObject(IOUtils.toString(stationData));
                    processElevators(station, jsonStation);
                }
                lines.add(line);
            }
        } catch (MalformedURLException e) {
            Log.e(TAG, "Unable to sync", e);
            return;
        } catch (IOException e) {
            Log.e(TAG, "Unable to sync", e);
            return;
        } catch (JSONException e) {
            Log.e(TAG, "Unable to sync", e);
            return;
        }

        synchronizeWithDatabase(lines, syncResult);
    }

    private void synchronizeWithDatabase(Set<Line> lines, SyncResult syncResult) {
        Log.d(TAG, "synchronizeWithDatabase");
        Set<Line> oldLines = mSource.getAllLines();
        int nbLines = 0, nbStations = 0, nbElevators = 0;
        for (Line line : lines) {
            nbLines++;
            Map<Station, Station> oldStations = new HashMap<Station, Station>();
            if (!oldLines.contains(line)) {
                mSource.addLineToDatabase(line);
                syncResult.stats.numInserts++;
                syncResult.stats.numEntries++;
            } else {
                for (Station station : mSource.getStationsPerLine(line)) {
                    oldStations.put(station, station);
                }
                oldLines.remove(line);
            }
            for (Station station : line.getStations()) {
                nbStations++;
                Map<Elevator, Elevator> oldElevators = new HashMap<Elevator, Elevator>();
                if (!oldStations.containsKey(station)) {
                    mSource.addStationToDatabase(station);
                    syncResult.stats.numInserts++;
                    syncResult.stats.numEntries++;
                } else {
                    if (station.getWorking() != oldStations.get(station).getWorking()) {
                        mSource.addStationToDatabase(station);
                        syncResult.stats.numUpdates++;
                        syncResult.stats.numEntries++;
                    }
                    for (Elevator oldElevator : mSource.getElevatorsPerStation(station)) {
                        oldElevators.put(oldElevator, oldElevator);
                    }
                    oldStations.remove(station);
                }
                for (Elevator elevator : station.getElevators()) {
                    nbElevators++;
                    if (!oldElevators.containsKey(elevator)) {
                        mSource.addElevatorToDatabase(elevator);
                        syncResult.stats.numInserts++;
                        syncResult.stats.numEntries++;
                    } else {
                        if (!elevator.getStatusDate().equals(
                                oldElevators.get(elevator).getStatusDate())) {
                            // Elevator status has been updated
                            mSource.addElevatorToDatabase(elevator);
                            syncResult.stats.numUpdates++;
                            syncResult.stats.numEntries++;
                        }
                        oldElevators.remove(elevator);
                    }
                }
                for (Elevator elevator : oldElevators.keySet()) {
                    mSource.deleteElevatorFromDatabase(elevator);
                    syncResult.stats.numDeletes++;
                    syncResult.stats.numEntries++;
                }
            }
            for (Station station : oldStations.keySet()) {
                mSource.deleteStationFromDatabase(station);
                syncResult.stats.numDeletes++;
                syncResult.stats.numEntries++;
            }
        }
        for (Line line : oldLines) {
            mSource.deleteLineFromDatabase(line);
            syncResult.stats.numDeletes++;
            syncResult.stats.numEntries++;
        }
        Log.d(TAG, new StringBuilder("lines, stations, elevators: ").append(nbLines).append(" ")
                .append(nbStations).append(" ").append(nbElevators).toString());
        Log.d(TAG, new StringBuilder("SyncStats: ").append(syncResult.stats.numEntries).append(" ")
                .append(syncResult.stats.numInserts).append(" ")
                .append(syncResult.stats.numUpdates).append(" ")
                .append(syncResult.stats.numDeletes).toString());
    }

    private Line processLine(JSONObject jsonLine) throws JSONException {
        Line line = new Line(jsonLine.getString("id"), jsonLine.getString("network"));

        JSONArray badStations = jsonLine.getJSONArray("badStations");
        for (int j = 0; j < badStations.length(); j++) {
            JSONObject jsonStation = badStations.getJSONObject(j);
            Station station = new Station(jsonStation.getString("name"),
                    jsonStation.getString("displayname"), false);
            station.addToLine(line);
        }

        JSONArray goodStations = jsonLine.getJSONArray("goodStations");
        for (int j = 0; j < badStations.length(); j++) {
            JSONObject jsonStation = goodStations.getJSONObject(j);
            Station station = new Station(jsonStation.getString("name"),
                    jsonStation.getString("displayname"), true);
            station.addToLine(line);
        }
        return line;
    }

    private void processElevators(Station station, JSONObject jsonStation) throws JSONException {
        if (!jsonStation.getBoolean("haselevators")) {
            return;
        }
        JSONArray elevators = jsonStation.getJSONArray("elevators");
        for (int i = 0; i < elevators.length(); i++) {
            JSONObject jsonElevator = elevators.getJSONObject(i);
            JSONObject jsonStatus = jsonElevator.getJSONObject("status");
            Date lastUpdate = ISODateTimeFormat.dateTimeParser()
                    .parseDateTime(jsonStatus.getString("lastupdate")).toDate();
            Elevator elevator = new Elevator(jsonElevator.getString("id"),
                    jsonElevator.getString("situation"), jsonElevator.getString("direction"),
                    jsonStatus.getString("state"), lastUpdate);
            station.addElevator(elevator);
        }
    }
}
