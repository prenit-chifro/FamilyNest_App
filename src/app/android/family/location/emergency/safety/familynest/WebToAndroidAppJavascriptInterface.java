package app.android.family.location.emergency.safety.familynest;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

import com.google.android.gcm.GCMRegistrar;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings.Secure;
import android.telephony.SmsManager;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.widget.Toast;

public class WebToAndroidAppJavascriptInterface{
	
	 public Context myContext;
	 public static AndroidMainActivity myActivity;
	 public WebView myWebView;
	 public LocationUpdater gps;
	 
	 public static boolean gcmRegistrationIdRequested = false;
	 public static String gcmRegistrationId = "";
	 	
    /** Instantiate the interface and set the context */
    WebToAndroidAppJavascriptInterface(Context c, AndroidMainActivity startActivity, WebView webView) {
        myContext = c;
        myActivity = startActivity;
        myWebView = webView;
        gps = new LocationUpdater(myActivity,myContext,myWebView);
        
    }
    
    /** Show a toast from the web page */
    @JavascriptInterface
    public void showToast(String toast) {
        Toast.makeText(myContext, toast, Toast.LENGTH_SHORT).show();
    }

    @JavascriptInterface
    public void shareOnAndroid(final String shareText, final String targetUrl, final String imageUrl, final String fileName ){
    	Thread thread = new Thread(new Runnable()
		{
		    @Override
		    public void run() {
		    	Intent shareIntent = new Intent();
		    	shareIntent.setAction(Intent.ACTION_SEND);
		    	
		    	if(imageUrl.equals("")){
			      		    	
		    		shareIntent.setType("text/plain");
		    				    	
		    	}else {
		    		
			        File imageFile = CommonMethods.downloadImage(fileName, imageUrl, myContext);
			    	Uri uri = Uri.fromFile(imageFile);
			    	
			    	shareIntent.setType("image/jpeg");
			    	shareIntent.putExtra(Intent.EXTRA_STREAM,uri);

		    	}
		    	if(shareText.equals("")){
		    		shareIntent.putExtra(Intent.EXTRA_TEXT, targetUrl);
		    		shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared from FamilyNest");
		    	}else {
		    		shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
					shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Shared from FamilyNest");
		    	}
		        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		    	
				myActivity.startActivityForResult(Intent.createChooser(shareIntent, "Share On"), 2);

		    }
		});
    	thread.start();
            	
    }
    
    @JavascriptInterface
    public String getAndroidGCMRegistratioId() {
    	
    	if(checkNeedForNewGCMRegistration()){
    		GCMRegistrar.register(myActivity,
    		GCMIntentService.SENDER_ID);
    		gcmRegistrationIdRequested = true;
        }
    	
    	gcmRegistrationId = GCMRegistrar.getRegistrationId(myContext);
    	if(gcmRegistrationId != ""){
    		GCMRegistrar.setRegisteredOnServer(myContext, true);
    	}
    	return gcmRegistrationId;
    	
    }
    
    /** check if there is need to register again on gcm and update gcm registration id on server
     * 
     * @return
     */
    public boolean checkNeedForNewGCMRegistration(){
    	if(GCMRegistrar.isRegistered(myContext)){
    		if(GCMRegistrar.isRegisteredOnServer(myContext)){
            	gcmRegistrationId = GCMRegistrar.getRegistrationId(myContext);
            	if(!gcmRegistrationId.equals("")){
            		return false;
            	}
    		} 
    	} 
    	
    	return true;
    }
    
    @JavascriptInterface 
    public boolean canGetGpsLocation(){
    	return CommonMethods.isGPSAvailable(myContext);
    }
     
    /** Get GeoLocationLatitude From Android **/
    @JavascriptInterface
    public void getGeoLocation(String locationFor){
    	     	
    	// check if GPS enabled     
        if(canGetGpsLocation()){
        	
        	gps.startGpsLocationUpdates(locationFor);
                	
        }else{
            // can't get location
            // GPS or Network is not enabled
            // Ask user to enable GPS/network in settings
        	showToast("Please turn on GPS.");
            gps.showSettingsAlert();
                            
        }
        
    }
    
     
    /** Stop GPS GeoLocation Updates From Android **/
    @JavascriptInterface
    public void stopGeoLocationUpdates(){
    	   	    	
    	gps.stopGpsLocationUpdates();
    	
    }
    
