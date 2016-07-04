package com.metrictrade.push;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

public class MainActivity extends Activity
{
	public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_REG_NAME = "registration_name";
    //private static final String PROPERTY_REG_EMAIL = "registration_email";
	private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	private static final String TAG = "GoogleCloudMessaging";
	private final String SENDER_ID = "49203111182";
	
	TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;
    
    Connection conn = null;
    Statement st = null;
    
    HashMap<String, String> params = new HashMap<String, String>();
    
    NotificationManager NM;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mDisplay = (TextView) findViewById(R.id.display);
		
		context = getApplicationContext();
		
		if (checkPlayServices())
		{
			gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty())
            {
                registerInBackground();
            }
        }
		else
        {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
				
	}
	
	@Override
	protected void onResume()
	{
	    super.onResume();
	    checkPlayServices();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	
	private boolean checkPlayServices()
	{
	    int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
	    if (resultCode != ConnectionResult.SUCCESS)
	    {
	        if (GooglePlayServicesUtil.isUserRecoverableError(resultCode))
	        {
	            GooglePlayServicesUtil.getErrorDialog(resultCode, this,
	                    PLAY_SERVICES_RESOLUTION_REQUEST).show();
	        }
	        else
	        {
	            Log.i(TAG, "This device is not supported.");
	            finish();
	        }
	        return false;
	    }
	    return true;
	}
	
	private String getRegistrationId(Context context)
	{
	    final SharedPreferences prefs = getGCMPreferences(context);
	    String registrationId = prefs.getString(PROPERTY_REG_ID, "");
	    String registrationName = prefs.getString(PROPERTY_REG_NAME, "");
	    //AccountManager manager = (AccountManager) getSystemService(ACCOUNT_SERVICE);
	    //Account[] list = manager.getAccounts();
	    //String gmail = null;
	    
	    if (registrationId.isEmpty())
	    {
	        Log.i(TAG, "Registration not found.");
	        return "";
	    }
	    // Check if app was updated; if so, it must clear the registration ID
	    // since the existing regID is not guaranteed to work with the new
	    // app version.
	    int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
	    int currentVersion = getAppVersion(context);
	    if (registeredVersion != currentVersion)
	    {
	        Log.i(TAG, "App version changed.");
	        return "";
	    }

	    if (!registrationName.equals(prefs.getString("ZTrade_Username", "")))
	    {
	    	Log.i(TAG, "Username changed.");
	        return "";
	    }
	    return registrationId;
	}

	private SharedPreferences getGCMPreferences(Context context)
	{
		return getSharedPreferences(MainActivity.class.getSimpleName(),
				Context.MODE_PRIVATE);
	}

	private static int getAppVersion(Context context)
	{
		try
		{
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(), 0);
			return packageInfo.versionCode;
		}
		catch (NameNotFoundException e)
		{
			// should never happen
			throw new RuntimeException("Could not get package name: " + e);
		}
	}
	
	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and app versionCode in the application's
	 * shared preferences.
	 */
	private void registerInBackground()
	{
	    new AsyncTask<String, String, String>()
	    {
	        @Override
	        protected String doInBackground(String... params)
	        {
	            String msg = "";
	            try
	            {
	                if (gcm == null)
	                {
	                    gcm = GoogleCloudMessaging.getInstance(context);
	                }
	                regid = gcm.register(SENDER_ID);
	                msg = "Device registered, registration ID=" + regid;
	                
	                // Persist the regID - no need to register again.
	                storeRegistrationId(context, regid);

	                // You should send the registration ID to your server over HTTP,
	                // so it can use GCM/HTTP or CCS to send messages to your app.
	                // The request to your server should be authenticated if your app
	                // is using accounts.
	                sendRegistrationIdToBackend();

	            }
	            catch (IOException ex)
	            {
	                msg = "Error :" + ex.getMessage();
	                // If there is an error, don't just keep trying to register.
	                // Require the user to click a button again, or perform
	                // exponential back-off.
	            }
	            return msg;
	        }

	        protected void onPostExecute(String msg)
	        {
	            mDisplay.append(msg + "\n");
	        }
	    }.execute();

	}
	
	/**
	 * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
	 * or CCS to send messages to your app. Not needed for this demo since the
	 * device sends upstream messages to a server that echoes back the message
	 * using the 'from' address in the message.
	 */
	private void sendRegistrationIdToBackend()
	{
		URL url;
        try
        {  
            url = new URL("http://192.168.65.24/Push/register.jsp");
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException("invalid url: " + "http://192.168.65.24/Push/register.jsp");
        }
         
        StringBuilder bodyBuilder = new StringBuilder();
        Iterator<Entry<String, String>> iterator = params.entrySet().iterator();
        
        while (iterator.hasNext())
        {
            Entry<String, String> param = iterator.next();
            bodyBuilder.append(param.getKey()).append('=')
                    .append(param.getValue());
            if (iterator.hasNext())
            {
                bodyBuilder.append('&');
            }
        }
         
        String body = bodyBuilder.toString();
         
        Log.v(TAG, "Posting '" + body + "' to " + url);
         
        byte[] bytes = body.getBytes();
         
        HttpURLConnection conn = null;
        try
        {
            Log.e("URL", "> " + url);
            
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
             
            // handle the response
            int status = conn.getResponseCode();
             
            // If response is not success
            if (status != 200)
            {
              throw new IOException("Post failed with error code " + status);
            }
        }
        catch (IOException e)
        {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        finally
        {
            if (conn != null)
            {
                conn.disconnect();
            }
        }
	}
	
	/**
	 * Stores the registration ID and app versionCode in the application's
	 * {@code SharedPreferences}.
	 *
	 * @param context application's context.
	 * @param regId registration ID
	 */
	private void storeRegistrationId(Context context, String regId)
	{
	    final SharedPreferences prefs = getGCMPreferences(context);
	    int appVersion = getAppVersion(context);
	    Log.i(TAG, "Saving regId on app version " + appVersion);
	    
	    String registrationName = prefs.getString("ZTrade_Username", "");
	    
	    params.put("regId", regId);
	    params.put("username", "konstantinos_ar");
	    //params.put("email", "ka@metrictrade.com");
	    
	    SharedPreferences.Editor editor = prefs.edit();
	    editor.putString(PROPERTY_REG_ID, regId);
	    editor.putInt(PROPERTY_APP_VERSION, appVersion);
	    editor.putString(PROPERTY_REG_NAME, registrationName);
	    //editor.putString(PROPERTY_REG_EMAIL, gmail);
	    editor.commit();
	}
	
}
