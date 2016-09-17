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

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.widget.Toast;

public class CommonMethods {
	
	public static File downloadImage(final String fileName, final String URL, Context context) {
		File imageFile = null;
		if(isInternetAvailable(context)){
			Toast.makeText(context, "Downloading image. Please wait...", Toast.LENGTH_SHORT).show();
			File imageStorageDir = new File(
	                Environment.getExternalStorageDirectory().toString()
	                , "FamilyNest/Images");

	        if (!imageStorageDir.exists()) {
	            // Create FamilyNest Folder at sdcard
	            imageStorageDir.mkdirs();
	        }

	        
	        
	        if(fileName == "" || fileName == null){
	        	imageFile = new File(imageStorageDir, "FamilyNest_IMG_"+ String.valueOf(System.currentTimeMillis())
	            + ".jpg");
	        } else {
	        	imageFile = new File(imageStorageDir, "FamilyNest_IMG_" + fileName);
	        }
	        // Create camera captured image file path and name
	        
	        if(!imageFile.exists()){
	        	
        		Bitmap bitmap = null;
    			InputStream in = null;
    			
    	    	try {
            		
        			in = CommonMethods.downloadInputStream(URL);
        			if(in != null){
        				bitmap = BitmapFactory.decodeStream(in);
            			in.close();
            			try {
                			OutputStream outStream = new FileOutputStream(imageFile);
                			bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                			outStream.flush();
                			outStream.close();
                		} catch (Exception e) {
                			e.printStackTrace();
                		}

                    	ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    	bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);

        			}else{
        				imageFile = null;
        			}
        			
        		} catch (IOException e1) {
        			
        			e1.printStackTrace();
        		}
        		
        	}
		
		}
		
		return imageFile;
	}
	
	public static InputStream downloadInputStream(String urlString) throws IOException {
		InputStream in = null;
		int response = -1;

		URL url = new URL(urlString);
		URLConnection conn = url.openConnection();

		if (!(conn instanceof HttpURLConnection))
			throw new IOException("Not an HTTP connection");

		try {
			HttpURLConnection httpConn = (HttpURLConnection) conn;
			httpConn.setAllowUserInteraction(false);
			httpConn.setInstanceFollowRedirects(true);
			httpConn.setRequestMethod("GET");
			httpConn.connect();

			response = httpConn.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK) {
				in = httpConn.getInputStream();
			}
		} catch (Exception ex) {
			throw new IOException("Error connecting");
		}
		return in;
	}
	
	public static void setInternetStatus(Context context, boolean status){
		SharedPreferences intenetStatusPref = context.getSharedPreferences("InternetStatus", Context.MODE_PRIVATE);
		Boolean oldStatus = intenetStatusPref.getBoolean("isInternetAvailable", false);
		if(oldStatus != status){
			SharedPreferences.Editor editor = intenetStatusPref.edit();
			editor.putBoolean("isInternetAvailable", status);
			editor.apply();
		}
		
	}
	
	public static void setGPSStatus(Context context, boolean status){
		
		SharedPreferences GPSStatusPref = context.getSharedPreferences("GPSStatus", Context.MODE_PRIVATE);
		Boolean oldStatus = GPSStatusPref.getBoolean("isGPSAvailable", false);
		
		if(oldStatus != status){
			SharedPreferences.Editor editor = GPSStatusPref.edit();
			editor.putBoolean("isGPSAvailable", status);
			editor.apply();
		}
	}
	
	public static boolean isGPSAvailable(Context context){
		SharedPreferences GPSStatusPref = context.getSharedPreferences("GPSStatus", Context.MODE_PRIVATE);
		Boolean oldStatus = GPSStatusPref.getBoolean("isGPSAvailable", false);
		return oldStatus;
	}
    
	public static boolean isInternetAvailable(Context context){
		SharedPreferences intenetStatusPref = context.getSharedPreferences("InternetStatus", Context.MODE_PRIVATE);
		Boolean oldStatus = intenetStatusPref.getBoolean("isInternetAvailable", false);
		return oldStatus;
	}
	
	public static void setCurrentUserId(Context context, int currentUserId){
		SharedPreferences currentUserIdPref = context.getSharedPreferences("currentUserId", Context.MODE_PRIVATE);
		int oldCurrentUserId = currentUserIdPref.getInt("currentUserId", -1);
		if(oldCurrentUserId != currentUserId){
			SharedPreferences.Editor editor = currentUserIdPref.edit();
			editor.putInt("currentUserId", currentUserId);
			editor.apply();
		}
	}
}
