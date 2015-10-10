package fr.membrives.carnetdevoyage;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.JsonElement;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;
import ai.wit.sdk.model.WitOutcome;

public class ListenerService extends WearableListenerService
        implements IWitListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {
    private static final long CONNECTION_TIME_OUT_MS = 3000;
    // Used for voice messages (free text).
    private static final String RECORDING_ENDPOINT = "/recording";
    // Used for non-voice messages (start/stop).
    private static final String AUTO_RECORDING_ENDPOINT = "/recording/auto";
    private static final String TAG = "ListenerService";

    private String nodeId;
    private Wit mWit;
    private Firebase mRootRef;
    private GoogleApiClient mGoogleApiClient;
    private Map<String, Date> mMessages = new HashMap<>();


    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
        String accessToken = "JAVD3COEZDLUAYK3ZPJ3AU5VW6M75FLD";
        mWit = new Wit(accessToken, this);
        mWit.enableContextLocation(this);
        mRootRef = new Firebase("https://incandescent-heat-5800.firebaseio.com/carnetdevoyage");
        mGoogleApiClient = getGoogleApiClient(this);
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        GoogleApiClient client = new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(LocationServices.API).build();
        client.connect();
        return client;
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(RECORDING_ENDPOINT)) {
            nodeId = messageEvent.getSourceNodeId();
            Location lastLocation =
                    LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            String message = new String(messageEvent.getData());
            Date date = new Date();
            mMessages.put(message, date);
            writeMessageToFirebase(date, message, lastLocation);
            mWit.captureTextIntent(message);
        } else if (messageEvent.getPath().equals(AUTO_RECORDING_ENDPOINT)) {
            nodeId = messageEvent.getSourceNodeId();
            Location lastLocation = null;
            synchronized (mGoogleApiClient) {
                if (mGoogleApiClient.isConnecting()) {
                    try {
                        mGoogleApiClient.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Interrupted while waiting for GoogleApiClient", e);
                        return;
                    }
                }
                if (mGoogleApiClient.isConnected()) {
                    lastLocation =
                            LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                }
            }
            String message = new String(messageEvent.getData());
            Date date = new Date();
            writeMessageToFirebase(date, message, lastLocation);
        }
    }

    private void writeMessageToFirebase(Date date, String message, Location lastLocation) {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE);
        dateFormat.setTimeZone(tz);
        timeFormat.setTimeZone(tz);
        String dateIso = dateFormat.format(date);
        String timeIso = timeFormat.format(date);

        Firebase messageRef = mRootRef.child("records").child(dateIso).child(timeIso);
        messageRef.child("record").setValue(message);
        if (lastLocation != null) {
            messageRef.child("location").child("latitude").setValue(lastLocation.getLatitude());
            messageRef.child("location").child("longitude").setValue(lastLocation.getLongitude());
        }
    }

    private void writeWitToFirebase(Date date, List<WitOutcome> outcomes) {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat dtFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.FRANCE);
        dtFormat.setTimeZone(tz);
        String dtIso = dtFormat.format(date);

        Firebase intentRef = mRootRef.child("intents").child(dtIso).child("detected");
        for (WitOutcome outcome : outcomes) {
            Firebase outcomeRef = intentRef.push();
            outcomeRef.child("intent").setValue(outcome.get_intent());
            outcomeRef.child("confidence").setValue(outcome.get_confidence());
            outcomeRef.child("text").setValue(outcome.get_text());
            Firebase entityRef = outcomeRef.child("entities");
            for (Map.Entry<String, JsonElement> entry : outcome.get_entities().entrySet()) {
                entityRef.child(entry.getKey()).setValue(entry.getValue().toString());
            }
        }
    }

    @Override
    public void witDidGraspIntent(ArrayList<WitOutcome> arrayList, String s, Error error) {
        if (error != null) {
            Log.e(TAG, "Error while processing input: " + error.getLocalizedMessage());
        }
        if (arrayList.isEmpty()) {
            return;
        }
        String message = arrayList.get(0).get_text();
        writeWitToFirebase(mMessages.get(message), arrayList);
        mMessages.remove(mMessages);
    }

    @Override
    public void witDidStartListening() {

    }

    @Override
    public void witDidStopListening() {

    }

    @Override
    public void witActivityDetectorStarted() {

    }

    @Override
    public String witGenerateMessageId() {
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        synchronized (mGoogleApiClient) {
            mGoogleApiClient.notifyAll();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        synchronized (mGoogleApiClient) {
            mGoogleApiClient.notifyAll();
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        synchronized (mGoogleApiClient) {
            mGoogleApiClient.notifyAll();
        }
    }
}
