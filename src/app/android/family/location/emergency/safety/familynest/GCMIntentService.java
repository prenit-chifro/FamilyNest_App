package app.android.family.location.emergency.safety.familynest;

import java.io.IOException;
import java.io.InputStream;
import java.util.Timer;
import java.util.TimerTask;

import com.google.android.gcm.GCMBaseIntentService;
import com.google.android.gcm.GCMRegistrar;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.webkit.WebView;
import app.android.family.location.emergency.safety.familynest.R;

public class GCMIntentService extends GCMBaseIntentService {
	 
	private static final String TAG = "FamilyNest GCM::Service";
 
	// Use your PROJECT ID from Google API into SENDER_ID
	public static final String SENDER_ID = "411131764027";
 
	public GCMIntentService() {
		super(SENDER_ID);
	}
 
	@Override
	protected void onRegistered(Context context, String registrationId) {
		WebToAndroidAppJavascriptInterface.gcmRegistrationId = registrationId;
		WebToAndroidAppJavascriptInterface.notifyGCMRegistrationId(registrationId);
		Log.i(TAG, "onRegistered: registrationId=" + registrationId);
	}
 
	@Override
	protected void onUnregistered(Context context, String registrationId) {
 
		Log.i(TAG, "onUnregistered: registrationId=" + registrationId);
	}
 
	@Override
	protected void onMessage(Context context, Intent data) {
		
		int id;
		String title;
		String message;
		String targetUrl;
		String imageUrl;
		String bigImageUrl;
		
		// Data from Notification 
		id = Integer.parseInt(data.getStringExtra("id"));
		title = data.getStringExtra("title");
		message = data.getStringExtra("message");
		targetUrl = data.getStringExtra("target_url");
		imageUrl = data.getStringExtra("image_url");
		bigImageUrl = data.getStringExtra("big_image_url");
		
		// Open a new activity called FullscreenActivity
		Intent intent = new Intent(this, AndroidMainActivity.class);
		// Pass data to the new activity
		intent.putExtra("targetUrl", targetUrl);
		intent.putExtra("STARTING_APP_FROM_NOTIFICATION", "Yes");

		// Starts the activity on notification click
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, intent,
				PendingIntent.FLAG_UPDATE_CURRENT);
		
		// Create the notification with a notification builder
		NotificationCompat.Builder builder = new NotificationCompat.Builder(  
                this);
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setWhen(System.currentTimeMillis());
		builder.setDefaults(Notification.DEFAULT_SOUND);
		builder.setContentTitle(title);
		builder.setContentText(message);
		builder.setContentIntent(pIntent);
				
		if(imageUrl != null && imageUrl != ""){
			try {
				
				InputStream imageInputStream = CommonMethods.downloadInputStream(imageUrl);
				Bitmap bigImage = BitmapFactory.decodeStream(imageInputStream);
				
				builder.setLargeIcon(bigImage);
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}else{
			builder.setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_launcher));
		}
		
		if(bigImageUrl != null && imageUrl != ""){
			try {
				
				InputStream imageInputStream = CommonMethods.downloadInputStream(bigImageUrl);
				Bitmap bigImage = BitmapFactory.decodeStream(imageInputStream);
				
				NotificationCompat.BigPictureStyle notiStyle = new NotificationCompat.BigPictureStyle();
				notiStyle.bigPicture(bigImage);
				notiStyle.setBigContentTitle(title);
				notiStyle.setSummaryText(message);
				
				builder.setStyle(notiStyle);
				
			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
		
		Notification notification = builder.build();		
				
		// Remove the notification on click
		notification.flags |= Notification.FLAG_AUTO_CANCEL;
 
		notification.sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		notification.priority = Notification.PRIORITY_MAX;
		NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		manager.notify(R.string.app_name+id, notification);
 
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
	protected void onError(Context arg0, String errorId) {
 
		Log.e(TAG, "onError: errorId=" + errorId);
	}
 
}