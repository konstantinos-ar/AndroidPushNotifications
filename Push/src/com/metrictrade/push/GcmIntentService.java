package com.metrictrade.push;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.gcm.GoogleCloudMessaging;

public class GcmIntentService extends IntentService {

	public static int NOTIFICATION_ID;
	private static final String TAG = "GCMIntentService";
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;
    private Context context;
    
    //String mes;
    //private Handler handler;

    public GcmIntentService()
    {
        super("GcmIntentService");
    }
    
    @Override
    public void onCreate()
    {
        // TODO Auto-generated method stub
        super.onCreate();
        //handler = new Handler();
        context = getApplicationContext();
    }

    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty())
        {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */
            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType))
            {
                sendNotification("Send error: " + extras.toString(), null);
            }
            else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType))
            {
                sendNotification("Deleted messages on server: " +
                        extras.toString(), null);
            // If it's a regular GCM message, do some work.
            }
            else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType))
            {
                // This loop represents the service doing some work.
                for (int i=0; i<5; i++)
                {
                    Log.i(TAG, "Working... " + (i+1)
                            + "/5 @ " + SystemClock.elapsedRealtime());
                    try
                    {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
                //mes = extras.getString("title");
                //showToast(extras.getString("title"));
                //Log.i("GCM", "Received : (" +messageType+")  "+extras.getString("title"));
                Log.i(TAG, "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                sendNotification(extras.getString("title"), extras.getString("message"));
                Log.i(TAG, "Received: " + extras.toString());
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String title, String msg)
    {
    	final SharedPreferences prefs = getGCMPreferences(context);
    	SharedPreferences.Editor editor = prefs.edit();
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.drawable.logo_spec)
        .setSound(Settings.System.DEFAULT_NOTIFICATION_URI)
        .setContentTitle(title)
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        NOTIFICATION_ID = getNotificationId(context);
        editor.putInt("notification_id", NOTIFICATION_ID+1);
        editor.commit();
        Log.i(TAG, "Current NOTIFICATION_ID is: " + getNotificationId(context));
        
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
    
/*    public void showToast(final String mes)
    {
        handler.post(new Runnable()
        {
            public void run()
            {
                Toast.makeText(getApplicationContext(),mes , Toast.LENGTH_LONG).show();
            }
         });
    }
  */  
    private SharedPreferences getGCMPreferences(Context context)
	{
		return getSharedPreferences(MainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}
    
    private int getNotificationId(Context context)
    {
    	final SharedPreferences prefs = getGCMPreferences(context);
	    int notificationId = prefs.getInt("notification_id", 0);
	    return notificationId;
    }
	
}
