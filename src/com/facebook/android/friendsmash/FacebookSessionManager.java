package com.facebook.android.friendsmash;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.facebook.Session;
import com.facebook.SessionState;

/**
 *  Class to manager the session state within an Activity - each Activity must then create an instance
 *  of this class and override the methods outlined below (in CAPITALS)
 *  Note: This is a convenient substitute for having to extend FacebookActivity if that does not work
 *  within your app design and to avoid having to put session logic directly into each Activity
 */
public class FacebookSessionManager {
	
	// Callback method on session changes - calls updateView
	Session.StatusCallback statusCallback = new SessionStatusCallback();

	// Activity that is using this FacebookSessionManager
	Activity activity;
	
	// Declare the SessionStateChangeListener
	SessionStateChangeListener sessionStateChangeListener;
	
	// ADD THE FOLLOWING LINE OF CODE TO DECLARE YOUR FACEBOOKSESSIONMANAGER OBJECT AS AN ATTRIBUTE
	// IN YOUR ACTIVITY:
	// FacebookSessionManager facebookSessionManager;
	
	// ADD THE FOLLOWING LINE OF CODE IN YOUR ACTIVITY CONSTRUCTOR TO INSTANTIATE YOUR FACEBOOKSESSIONMANAGER:
	//	facebookSessionManager = new FacebookSessionManager(this, new FacebookSessionManager.SessionStateChangeListener() {
	//		public void onSessionStateChange(SessionState state, Exception exception) {
	//			// Add code here to accommodate session changes
	//		}
	//	});
	public FacebookSessionManager(Activity activity, SessionStateChangeListener sessionStateChangeListener) {
		this.activity = activity;
		this.sessionStateChangeListener = sessionStateChangeListener;
	}
	
	// ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONCREATE METHOD IN YOUR ACTIVITY
	// AFTER CALLING SUPER.ONCREATE(...):
	// facebookSessionManager.onCreate(savedInstanceState);
    public void onCreate(Bundle savedInstanceState) {
        Session session = Session.getActiveSession();
        if (session == null) {
	        if (savedInstanceState != null) {
	            session = Session.restoreSession(activity, null, statusCallback, savedInstanceState);
	        }
	        if (session == null) {
            	session = new Session(activity);
            }
	        Session.setActiveSession(session);
        }

    	if (sessionStateChangeListener != null) {
    		sessionStateChangeListener.onSessionStateChange(null, null);
    	}
    }

    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSTART METHOD IN YOUR ACTIVITY
 	// AFTER CALLING SUPER.ONSTART(...):
 	// facebookSessionManager.onStart();
    public void onStart() {
        Session session = Session.getActiveSession();
        
        // The following checks if the Session state is in CREATED_TOKEN_LOADED, which means
        // the session data has been persisted in the token cache, so just call open which
        // opens the session using this session data (avoids an app switch or any user
        // interaction)
        // Note: This should go in either onCreate or onStart - we place it in onStart so
        // that the UI is fully loaded in the logged out state and this then causes the
        // onSessionStateChange callback to be called, which will then execute
        // the appropriate logic to switch to the login state properly
        if (session.getState().equals(SessionState.CREATED_TOKEN_LOADED)) {
            session.openForRead(new Session.OpenRequest(activity).setCallback(statusCallback));
        } else {
        	session.addCallback(statusCallback);
        }
    }

    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSTOP METHOD IN YOUR ACTIVITY
 	// AFTER CALLING SUPER.ONSTOP(...):
 	// facebookSessionManager.onStop();
    public void onStop() {
        Session.getActiveSession().removeCallback(statusCallback);
    }

    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONACTIVITYRESULT METHOD IN YOUR ACTIVITY
 	// AFTER CALLING SUPER.ONACTIVITYRESULT(...):
 	// facebookSessionManager.onActivityResult(requestCode, resultCode, data);
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Session.getActiveSession().onActivityResult(activity, requestCode, resultCode, data);
    }

    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSAVEINSTANCESTATE METHOD IN YOUR ACTIVITY
 	// AFTER CALLING SUPER.ONSAVEINSTANCESTATE(...):
 	// facebookSessionManager.onSaveInstanceState(outState);
    protected void onSaveInstanceState(Bundle outState) {
        Session session = Session.getActiveSession();
        Session.saveSession(session, outState);
    }
	
	private class SessionStatusCallback implements Session.StatusCallback {
        @Override
        public void call(Session session, SessionState state, Exception exception) {
        	if (sessionStateChangeListener != null) {
        		sessionStateChangeListener.onSessionStateChange(state, exception);
        	}
        }
    }
	
	public interface SessionStateChangeListener {
		public void onSessionStateChange(SessionState state, Exception exception);
	}
}
