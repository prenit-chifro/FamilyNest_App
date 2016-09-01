package app.android.family.location.emergency.safety.familynest;

import java.io.File;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;

import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;
import android.widget.RelativeLayout.LayoutParams;

@SuppressLint("NewApi")
public class AndroidMainActivity extends Activity {
	
	private static String homeLocalUrl = "http://192.168.1.12:3000";
	private String localUrl = "http://192.168.137.1:3000";
    public String liveUrl = "http://familynest.co";
    public static String finalUrlToLoadInWebView = homeLocalUrl;   
    
	public static Context myContext;
	
	public static volatile EventBroadcastReceiver eventBroadcastReceiver; 
	    
	public static ViewFlipper flipper;
	public static WebView myWebView;
    
    public static boolean needToLoadUrl = true;
        
    public ProgressDialog myProgressDialog;
    public static Builder alertDialog;
    public static AlertDialog internetAlert;
    
    private ValueCallback<Uri> fileUploadCallback;
    private ValueCallback<Uri[]> fileUploadCallbackArray;

    private final static int FILECHOOSER_REQUESTCODE = 1;
    private final static int SHARE_REQUESTCODE = 2;
    private Uri cameraImageUri;
    private String cameraImagePath;
    private static final int PICK_CONTACT = 3;
    
    private String contactNumber = null;
    private String contactName = null;
    
    /**
     * Tag used on log messages.
     */
    static final String TAG = "FamilyNest_APP_MAIN_ACTIVITY";
    static final String TAG_ID = TAG+"-ID";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	    	
    	super.onCreate(savedInstanceState);
    	setContentView(R.layout.activity_android_main);
    	
    	flipper = (ViewFlipper) findViewById(R.id.viewFlipper);
        flipper.setDisplayedChild(0);
        
        myProgressDialog = new ProgressDialog(this);
        myProgressDialog.setMessage("Please wait ...");
        
        myWebView = (WebView) findViewById(R.id.myWebView);
        setWebView(myWebView);
    	        
        Intent startingIntent = getIntent();

     	// Check if App is launched or new Instance of Activity created from Notification 
     	if(startingIntent.getStringExtra("STARTING_APP_FROM_NOTIFICATION") != null){
     		// Yes From Notification. Handle Notification  
			String targetUrl = startingIntent.getStringExtra("targetUrl");
			if(targetUrl != myWebView.getUrl()){
				finalUrlToLoadInWebView = targetUrl;
			}else{
				needToLoadUrl = false;
			}
			
		} 
			
