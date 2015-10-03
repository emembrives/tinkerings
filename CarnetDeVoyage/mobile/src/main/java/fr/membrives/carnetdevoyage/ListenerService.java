package fr.membrives.carnetdevoyage;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.firebase.client.Firebase;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import ai.wit.sdk.IWitListener;
import ai.wit.sdk.Wit;
import ai.wit.sdk.model.WitOutcome;

public class ListenerService extends WearableListenerService implements IWitListener {
    private static final long CONNECTION_TIME_OUT_MS = 3000;
    private static final String RECORDING_ENDPOINT = "/recording";
    private static final String TAG = "ListenerService";

    private String nodeId;
    private Wit mWit;
    private Firebase mRootRef;
    private Map<String, Date> mMessages = new HashMap<>();


    @Override
    public void onCreate() {
        super.onCreate();
        Firebase.setAndroidContext(this);
        String accessToken = "JAVD3COEZDLUAYK3ZPJ3AU5VW6M75FLD";
        mWit = new Wit(accessToken, this);
        mWit.enableContextLocation(this);
        mRootRef = new Firebase("https://incandescent-heat-5800.firebaseio.com/carnetdevoyage");
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GMS connected");
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals(RECORDING_ENDPOINT)) {
            nodeId = messageEvent.getSourceNodeId();
            String message = new String(messageEvent.getData());
            Date date = new Date();
            mMessages.put(message, date);
            writeMessageToFirebase(date, message);
            mWit.captureTextIntent(message);
        }
    }

    private void writeMessageToFirebase(Date date, String message) {
        TimeZone tz = TimeZone.getTimeZone("Europe/Paris");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE);
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.FRANCE);
        dateFormat.setTimeZone(tz);
        timeFormat.setTimeZone(tz);
        String dateIso = dateFormat.format(date);
        String timeIso = timeFormat.format(date);

        Firebase messageRef = mRootRef.child("records").child(dateIso).child(timeIso);
        messageRef.child("record").setValue(message);
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

    private void reply(String message) {
        GoogleApiClient client = new GoogleApiClient.Builder(this).addApi(Wearable.API).build();
        client.blockingConnect(CONNECTION_TIME_OUT_MS, TimeUnit.MILLISECONDS);
        Wearable.MessageApi.sendMessage(client, nodeId, message, null);
        client.disconnect();
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
}
