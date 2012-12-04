package com.facebook.android.friendsmash;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;

/**
 *  AsyncTask used to fetch a bitmap in the background - used by GameFragment to fetch bitmaps of
 *  the friend that the user is to smash in a new game
 */
public class FetchBitmapTask extends AsyncTask<String, Void, Bitmap> {
	
	private FetchBitmapListener listener;
	
	public interface FetchBitmapListener {
		public void onBitmapFetched(Bitmap bitmap);
	}
	
	public FetchBitmapTask(FetchBitmapListener listener) {
		this.listener = listener;
	}
	
    protected void onPreExecute() {
    }

    protected void onPostExecute(Bitmap bitmap) {
    	if (listener != null) {
    		// Call the onBitmapFetched method after the bitmap has been fetched
    		// This method will be defined in the created listener interface when
    		// one instantiates an instance of this class
			listener.onBitmapFetched(bitmap);
		}
    }

	@Override
	protected Bitmap doInBackground(String... urls) {
		
		Bitmap bitmap = null;
		
		// Attempt to fetch the bitmap on this background AsyncTask
		try {
			if (urls != null) {
				URL bitmapURL = new URL(urls[0]);
				bitmap = BitmapFactory.decodeStream(bitmapURL.openConnection().getInputStream());
			}
		} catch (MalformedURLException e)
		{
			// Unknown error
			Log.i("MalformedURLException", e.toString());
		}
    	catch (IOException e)
		{
			// Unable to get user profile pic
    		Log.i("IOException", e.toString());
		}
		
		return bitmap;
	}
}