     	// Check if app is launched by Url Click from outside source via our app's URI Scheme 
     	if(startingIntent.getStringExtra("URL_FROM_WEB_SEPARATE_APP_LAUNCH") != null){
     		// Yes From URL Click
     		
     		String targetUrl = startingIntent.getStringExtra("URL_FROM_WEB_SEPARATE_APP_LAUNCH");
     		if(targetUrl != myWebView.getUrl()){
         		finalUrlToLoadInWebView = targetUrl;
     		}else{
     			needToLoadUrl = false;
     		}
    	} else {
    		Uri startingData = startingIntent.getData();
           	if(startingData != null) {
           		String scheme = startingData.getScheme(); // "http"
                String host = startingData.getHost(); // "kidss.com or localhost ip"
                List<String> pathSegments = startingData.getPathSegments();
    	        int pathSegmentsSize = pathSegments.size();
    	             
    	        String hostUrlFromOutsideClick = String.format("%s://%s", scheme, host);
    	        String fullUrlFromOutsideClick = hostUrlFromOutsideClick;
    	        for (int i=0; i < pathSegmentsSize; i++) {
    	        	fullUrlFromOutsideClick = fullUrlFromOutsideClick + "/" + pathSegments.get(i);
    	        }
    	        
    	        if(fullUrlFromOutsideClick != myWebView.getUrl()){
    	        	Intent startAppFromWebIntent = new Intent(this, AndroidMainActivity.class);
    	            startAppFromWebIntent.putExtra("URL_FROM_WEB_SEPARATE_APP_LAUNCH", fullUrlFromOutsideClick);
    	            startAppFromWebIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
    	     				| Intent.FLAG_ACTIVITY_NEW_TASK);
    	            startActivity(startAppFromWebIntent);
    	            setResult(RESULT_OK, startingIntent);
    	            finish();
    	        }else{
    	        	needToLoadUrl = false;
    	        }
	            
           	}          
        }
    	
     	myContext = getApplicationContext();
     	setActivityRunning(true);
     	
     	EventBroadcastReceiver.setInternetStatus(myContext, this);
     	EventBroadcastReceiver.setGPSStatus(myContext);
    
    }
    
    @Override
    public void onResume()
    {
       super.onResume();
       setLayoutIfInternetAvailable(myContext, this);

    }
    public void onPause()
    {
       super.onPause();
    }
    @Override
    public void onDestroy()
    {
    	setActivityRunning(false);
    	super.onDestroy();
    }
    @Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
	    // Check if the key event was the Back button and if there's history
	    if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
	        myWebView.goBack();
	        return true;
	    }
	    // If it wasn't the Back key or there's no web page history, bubble up to the default
	    // system behavior (probably exit the activity)
	    return super.onKeyDown(keyCode, event);
	}
    @Override 
    protected void onActivityResult(int requestCode, int resultCode,  
                                       Intent intent) { 
    	if(requestCode == FILECHOOSER_REQUESTCODE){
    		if(resultCode == RESULT_OK){
    			if(fileUploadCallbackArray != null){
                    Uri[] results = null;
                    if (intent == null) {
                        if(cameraImagePath != null) {
                            results = new Uri[]{Uri.parse(cameraImagePath)};
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            results = new Uri[]{Uri.parse(dataString)};
                        }
                    }
                    fileUploadCallbackArray.onReceiveValue(results);
                    fileUploadCallbackArray = null;
                    
                    return;
                }
                if(fileUploadCallback != null) {
                    Uri result = null;
                    if (intent == null) {
                        if(cameraImagePath != null) {
                            result = Uri.parse(cameraImagePath);
                        }
                    } else {
                        String dataString = intent.getDataString();
                        if (dataString != null) {
                            result = Uri.parse(dataString);
                        }
                    }
                    fileUploadCallback.onReceiveValue(result);
                    fileUploadCallback = null;

                    return;
                }

    		}
    		
    		if(resultCode == RESULT_CANCELED){
    			if(fileUploadCallbackArray != null) {
        	        
                    fileUploadCallbackArray.onReceiveValue(null);
                    return;

                }
                if(fileUploadCallback != null) {
        	        
                    fileUploadCallback.onReceiveValue(null);
                    return;

                }

    		}
        }

    	if(requestCode == SHARE_REQUESTCODE){
    		if(resultCode == RESULT_OK){
    			Toast.makeText(this, "Shared Successfully", Toast.LENGTH_SHORT).show();    			
            }
    	}
    	
    	
        if (requestCode == PICK_CONTACT) {
        	
          if (resultCode == RESULT_OK) {
        	    
            Uri contactData = intent.getData();
            Cursor c =  getContentResolver().query(contactData, null, null, null, null);
            if (c.moveToFirst()) {


                String id =c.getString(c.getColumnIndexOrThrow(ContactsContract.Contacts._ID));

                String hasPhone =c.getString(c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                
                if (hasPhone.equalsIgnoreCase("1")) {
                 Cursor phones = getContentResolver().query( 
                              ContactsContract.CommonDataKinds.Phone.CONTENT_URI,null, 
                              ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ id, 
                              null, null);
                    phones.moveToFirst();
                    contactNumber = phones.getString(phones.getColumnIndex("data1"));
                    
                }
                
                contactName = c.getString(c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                
                loadUrl(String.format("javascript:getSelectedContactNoFromAndroid('%s', '%s')" , contactName, contactNumber));
                
            }
          }
          
        }

         
    }
    
    private void setWebView(WebView webView) {

    	// For Chrome webview debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        	WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        webView.setScrollbarFadingEnabled(true);
        
        WebSettings myWebSettings = webView.getSettings();
        myWebSettings.setUserAgentString(
        		myWebSettings.getUserAgentString() 
        	    + " "
        	    + "FamilyNest_ANDROID_APP VERSION_1.0.0");
        myWebSettings.setJavaScriptEnabled(true);
        myWebSettings.setAllowFileAccess(true);
        myWebSettings.setAllowContentAccess(true);
        myWebSettings.setSupportZoom(false);
        
        webView.addJavascriptInterface(new WebToAndroidAppJavascriptInterface(this, this, webView), "Android");

        webView.setWebViewClient(new WebViewClient() {
        	

            //If you will not use this method url links are open in new brower not in webview
            public boolean shouldOverrideUrlLoading(WebView webView, String url) {
            	// Load all links in APPs webview
            	return false;
            }

            //Show loader on url load
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                
                if(flipper.getDisplayedChild() != 0){
                	if(!myProgressDialog.isShowing()){
            			myProgressDialog.show();
            			new java.util.Timer().schedule( 
        			        new java.util.TimerTask() {
        			            @Override
        			            public void run() {
        			            	//Close progressDialog
        		            		if(myProgressDialog.isShowing()){
        		            			myProgressDialog.dismiss();
        		                    }
        			            }
        			        }, 
        			        60*2000 
                        );
            		}

                }
            	            	                	
                super.onPageStarted(view, url, favicon);

            }
            
            // Called when all page resources loaded
            public void onPageFinished(WebView view, String url) {
            	
            	try {
                    //Close progressDialog
            		if(myProgressDialog.isShowing()){
            			myProgressDialog.dismiss();
                    }
                
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            	
            	if(flipper.getDisplayedChild() == 0){
            		
            		flipper.setDisplayedChild(1);
            	}
            	
            	super.onPageFinished(view, url);

            }
            
            @Override
            public void onLoadResource(WebView view, String url) {
            	super.onLoadResource(view, url);
            }

        });

        webView.setWebChromeClient(new WebChromeClient() {

            // general version
            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                createChooserForSimpleVersions(uploadMsg);
            }

            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                createChooserForSimpleVersions(uploadMsg);
            }

            //For Android 4.1 only
            protected void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType, String capture) {
                createChooserForSimpleVersions(uploadMsg);
            }

            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                
                fileUploadCallbackArray = filePathCallback;
                Intent cameraIntent = setCameraIntent();
                Intent galleryIntent = setGalleryIntent();
                Intent[] intentArray;
                if (cameraIntent != null) {
                    intentArray = new Intent[]{cameraIntent};
                } else {
                    intentArray = new Intent[0];
                }
                Intent chooserIntent = setChooserIntent(intentArray, galleryIntent);
                startActivityForResult(chooserIntent, FILECHOOSER_REQUESTCODE);

                return true;
            }

            public void createChooserForSimpleVersions(ValueCallback<Uri> uploadMsg) {
            	
                fileUploadCallback = uploadMsg;
                Intent cameraIntent = setCameraIntent();
                Intent galleryIntent = setGalleryIntent();
                Intent[] intentArray;
                if (cameraIntent != null) {
                    intentArray = new Intent[]{cameraIntent};
                } else {
                    intentArray = new Intent[0];
                }
                Intent chooserIntent = setChooserIntent(intentArray, galleryIntent);
                startActivityForResult(chooserIntent, FILECHOOSER_REQUESTCODE);
            }

        });

    }
    
    public static void setLayoutIfInternetAvailable(Context context, Activity activity) {
        
    	if(myWebView != null){
    		if (CommonMethods.isInternetAvailable(context)) {
                if(needToLoadUrl == true){
                	loadUrl(finalUrlToLoadInWebView);
                	needToLoadUrl = false;
                }
                if(internetAlert != null){
                	internetAlert.dismiss();
                }
                        
            } else {
            	loadUrl("file:///android_asset/no_internet_view.html");
            	createNetErrorDialog(context, activity);
            	        	        	
            	needToLoadUrl = true;
    	    }
            
    	}
    	
    }

    static void createNetErrorDialog( final Context context, final Activity activity) {
    	
    	if(activity != null){
    		activity.runOnUiThread(new Runnable() {
    		    public void run() {
    		    	if(alertDialog == null){
    	        		alertDialog = new Builder(activity);
    	        		alertDialog.setTitle("No Internet Connection.");
    	                alertDialog.setMessage("Please connect to Internet.");
    	                alertDialog.setPositiveButton("Wi-fi", new DialogInterface.OnClickListener(){

    	        			@Override
    	        			public void onClick(DialogInterface dialog, int which) {
    	        				dialog.dismiss();
    	        				Intent intent = new Intent("android.net.wifi.PICK_WIFI_NETWORK");
    	        				activity.startActivity(intent);
    	        	        }
    	                	
    	                });
    	                alertDialog.setNegativeButton("Mobile Data", new DialogInterface.OnClickListener(){

    	        			@Override
    	        			public void onClick(DialogInterface dialog, int which) { 
    	        				 dialog.dismiss();
    	        				 Intent intent = new Intent();
    	        		         intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.Settings$DataUsageSummaryActivity"));
    	        		         activity.startActivity(intent);
    	        	        }
    	                	
    	                });
    	                alertDialog.setNeutralButton("Cancel", new DialogInterface.OnClickListener(){

    	        			@Override
    	        			public void onClick(DialogInterface dialog, int which) {
    	        				 dialog.dismiss();
    	        			}
    	                	
    	                });
    	                
    	            }
    	    		
    	    		if(internetAlert == null){
    	    			internetAlert = alertDialog.create();
    	    		}
    	    		
    	    		if(!internetAlert.isShowing()){
    	    			internetAlert.show();
    	    		} 
    	    	}

    		});
        }

    }
    
    private Intent setCameraIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);


        File imageStorageDir = new File(
                Environment.getExternalStorageDirectory().toString(), "FamilyNest/Images");

        if (!imageStorageDir.exists()) {
            // Create KIDSS Folder at sdcard
            imageStorageDir.mkdirs();
        }

        // Create camera captured image file path and name
        File photoFile = new File(imageStorageDir, "FamilyNest_IMG_"+ String.valueOf(System.currentTimeMillis())
                        + ".jpg");

        cameraImagePath = "file://" +   photoFile.getAbsolutePath().toString();
        cameraImageUri = Uri.fromFile(photoFile);

        takePictureIntent.putExtra("PhotoPath", cameraImagePath);
        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);


        return takePictureIntent;

    }

    private Intent setGalleryIntent(){
        // Set up the intent to get an existing image
        Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
        contentSelectionIntent.setType("image/*");

        return contentSelectionIntent;
    }

    private Intent setChooserIntent(Intent[] intentArray, Intent galleryIntent){
        Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
        chooserIntent.putExtra(Intent.EXTRA_INTENT, galleryIntent);
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

        return chooserIntent;

    }
    
    public String getCurrentUrl (){
    	if(myWebView.getUrl() != null || myWebView.getUrl() != ""){
    		return null;
    	}else {
    		return myWebView.getUrl();
    	}
    }
    
    public static void loadUrl(final String url){
    	myWebView.post(new Runnable() {
    	    @Override
    	    public void run() {
    	    	myWebView.loadUrl(url);
    	    }
    	});

    }
    
    public void startGPSUpdatesInBackground(){
    	EventBroadcastReceiver.setGPSStatus(myContext);
    }
    
    public void stopGPSUpdatesInBackground(){
    	EventBroadcastReceiver.stopGPSUpdates();
    }
    
    public void setActivityRunning(boolean status){
    	SharedPreferences appRunningStatusPref = myContext.getSharedPreferences("AppRunningStatus", 0);
		boolean oldStatus = appRunningStatusPref.getBoolean("isAppRunning", false);
		if(oldStatus != status){
			SharedPreferences.Editor editor = appRunningStatusPref.edit();
			editor.putBoolean("isAppRunning", status);
			editor.apply();
		}
    }
    
    public static boolean isAppRunning(Context context){
    	SharedPreferences appRunningStatusPref = myContext.getSharedPreferences("AppRunningStatus", 0);
		boolean oldStatus = appRunningStatusPref.getBoolean("isAppRunning", false);
		return oldStatus;
	}
	

    
}