    @JavascriptInterface
    public void startGeoLocationUpdatesInBackground(){
    	myActivity.startGPSUpdatesInBackground();
    }
    
    @JavascriptInterface
    public void stopGeoLocationUpdatesInBackground(){
    	   	    	
    	myActivity.stopGPSUpdatesInBackground();
    	
    }
    
    /** Stop GPS GeoLocation Updates From Android **/
    @JavascriptInterface
    public void getContactNoDetail(){
    	// Declare
    	  
    	  Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
    	  int PICK_CONTACT = 3;
    	  myActivity.startActivityForResult(intent, PICK_CONTACT);
   	    	
    }
    
    @JavascriptInterface
    public void sendSMS(String isdCode, String contactNumber, String message) {
        SmsManager sms = SmsManager.getDefault();
    	String phoneNumber = "+" + isdCode + contactNumber;  
        ArrayList<String> messageParts = sms.divideMessage(message);


        sms.sendMultipartTextMessage(phoneNumber, null, messageParts, null, null);
        showToast("SMS Sent Successfully");
    }
    
    @JavascriptInterface
    public void dialCall(String isdCode, String contactNumber){
    	showToast("in android call method: " + isdCode + contactNumber);
    	if((isdCode != null && isdCode != "") && (contactNumber != null && contactNumber != "")){
    		Intent callIntent = new Intent(Intent.ACTION_DIAL);
    		callIntent.setData(Uri.parse("tel:" + isdCode + contactNumber));
    		myActivity.startActivity(callIntent);
    	}
    }
    		
    @JavascriptInterface
    public void saveFamilyMembeDetails(String familyMemberDetailsString){
    	if(familyMemberDetailsString != null && !familyMemberDetailsString.isEmpty()){
    		SharedPreferences familyMemberDetails = myContext.getSharedPreferences("FamilyMemberDetails", 0);
        	SharedPreferences.Editor editor = familyMemberDetails.edit();
        	editor.putString("familyMemberDetailsString", familyMemberDetailsString);
        	// Apply the edits!
        	editor.apply();
        	//showToast("Successfully saved Family Member details in phone ");
        	try {
    			JSONObject familyJson = new JSONObject(familyMemberDetailsString);
    			String imageUrlsString = familyJson.getString("image_urls"); String[] imageUrls = imageUrlsString.split(", ", -1);
    			String imageFileNamesString = familyJson.getString("image_file_names"); String[] imageFileNames = imageFileNamesString.split(", ", -1);
    			for(int i = 0; i< imageFileNames.length; i++){
    				if(imageFileNames[i] != null && !imageFileNames[i].isEmpty()){
    					final String fileName = imageFileNames[i];
    					final String fileUrl = imageUrls[i];
    					Thread thread = new Thread(new Runnable()
    					{
    					    @Override
    					    public void run() {
    					    	CommonMethods.downloadImage(fileName, fileUrl, myContext);
    					    }
    					});
    					thread.start();
    				}
    				
    			}
    		} catch (JSONException e) {
     
    			e.printStackTrace();
    		}

    	}
    }
    
    @JavascriptInterface
    public String getFamilyMembeDetails(){
    	return myContext.getSharedPreferences("FamilyMemberDetails", 0).getString("familyMemberDetailsString", null);
    }
    
    
    @JavascriptInterface
    public void openImage(final String fileName, final String imageUrl){
    	
    	Thread thread = new Thread(new Runnable()
		{
		    @Override
		    public void run() {
		    	File imageFile = CommonMethods.downloadImage(fileName, imageUrl, myContext);
		    	
		    	if(imageFile != null){
		    		new MediaScanner(myActivity, imageFile);
		    	}
					
		    }
		});
    	thread.start();
				    
    }
    
    @JavascriptInterface
    public void checkInternetConnection(){
    	myActivity.setLayoutIfInternetAvailable(myContext);
    }
    
