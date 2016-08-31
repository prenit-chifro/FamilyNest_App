package app.android.family.location.emergency.safety.familynest;

import java.io.File;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class MediaScanner implements MediaScannerConnectionClient {

	private MediaScannerConnection mMs;
    private File mFile;
    private Activity myActivity;

    public MediaScanner(Activity activity, File imageFile) {
    	mFile = imageFile;
        myActivity = activity;
        mMs = new MediaScannerConnection(myActivity.getApplicationContext(), this);
        mMs.connect();
	}

	@Override
	public void onMediaScannerConnected() {
		mMs.scanFile(mFile.getAbsolutePath(), null);
	}

	@Override
	public void onScanCompleted(String path, Uri uri) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(uri);
        myActivity.startActivity(intent);
        mMs.disconnect();
		
	}

}
