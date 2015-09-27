package fr.membrives.carnetdevoyage;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sends messages to a connected handset.
 */
public class HandheldCommunicationService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final String TAG = "carnetdevoyage.HCS";
    private static final String ACTION_VOICE_INPUT =
            "fr.membrives.carnetdevoyage.action" + ".VOICE_INPUT";
    private static final String EXTRA_VOICE_INPUT =
            "fr.membrives.carnetdevoyage.extra" + ".VOICE_INPUT";

    private static final String RECORDING_ENDPOINT = "/recording";
    private static final String HANDHELD_CAPABILITY_NAME = "recording";

    private GoogleApiClient mGoogleApiClient;
    private AtomicReference<String> mNodeId = new AtomicReference<String>(null);

    public HandheldCommunicationService() {
        super("HandheldCommunicationService");
    }

    /**
     * Starts this service to send detected voice input to the handheld for processing.
     *
     * @see IntentService
     */
    public static void startSendVoiceInput(Context context, List<String> voiceInputs) {
        Intent intent = new Intent(context, HandheldCommunicationService.class);
        intent.setAction(ACTION_VOICE_INPUT);
        intent.putExtra(EXTRA_VOICE_INPUT, voiceInputs.get(0));
        context.startService(intent);
        Log.i(TAG, "startSendVoiceInput");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mGoogleApiClient = getGoogleApiClient(this);
        mGoogleApiClient.connect();
        return super.onStartCommand(intent, flags, startId);
    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context).addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).addApi(Wearable.API).build();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.i(TAG, "GMS connected");
        findHandheldNode();
    }

    private void findHandheldNode() {
        Wearable.CapabilityApi.getCapability(mGoogleApiClient, HANDHELD_CAPABILITY_NAME,
                CapabilityApi.FILTER_REACHABLE)
                .setResultCallback(new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult getCapabilityResult) {
                        Log.i(TAG, "Nodes found");
                        Set<Node> connectedNodes = getCapabilityResult.getCapability().getNodes();
                        String bestNode = pickBestNodeId(connectedNodes);
                        synchronized (mNodeId) {
                            mNodeId.set(bestNode);
                            if (mNodeId.get() != null) {
                                Log.i(TAG, "Messages may proceed");
                                mNodeId.notifyAll();
                            }
                        }
                    }
                });
    }

    private String pickBestNodeId(Set<Node> nodes) {
        String bestNodeId = null;
        // Find a nearby node or pick one arbitrarily
        for (Node node : nodes) {
            if (node.isNearby()) {
                return node.getId();
            }
            bestNodeId = node.getId();
        }
        return bestNodeId;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "GMS connection suspended");
        mNodeId.set(null);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(TAG, "GMS connection failed");
        mNodeId.set(null);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_VOICE_INPUT.equals(action)) {
                final String voiceInput = intent.getStringExtra(EXTRA_VOICE_INPUT);
                handleSendVoiceInput(voiceInput);
            } else {
                throw new UnsupportedOperationException("Unknown operation");
            }
        }
    }

    /**
     * Sends the voice input to the handheld device.
     */
    private void handleSendVoiceInput(String voiceInput) {
        synchronized (mNodeId) {
            if (mNodeId.get() == null) {
                Log.i(TAG, "Waiting for node");
                try {
                    mNodeId.wait(3000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Error while waiting for node ID.", e);
                }
            }
            Wearable.MessageApi.sendMessage(mGoogleApiClient, mNodeId.get(), RECORDING_ENDPOINT,
                    voiceInput.getBytes())
                    .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                            if (!sendMessageResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Unable to send message");
                            } else {
                                Log.i(TAG, "Message sent");
                            }
                        }
                    });
        }
    }

}
