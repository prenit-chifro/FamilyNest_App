package app.android.family.location.emergency.safety.familynest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gcm.GCMRegistrar;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class EventBroadcastReceiver extends BroadcastReceiver implements GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener, LocationListener{

	private static final String TAG = "EventBroadcastReceiver";
	private static final String BOOT_EVENT_BROADCAST_RECEIVER_TAG = "BOOT_EVENT_BROADCAST_RECEIVER";
	private static final String INTERNET_CONNECTIVITY_EVENT_BROADCAST_RECEIVER_TAG = "INTERNET_CONNECTIVITY_EVENT_BROADCAST_RECEIVER";
	private static final String GPS_STATUS_CHANGE_EVENT_BROADCAST_RECEIVER_TAG = "GPS_STATUS_CHANGE_EVENT_BROADCAST_RECEIVER";
	
	static String serverUrl = "http://192.168.1.12:3000/update_user_location_from_device";
	static String charset = "UTF-8";  // Or in Java 7 and later, use the constant: java.nio.charset.StandardCharsets.UTF_8.name()
		
	public static volatile boolean isInternetAvailable;
	public static volatile boolean isGPSEnabled;
	public static volatile boolean canGetLocation;
	
	private static volatile ConnectivityManager connectivityManager;
	private static volatile NetworkInfo networkInfo;
	private static volatile GoogleApiClient mGoogleApiClient;
	private static volatile LocationManager locationManager;
	private static volatile LocationRequest mLocationRequest;
	private static Location mLastLocation;
	private static volatile Notification internetErrorNotification;
	private static volatile Notification gpsErrorNotification;
	
	// The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // 10 meters
 
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 30 ; // 30 seconds

	private static volatile EventBroadcastReceiver thisClassInstance;
	public static volatile Context mycontext;
	
	@Override
	public void onReceive(Context context, Intent intent) {
		if(thisClassInstance == null){
			thisClassInstance = new EventBroadcastReceiver();
		}
		
		if(mycontext == null){
			mycontext = context;
		}
		
		if (intent.getAction().matches("android.net.conn.android.intent.action.BOOT_COMPLETED")) {
			Log.v(BOOT_EVENT_BROADCAST_RECEIVER_TAG, "DEVICE BOOT EVENT RECEIVED");
	    	
	    	setInternetStatus(context, null);
	    	
	    	setGPSStatus(context);
	    	
	    }
		
		if (intent.getAction().matches("android.net.conn.CONNECTIVITY_CHANGE")) {
			Log.v(INTERNET_CONNECTIVITY_EVENT_BROADCAST_RECEIVER_TAG, "INTERNET CCONNECTION STATUS CHANGE EVENT RECEIVED");
	    	
	    	setInternetStatus(context, null);
	    }
		
		if (intent.getAction().matches("android.location.PROVIDERS_CHANGED")) {
			Log.v(GPS_STATUS_CHANGE_EVENT_BROADCAST_RECEIVER_TAG, "GPS STATUS CHANGE RECEIVED");
	    	
	    	setGPSStatus(context);
	    }
		
	}
	
	public static void setInternetStatus(Context context, Activity activity){
		
		if(mycontext == null){
			mycontext = context;
		}
		
		if(connectivityManager == null){
			connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		}
		
		networkInfo = connectivityManager.getActiveNetworkInfo();
		
		if( networkInfo != null && networkInfo.isConnected()){
			 CommonMethods.setInternetStatus(context, true);
	 
             AndroidMainActivity.setLayoutIfInternetAvailable(context, activity);
			
             NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
          	 manager.cancel(R.string.app_name-1);
			 
          	 sendStoredLocationsToServer(context);
		
		 }else {
			 CommonMethods.setInternetStatus(context, false);
			 setInternetNotification(context);
			 AndroidMainActivity.setLayoutIfInternetAvailable(context, activity);
					 
		 }
	}
		
	public static void setGPSStatus(Context context){
			
		if(mycontext == null){
			mycontext = context;
		}
		
		locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		
		try {
            // getting GPS status
            isGPSEnabled = locationManager
                    .isProviderEnabled(LocationManager.GPS_PROVIDER);
            
            if(isGPSEnabled){
            	
            	CommonMethods.setGPSStatus(context, true);
            	
            	startGPSUpdates(context, null);
            	
            	NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            	manager.cancel(R.string.app_name-2);
            	
            } else {
            	
            	CommonMethods.setGPSStatus(context, false);
            	
            	setGPSNotification(context);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
	}

	public static void startGPSUpdates(Context context, LocationRequest newLocationRequest){
		
		if(mycontext == null){
			mycontext = context;
		}
		
		if(mGoogleApiClient == null){
			if(thisClassInstance == null){
				thisClassInstance = new EventBroadcastReceiver();
			}
			mGoogleApiClient = new GoogleApiClient.Builder(context)
		            .addConnectionCallbacks(thisClassInstance)
		            .addOnConnectionFailedListener(thisClassInstance)
		            .addApi(LocationServices.API)
		            .build();
		}
		   	    
		if(!mGoogleApiClient.isConnected()){
			mGoogleApiClient.connect();
		} else {
			if(newLocationRequest == null){
				if(mLocationRequest == null){
					mLocationRequest = LocationRequest.create();
			        mLocationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES); // Updte every 10 meters
			        mLocationRequest.setInterval(MIN_TIME_BW_UPDATES); // Update location every 60 seconds
			        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
			    }
		        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, thisClassInstance);
			}else{
		        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, newLocationRequest, thisClassInstance);
			}
			
		}
	
	}
	
	public static void stopGPSUpdates(){
		
		if(mGoogleApiClient != null && mGoogleApiClient.isConnected()){
			mGoogleApiClient.disconnect();
		}
	}
		
	public static Location getLastKnowLocation(){
		mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
		return mLastLocation;
	}
	
	public static void sendStoredLocationsToServer(Context context){
		
		if(mycontext == null){
			mycontext = context;
		}
		
		SharedPreferences unsentLocationUpdates = mycontext.getSharedPreferences("UnsentLocationUpdates", 0);
		 String locationUpdatesString = unsentLocationUpdates.getString("locationUpdatesString", "");
		 if(locationUpdatesString != ""){
			 String[] locationUpdatesArray = locationUpdatesString.split(", ", -1);
			 if(locationUpdatesArray.length > 0){
				 for(int i=0; i< locationUpdatesArray.length-1; i++){
					 String[] locationDetailArray = locationUpdatesArray[i].split("-", -1);
					 double latitude = Double.parseDouble(locationDetailArray[0]);
					 double longitude = Double.parseDouble(locationDetailArray[1]);
					 long creationTime = Long.parseLong(locationDetailArray[2]);
					 sendLocationToServer(latitude, longitude, creationTime);
				 }
			 }
		 }
		 SharedPreferences.Editor editor = unsentLocationUpdates.edit();
		 editor.putString("locationUpdatesString", "");
		 editor.apply();
	}
	
	public static void sendLocationToServer(double latitude, double longitude, long creationTimeInMiliseconds){
		
		SharedPreferences currentUserIdPref = mycontext.getSharedPreferences("currentUserId", Context.MODE_WORLD_READABLE);
		int currentUserId = currentUserIdPref.getInt("currentUserId", -1);
		String gcmRegistrationId = GCMRegistrar.getRegistrationId(mycontext);
		
		final String query;
				
		if(currentUserId > 0){
			query = String.format("latitude=%s&longitude=%s&creation_time=%d&current_user_id=%d&gcm_registration_id=%s", (long)latitude, longitude, creationTimeInMiliseconds, currentUserId, gcmRegistrationId);
			
		}else{
			if(gcmRegistrationId != ""){
				query = String.format("latitude=%s&longitude=%s&creation_time=%d&gcm_registration_id=%s", latitude, longitude, creationTimeInMiliseconds, gcmRegistrationId);
				
			}else{
				query = "";
				
			}
			
		}
		 
		if(query != ""){
			Thread thread = new Thread(new Runnable()
			{
			    @Override
			    public void run() 
			    {	
			    	URLConnection connection;
					try {
						connection = new URL(serverUrl).openConnection();
						connection.setDoOutput(true); // Triggers POST.
						connection.setRequestProperty("Accept-Charset", charset);
						connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=" + charset);
						
						try {
							OutputStream output = connection.getOutputStream();
						    output.write(query.getBytes(charset));
						} catch (IOException e) {
							e.printStackTrace();
						}

						try 
				        {
				        	InputStream response = connection.getInputStream();
				        	
				        } 
				        catch (Exception e)
				        {
				            e.printStackTrace();
				        }
						
					} catch (MalformedURLException e) {
						
						e.printStackTrace();
					} catch (IOException e) {
						
						e.printStackTrace();
					}
			    }
			});
			thread.start();
		}
	}
	
	private static void setInternetNotification(Context context){
		if(mycontext == null){
			mycontext = context;
		}
		
		if(internetErrorNotification == null){
			String title = "Internet connection not available";
			String message = "Tap to connect";
			
			
			Intent wifiIntent = new Intent("android.net.wifi.PICK_WIFI_NETWORK");
			PendingIntent pIntent = PendingIntent.getActivity(mycontext, 0, wifiIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
						
			// Create the notification with a notification builder
			NotificationCompat.Builder internetErrorNotificationBuilder = new NotificationCompat.Builder(mycontext);
			internetErrorNotificationBuilder.setSmallIcon(R.drawable.ic_launcher);
			internetErrorNotificationBuilder.setWhen(System.currentTimeMillis());
			internetErrorNotificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
			internetErrorNotificationBuilder.setContentTitle(title);
			internetErrorNotificationBuilder.setContentText(message);
			internetErrorNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
			internetErrorNotificationBuilder.setContentIntent(pIntent);					
			internetErrorNotification = internetErrorNotificationBuilder.build();		
							
			// Remove the notification on click
			internetErrorNotification.flags |= Notification.FLAG_AUTO_CANCEL;
			internetErrorNotification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			internetErrorNotification.priority = Notification.PRIORITY_MAX;
		}
	
		NotificationManager manager = (NotificationManager) mycontext.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(R.string.app_name-1, internetErrorNotification);
		{
			// Wake Android Device when notification received
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			final PowerManager.WakeLock mWakelock = pm.newWakeLock(
					PowerManager.FULL_WAKE_LOCK
							| PowerManager.ACQUIRE_CAUSES_WAKEUP, "GCM_PUSH");
			mWakelock.acquire();
 
			// Timer before putting Android Device to sleep mode.
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				public void run() {
					mWakelock.release();
				}
			};
			timer.schedule(task, 5000);
		}

	}
	
	private static void setGPSNotification(Context context){
		
		if(mycontext == null){
			mycontext = context;
		}
		
		if(gpsErrorNotification == null){
			String title = "Location service not available";
			String message = "Tap to open GPS settings";
			
			Intent gpsIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
			PendingIntent pIntent = PendingIntent.getActivity(mycontext, 0, gpsIntent,
					PendingIntent.FLAG_UPDATE_CURRENT);
			
			// Create the notification with a notification builder
			NotificationCompat.Builder internetErrorNotificationBuilder = new NotificationCompat.Builder(context);
			internetErrorNotificationBuilder.setSmallIcon(R.drawable.ic_launcher);
			internetErrorNotificationBuilder.setWhen(System.currentTimeMillis());
			internetErrorNotificationBuilder.setDefaults(Notification.DEFAULT_SOUND);
			internetErrorNotificationBuilder.setContentTitle(title);
			internetErrorNotificationBuilder.setContentText(message);
			internetErrorNotificationBuilder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
			internetErrorNotificationBuilder.setContentIntent(pIntent);									
			gpsErrorNotification = internetErrorNotificationBuilder.build();		
							
			// Remove the notification on click
			gpsErrorNotification.flags |= Notification.FLAG_AUTO_CANCEL;
			gpsErrorNotification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			gpsErrorNotification.priority = Notification.PRIORITY_MAX;
		}
	
		NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		manager.notify(R.string.app_name-2, gpsErrorNotification);
		{
			// Wake Android Device when notification received
			PowerManager pm = (PowerManager) context
					.getSystemService(Context.POWER_SERVICE);
			final PowerManager.WakeLock mWakelock = pm.newWakeLock(
					PowerManager.FULL_WAKE_LOCK
							| PowerManager.ACQUIRE_CAUSES_WAKEUP, "GCM_PUSH");
			mWakelock.acquire();
 
			// Timer before putting Android Device to sleep mode.
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				public void run() {
					mWakelock.release();
				}
			};
			timer.schedule(task, 5000);
		}

	}
	
	@Override
	public void onLocationChanged(Location changedLocation) {
		
		Long createdAtMiliseconds = System.currentTimeMillis();
		if(CommonMethods.isInternetAvailable(mycontext)){
			
			sendLocationToServer(changedLocation.getLatitude(), changedLocation.getLongitude(), createdAtMiliseconds);
		}else{
			
			SharedPreferences unsentLocationUpdates = mycontext.getSharedPreferences("UnsentLocationUpdates", 0);
			SharedPreferences.Editor editor = unsentLocationUpdates.edit();
			String locationUpdatesString = unsentLocationUpdates.getString("locationUpdatesString", "");
			if(locationUpdatesString == ""){
				locationUpdatesString = String.format("%s-%s-%s", changedLocation.getLatitude(), changedLocation.getLongitude(), createdAtMiliseconds );
				editor.putString("locationUpdatesString", locationUpdatesString);
			}else{
				locationUpdatesString = locationUpdatesString + String.format(", %s-%s-%s", changedLocation.getLatitude(), changedLocation.getLongitude(), createdAtMiliseconds );
				editor.putString("locationUpdatesString", locationUpdatesString);
			}
        	        	
        	// Apply the edits!
        	editor.apply();
		}
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		canGetLocation = false;
	}

	@Override
	public void onConnected(Bundle bundle) {
		
		if(mLocationRequest == null){
			mLocationRequest = LocationRequest.create();
	        mLocationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES); // Updte every 10 meters
	        mLocationRequest.setInterval(MIN_TIME_BW_UPDATES); // Update location every 60 seconds
	        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
	    }
		
		if(isGPSEnabled == true){
			LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, thisClassInstance);
		}
		
	}

	@Override
	public void onConnectionSuspended(int code) {
		canGetLocation = false;
	}
	
}