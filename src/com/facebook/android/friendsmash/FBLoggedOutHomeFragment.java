package com.facebook.android.friendsmash;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.widget.LoginButton;
import com.facebook.widget.LoginButton.OnErrorListener;

/**
 *  Fragment to be displayed if the user is logged out of Facebook in the social version of the game
 */
public class FBLoggedOutHomeFragment extends Fragment {
	
	View progressContainer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		// Hide the notification bar
		getActivity().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {

		View v = inflater.inflate(R.layout.fragment_home_fb_logged_out, parent, false);
		
		progressContainer = v.findViewById(R.id.progressContainer);
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Set an error listener for the login button
		LoginButton loginButton = (LoginButton) v.findViewById(R.id.loginButton);
		if (loginButton != null) {
			loginButton.setOnErrorListener(new OnErrorListener() {
	
				@Override
				public void onError(FacebookException error) {
					if (error != null && !(error instanceof FacebookOperationCanceledException)) {
						// Failed probably due to network error (rather than user canceling dialog which would throw a FacebookOperationCanceledException)
						((HomeActivity)getActivity()).showError(getResources().getString(R.string.network_error));
					}
				}
				
			});
		}
		
		return v;
	}

}