    @JavascriptInterface
    public void sendEmergencySMS(String jsonString){
    	if(gps.canGetLocation()){
    		if(jsonString != null && !jsonString.isEmpty()){
    			gps.extraDataString = jsonString;
    		}
    		gps.startGpsLocationUpdates("sms"); 
    	}else {
    		try {
        		JSONObject familyJson;
        		if(jsonString == null || jsonString.isEmpty()){
        			jsonString = myContext.getSharedPreferences("FamilyMemberDetails", 0).getString("familyMemberDetailsString", null);        			
        		}
        		
        		if(jsonString != null && !jsonString.isEmpty()){
        			familyJson = new JSONObject(jsonString);
            	    String[] relations = familyJson.getString("relations").split(", ", -1);
                    String[] isdCodes = familyJson.getString("isd_codes").split(", ", -1);
                    String[] contactNos = familyJson.getString("contact_nos").split(", ", -1);
                
                    SmsManager sms = SmsManager.getDefault();
                    
        	        for (int i = 0; i < contactNos.length; i++) {
        	        	if(contactNos[i] != null || !contactNos[i].isEmpty()){
        	        		String message; String contactNo;
            	        	
        	        		message = relations[i] + ", I am in an Emergency. Please Help Visit http://familynest.co for more details";
            	        	ArrayList<String> messageParts = sms.divideMessage(message);
            	            contactNo = "+" + isdCodes[i] + contactNos[i];
            	            
            	            sms.sendMultipartTextMessage(contactNo, null, messageParts, null, null);
            	            Toast.makeText(myContext, "Successfully sent Alert SMS to your " + relations[i], Toast.LENGTH_SHORT).show();
        	        	}
        	        	
        	        }

        		}
        	} catch (JSONException e) {
    			e.printStackTrace();
    		}

    	}
    }
    
    @JavascriptInterface
    public String downloadImageAndReturnLocalPath(String fileName, String imageUrl){
    	if(fileName != null && !fileName.isEmpty()){
    		File imageFile = CommonMethods.downloadImage(fileName, imageUrl, myContext);
        	if(imageFile != null && imageFile.exists()){
        		return "file://" + imageFile.getAbsolutePath().toString();
        	}
    	}
    	
    	return "";
    	
    }
    
    @JavascriptInterface
    public void startGoogleMapDirections(String sLat, String sLong, String eLat, String eLong){
    	Intent intent;
    	if((sLat != null  && !sLat.isEmpty())&&(sLong != null  && !sLong.isEmpty())){
    		 intent = new Intent(android.content.Intent.ACTION_VIEW, 
    			    Uri.parse("http://maps.google.com/maps?saddr=" + sLat + "," + sLong + "&daddr=" + eLat + "," + eLong));

    	} else {
    		 intent = new Intent(android.content.Intent.ACTION_VIEW, 
    				 Uri.parse("http://maps.google.com/maps?daddr=" + eLat + "," + eLong));
    	}
    	myActivity.startActivity(intent);

    }
    
    public static void notifyGCMRegistrationId(String registrationId){
    	if(registrationId != "" && gcmRegistrationIdRequested == true){
    		String url = String.format("javascript:notifyGCMRegistrationId('%s')" , registrationId);
        	myActivity.loadUrl(url);
    	}
    }
    
    @JavascriptInterface
    public void setCurrentUserIdOnDevice(String currentUserId){
    	if(currentUserId != null && currentUserId != "" && myContext != null){
    		CommonMethods.setCurrentUserId(myContext, Integer.parseInt(currentUserId));
    	}
    }
    
    @JavascriptInterface
    public void setCurrentUrl(String relativePath){
    	if(relativePath != null && myActivity != null && myActivity.finalUrlToLoadInWebView != myActivity.finalHostToLoadInWebView + relativePath){
    		myActivity.finalUrlToLoadInWebView = myActivity.finalHostToLoadInWebView + relativePath;
    	}
    }
   
    @JavascriptInterface
    public float getBatteryLevel(){
    	return EventBroadcastReceiver.getBatteryLevel(myContext);	
    }
     
}