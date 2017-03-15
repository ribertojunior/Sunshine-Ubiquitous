package com.example.android.sunshine;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallbacks;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Junior on 15/03/2017.
 * Class to handle message sending
 */

public class TodayWeatherSender implements GoogleApiClient.ConnectionCallbacks {

    private String mNodeId;
    private String mMessage;
    private GoogleApiClient mClient;

    public static final String TAG = TodayWeatherSender.class.getSimpleName();

    private GoogleApiClient getGoogleApiClient(Context context) {
        return new GoogleApiClient.Builder(context)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    public TodayWeatherSender(Context mContext, String mMessage) {
        this.mMessage = mMessage;
        mClient = getGoogleApiClient(mContext);

    }

    public boolean connectAndSend(){
        mClient.connect();
        if (mClient.isConnected()) {
            Log.d(TAG, "TodayWeatherSender: Successful.");
            return true;
        }
        else {
            Log.d(TAG, "TodayWeatherSender: Failed.");
            return false;
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected: Connected.");
        retrieveDeviceNode();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }
    private void retrieveDeviceNode() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Wearable.NodeApi.getConnectedNodes(mClient).setResultCallback(new ResultCallbacks<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onSuccess(@NonNull NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (BuildConfig.DEBUG){
                            Log.d(TAG, "onSuccess: Success.");
                        }
                        mNodeId = getConnectedNodesResult.getNodes().get(0).getId();
                        sendMessage();
                    }

                    @Override
                    public void onFailure(@NonNull Status status) {
                        if (BuildConfig.DEBUG){
                            Log.d(TAG, "onFailure: Failed.");
                        }
                    }
                });
            }
        }).start();
    }
    private void sendMessage() {
        if (mNodeId != null) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, "sendMessage - Sending: "+mMessage);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Wearable.MessageApi.sendMessage(mClient, mNodeId, mMessage, null).setResultCallback(new ResultCallbacks<MessageApi.SendMessageResult>() {
                        @Override
                        public void onSuccess(@NonNull MessageApi.SendMessageResult sendMessageResult) {
                            if (BuildConfig.DEBUG)
                                Log.d(TAG, "onSuccess - sendMessage: "+sendMessageResult.getRequestId());
                        }

                        @Override
                        public void onFailure(@NonNull Status status) {
                            if (BuildConfig.DEBUG){
                                Log.d(TAG, "onFailure - sendMessage: " + status.getStatusMessage());
                            }

                        }
                    });
                }
            }).start();
        }
    }

}
