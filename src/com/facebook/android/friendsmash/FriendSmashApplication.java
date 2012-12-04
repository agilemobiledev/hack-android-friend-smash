package com.facebook.android.friendsmash;

import java.util.List;
import java.util.Vector;

import android.app.Application;

import com.facebook.model.GraphUser;

/**
 *  Use a custom Application class to pass state data between Activities.
 */
public class FriendSmashApplication extends Application {

	/* Static Attributes */
	
	// Tag used when logging all messages with the same tag (e.g. for demoing purposes)
    public static final String TAG = "FriendSmash";
	
	// Switch between the non-social and social Facebook versions of the game
	public static final boolean isSocial = true;
	
	// Facebook App ID for FriendSmash
	public static final String appID = "480369938658210";

	
	/* Friend Smash application attributes */
	
	// Player's current score
	private int score = -1;
	
	
	/* Facebook application attributes */
	
	// FacebookIntegrator used for much of the Facebook integration
	protected FacebookIntegrator facebookIntegrator = new FacebookIntegrator();
	
	// Logged in status of the user
	private boolean loggedIn = false;
	
	// Current logged in FB user
	private GraphUser currentFBUser;
	
	// List of the logged in user's friends
	private List<GraphUser> friends;
		
	// ID of the last friend smashed (linked to the current score)
	private String lastFriendSmashedID = null;
	
	// Vector of ordered ScoreboardEntry objects in order from highest to lowest score to
	// be shown in the ScoreboardFragment
	Vector<ScoreboardEntry> scoreboardEntriesVector = null;
		

	/* Friend Smash application attribute getters & setters */
	
	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	
	/* Facebook attribute getters & setters */
	
	public boolean isLoggedIn() {
		return loggedIn;
	}

	public void setLoggedIn(boolean loggedIn) {
		this.loggedIn = loggedIn;
		if (loggedIn == false) {
			// If the user is logged out, reset the score
			setScore(-1);
		}
	}

	public GraphUser getCurrentFBUser() {
		return currentFBUser;
	}

	public void setCurrentFBUser(GraphUser currentFBUser) {
		this.currentFBUser = currentFBUser;
	}

	public List<GraphUser> getFriends() {
		return friends;
	}

	public void setFriends(List<GraphUser> friends) {
		this.friends = friends;
	}

	public String getLastFriendSmashedID() {
		return lastFriendSmashedID;
	}

	public void setLastFriendSmashedID(String lastFriendSmashedID) {
		this.lastFriendSmashedID = lastFriendSmashedID;
	}
	
}
