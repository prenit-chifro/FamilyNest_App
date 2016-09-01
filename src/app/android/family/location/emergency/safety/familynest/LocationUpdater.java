package app.android.family.location.emergency.safety.familynest;

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.webkit.WebView;
import android.widget.Toast;

public class LocationUpdater implements GoogleApiClient.ConnectionCallbacks,
GoogleApiClient.OnConnectionFailedListener, LocationListener{

	public AndroidMainActivity mActivity;
	public Context mContext;
	public WebView mWebView;
	
	public String needLocationFor;
	public String extraDataString = null;
	
  	public LocationManager locationManager;
	public Location mLastLocation;
	public GoogleApiClient mGoogleApiClient;
	public LocationRequest mLocationRequest;
	
	// flag for GPS status
    boolean isGPSEnabled = false;
    boolean isNetworkEnabled;
    
    // flag for GPS status
    boolean canGetLocation = false;
        	
    boolean doesNeedContinuousLocationUpdates = false;
	// The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
 
    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1000 * 30; // 30 seconds
	
    public LocationUpdater(AndroidMainActivity activity, Context context, WebView webView){
    	mActivity = activity;
    	this.mContext = context;
    	this.mWebView = webView;
    	buildGoogleApiClient();
    }
    
    void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        mLocationRequest = LocationRequest.create();
        mLocationRequest.setSmallestDisplacement(MIN_DISTANCE_CHANGE_FOR_UPDATES); // Updte every 10 meters
        mLocationRequest.setInterval(MIN_TIME_BW_UPDATES); // Update location every 60 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        
    }
    
    public boolean canGetLocation() {	
    	return CommonMethods.isGPSAvailable(mContext);
    }
    

    /**
     * Start using GPS listener
     * Calling this function will Start using GPS in your app
     * */
    public void startGpsLocationUpdates(String locationFor){
    	if(canGetLocation()){
    		
    		needLocationFor = locationFor;
        	
        	if(needLocationFor == "web"){
        		doesNeedContinuousLocationUpdates = true;
        	}
        	
    		Toast.makeText(mContext, "Location Updates started.", Toast.LENGTH_SHORT).show();
    		if(!mGoogleApiClient.isConnected()){
    			mGoogleApiClient.connect();
    		} else {
    			
    	        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    	        
    	        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
    	                mGoogleApiClient);
    	        
    	    }
    		
    	} else {
    		showSettingsAlert();
    	}
    	
    }
    
    /**
     * Stop using GPS listener
     * Calling this function will stop using GPS in your app
     * */
    public void stopGpsLocationUpdates(){
    	if(mGoogleApiClient.isConnected()){
    		mGoogleApiClient.disconnect();
    		doesNeedContinuousLocationUpdates = false;
    	}
        Toast.makeText(mContext, "Location Updates stopped.", Toast.LENGTH_SHORT).show();
               
    }

    @Override
	public void onConnected(Bundle arg0) {
	 	
		LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
            		
	}

	@Override
	public void onLocationChanged(Location new_location) {
		
    	if(new_location != null){
    		mLastLocation = new_location;
    		
    		if(needLocationFor == "sms"){
    			if(doesNeedContinuousLocationUpdates == false){
    	        	stopGpsLocationUpdates();
    	        }
    			try {
    				
    				JSONObject familyJson = null;
    				if(extraDataString == null || extraDataString.isEmpty()){
    					extraDataString = mContext.getSharedPreferences("FamilyMemberDetails", 0).getString("familyMemberDetailsString", null);
    				} 
    				
    				if(extraDataString != null && !extraDataString.isEmpty()){
    					familyJson = new JSONObject(extraDataString);
        				
        				String[] relations = familyJson.getString("relations").split(", ", -1);
    	                String[] isdCodes = familyJson.getString("isd_codes").split(", ", -1);
    	                String[] contactNos = familyJson.getString("contact_nos").split(", ", -1);
    	    			

    	                SmsManager sms = SmsManager.getDefault();
    	               
    	    	        for (int i = 0; i < contactNos.length; i++) {
    	    	        	if(contactNos[i] != null || !contactNos[i].isEmpty()){
        	    	        	String message; String contactNo;
    		    	        	
        	    	        	message = relations[i] + ", I am in an Emergency. Please Help. My last location is http://maps.google.com/maps?daddr=" + new_location.getLatitude() + "," + new_location.getLongitude() + " Visit http://familynest.co for more details";
        	    	            ArrayList<String> messageParts = sms.divideMessage(message);

        	    	            contactNo = "+" + isdCodes[i] + contactNos[i];
        	    	            
        	    	            try{
        	    	            	sms.sendMultipartTextMessage(contactNo, null, messageParts, null, null);
        		    	            Toast.makeText(mContext, "Successfully sent Emergency SMS to your " + relations[i], Toast.LENGTH_SHORT).show();
        		    	        		    	            
        	    	            }
        	    	            catch(Exception e){
        	    	            	
        	    	            }
        	    	         
    	    	        	}
    	    	        }
    	    	    
    				}
    					    	        	    	        
	    	        extraDataString = null;
	
    			} catch (JSONException e) {
    	            e.printStackTrace();
    	        }
    			
    		} else {
    			mActivity.loadUrl(String.format("javascript:notifyGeolocationChange('%f', '%f', '%s')" , mLastLocation.getLatitude(), mLastLocation.getLongitude(), needLocationFor));
    		}
    		
    	}			

	}

	@Override
	public void onConnectionFailed(ConnectionResult arg0) {
		canGetLocation = false;
	}

	@Override
	public void onConnectionSuspended(int arg0) {
		canGetLocation = false;
	}
	
	public void showSettingsAlert(){
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
      
        // Setting Dialog Title
        alertDialog.setTitle("GPS settings");
  
        // Setting Dialog Message
        alertDialog.setMessage("GPS is not enabled. Do you want to go to settings menu?");
  
        // On pressing Settings button
        alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog,int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                mContext.startActivity(intent);
            }
        });
  
        // on pressing cancel button
        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            dialog.cancel();
            }
        });
  
        // Showing Alert Message
        alertDialog.show();
    }


}
