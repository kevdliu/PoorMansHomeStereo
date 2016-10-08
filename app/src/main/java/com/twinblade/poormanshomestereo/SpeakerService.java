package com.twinblade.poormanshomestereo;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.view.ViewCompat;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class SpeakerService extends Service {
    private SpeakerServer mSpeakerServer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        init(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        init(intent);
        return START_STICKY;
    }

    private void init(Intent intent) {
        if ("STOP_SERVICE".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            mSpeakerServer.stop();
        } else {
            try {
                mSpeakerServer = new SpeakerService.SpeakerServer();
                mSpeakerServer.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_queue)
                    .setContentTitle("My notification") // TODO: Song name?
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentText("Click to end");

            Intent intentStopService = new Intent(this, SpeakerService.class);
            intentStopService.setAction("STOP_SERVICE");

            PendingIntent pStopSelf =
                    PendingIntent.getService(
                            this,
                            0,
                            intentStopService,
                            PendingIntent.FLAG_CANCEL_CURRENT
                    );
            mBuilder.setContentIntent(pStopSelf);
            mBuilder.addAction(R.mipmap.ic_search, "Stop", pStopSelf); // Not sure if needed
            startForeground(Constants.SPEAKER_NOTIFICATION_ID, mBuilder.build());
        }
    }

    @Override
    public void onCreate() {

    }

    public class SpeakerServer extends NanoHTTPD {
        public SpeakerServer() {
            super(Constants.SPEAKER_SERVER_PORT);
        }

        @Override
        public Response serve(IHTTPSession session) {
            Map<String, String> files = new HashMap<String, String>();

            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return newFixedLengthResponse("Oops, IO Exception....");
            } catch (ResponseException e) {
               return newFixedLengthResponse("Response exception...");
            }
            String postBody = session.getQueryParameterString();
            String command = files.get(Constants.SPEAKER_COMMAND);

            return newFixedLengthResponse(command);
        }

    }
}
