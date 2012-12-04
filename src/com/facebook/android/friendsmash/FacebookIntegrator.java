package com.facebook.android.friendsmash;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.facebook.FacebookException;
import com.facebook.FacebookOperationCanceledException;
import com.facebook.FacebookRequestError;
import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.android.friendsmash.FetchBitmapTask.FetchBitmapListener;
import com.facebook.model.GraphObject;
import com.facebook.model.GraphUser;
import com.facebook.widget.WebDialog;

/**
 *  Most of the Facebook integration methods are defined in here to separate them
 *  out from the game logic.  The main exception to this is the login/logout logic
 *  which is implemented within the Activities and Fragments themselves (due to the
 *  requirement to override a number of methods) - however, the FacebookSessionManager
 *  is used to abstract out much of the session management logic.
 */
public class FacebookIntegrator {

	
	// Fetch logged in user's friends and their own information - called from HomeActivity after
	// the session state has been checked
	protected void fetchUserInformation(final HomeActivity homeActivity) {
		final Session session = Session.getActiveSession();
		// Get the user's list of friends
		Request friendsRequest = Request.newMyFriendsRequest(session, new Request.GraphUserListCallback() {

			@Override
			public void onCompleted(List<GraphUser> users, Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					homeActivity.showErrorAndLogout(homeActivity.getResources().getString(R.string.network_error));
				} else if (session == Session.getActiveSession()) {
					if (users != null) {
						// Set the friends attribute
						((FriendSmashApplication)homeActivity.getApplication()).setFriends(users);
					}
				}
			}
		});
		
		// Get current logged in user information
		Request meRequest = Request.newMeRequest(session, new Request.GraphUserCallback() {
			
			@Override
			public void onCompleted(GraphUser user, Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					homeActivity.showErrorAndLogout(homeActivity.getResources().getString(R.string.network_error));
				} else if (session == Session.getActiveSession()) {
					if (user != null) {
						// Set the currentFBUser attribute
						((FriendSmashApplication)homeActivity.getApplication()).setCurrentFBUser(user);
						
						if (homeActivity.shouldSwitchToHomeFragment == true) {
							homeActivity.loadPersonalizedFragment();
						}
					}
					else {
						homeActivity.showErrorAndLogout("Error fetching your user profile - Please try again");
					}
				}
				
				homeActivity.endFetchUserInformation();
			}
		});
		
		// Execute the batch of requests asynchronously
		Request.executeBatchAsync(friendsRequest, meRequest);
	}
	
	
	// Fetch the image of the selected user's friend and start firing images of this friend
	protected void fetchFriendBitmapAndFireImages(final GameFragment gameFragment,
			final UserImageView userImageView, final String friendToSmashID, final boolean extraImage) {
		new FetchBitmapTask(new FetchBitmapListener() {
        	public void onBitmapFetched(Bitmap bitmap) {
        		gameFragment.friendToSmashBitmap = bitmap;
        		
        		// Hide the spinner while retrieving
        		gameFragment.progressContainer.setVisibility(View.INVISIBLE);
				
				if (gameFragment.friendToSmashBitmap != null) {
					gameFragment.setFriendImageAndFire(userImageView, gameFragment.friendToSmashBitmap, extraImage);
                	
                	// Also set the lastFriendSmashedID in the application
                	((FriendSmashApplication) gameFragment.getActivity().getApplication()).setLastFriendSmashedID(friendToSmashID);
                } else {
                	gameFragment.closeAndShowError(gameFragment.getResources().getString(R.string.network_error));
                }
        	}
        }).execute("http://graph.facebook.com/" + friendToSmashID +
        		"/picture?width=" + gameFragment.iconWidth + "&height=" + gameFragment.iconWidth);
	}
	
	// Fires the first image in a game with a given request id  (from a user deep linking by clicking
	// on a request from a specific user)
	protected void fireFirstImageWithRequestID(final GameFragment gameFragment, String requestID) {
		final Session session = Session.getActiveSession();
		Request requestIDGraphPathRequest = Request.newGraphPathRequest(session, requestID, new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					gameFragment.closeAndShowError(gameFragment.getResources().getString(R.string.network_error));
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Extract the user id from the response
						GraphObject graphObject = response.getGraphObject();
						JSONObject fromObject = (JSONObject)graphObject.getProperty("from");
						try {
							gameFragment.friendToSmashIDProvided = fromObject.getString("id");
						} catch (JSONException e) {
							Log.e(FriendSmashApplication.TAG, e.toString());
							gameFragment.closeAndShowError(gameFragment.getResources().getString(R.string.network_error));
						}
						
						// With the user id, fetch and set their name
						Request userGraphPathRequest = Request.newGraphPathRequest(session, gameFragment.friendToSmashIDProvided, new Request.Callback() {

							@Override
							public void onCompleted(Response response) {
								FacebookRequestError error = response.getError();
								if (error != null) {
									Log.e(FriendSmashApplication.TAG, error.toString());
									gameFragment.closeAndShowError(gameFragment.getResources().getString(R.string.network_error));
								} else if (session == Session.getActiveSession()) {
									if (response != null) {
										// Extract the user name from the response
										GraphObject graphObject = response.getGraphObject();
										gameFragment.friendToSmashFirstName = (String)graphObject.getProperty("first_name");
									}
									if (gameFragment.friendToSmashFirstName != null) {
										// If the first name of the friend to smash has been set, set the text in the smashPlayerNameTextView
										// and hide the progress spinner now that the user's details have been fetched
										gameFragment.progressContainer.setVisibility(View.INVISIBLE);
										gameFragment.setSmashPlayerNameTextView();
										
										// Now you're ready to fire the first image
										gameFragment.spawnImage(false);
									}
								}
							}
						});
						Request.executeBatchAsync(userGraphPathRequest);
					}
				}
			}
		});
		Request.executeBatchAsync(requestIDGraphPathRequest);
	}
	
	// Fires the first image in a game with a given user id  (from a user deep linking by clicking
	// on a feed post from a specific user)
	protected void fireFirstImageWithUserID(final GameFragment gameFragment, String userID) {
		final Session session = Session.getActiveSession();
		
		// With the user id, fetch and set their name, then start the firing of images
		gameFragment.friendToSmashIDProvided = userID;
		Request userGraphPathRequest = Request.newGraphPathRequest(session, gameFragment.friendToSmashIDProvided, new Request.Callback() {

			@Override
			public void onCompleted(Response response) {
				FacebookRequestError error = response.getError();
				if (error != null) {
					Log.e(FriendSmashApplication.TAG, error.toString());
					gameFragment.closeAndShowError(gameFragment.getResources().getString(R.string.network_error));
				} else if (session == Session.getActiveSession()) {
					if (response != null) {
						// Extract the user name from the response
						GraphObject graphObject = response.getGraphObject();
						gameFragment.friendToSmashFirstName = (String)graphObject.getProperty("first_name");
					}
					if (gameFragment.friendToSmashFirstName != null) {
						// If the first name of the friend to smash has been set, set the text in the smashPlayerNameTextView
						// and hide the progress spinner now that the user's details have been fetched
						gameFragment.progressContainer.setVisibility(View.INVISIBLE);
						gameFragment.setSmashPlayerNameTextView();
						
						// Now you're ready to fire the first image
						gameFragment.spawnImage(false);
					}
				}
			}
		});
		Request.executeBatchAsync(userGraphPathRequest);
	}
	
	
	// Pop up a request dialog for the user to invite their friends to smash them back in Friend Smash
	protected void sendRequest(final HomeFragment homeFragment) {
    	Bundle params = new Bundle();
    	
    	// Uncomment following link once uploaded on Google Play for deep linking
    	// params.putString("link", "https://play.google.com/store/apps/details?id=com.facebook.android.friendsmash");
    	
    	// 1. No additional parameters provided - enables generic Multi-friend selector
    	params.putString("message", "I just smashed " + homeFragment.application.getScore() + " friends! Can you beat it?");
    	
    	// 2. Optionally provide a 'to' param to direct the request at a specific user
//    	params.putString("to", "515768651");
    	
    	// 3. Suggest friends the user may want to request - could be game specific
    	// e.g. players you are in a match with, or players who recently played the game etc.
    	// Normally this won't be hardcoded as follows but will be context specific
//		    	String [] suggestedFriends = {
//		    			"695755709",
//		    			"685145706",
//		    			"569496010",
//		    			"100003900225427",
//		    			"100003963180539"
//		    	};
//		    	params.putString("suggestions", Utility.joinArrayOfStrings(suggestedFriends, ","));
//		    	
    	// Show FBDialog without a notification bar
    	showDialogWithoutNotificationBar(homeFragment, "apprequests", params);
	}
	
	// Pop up a filtered request dialog for the user to invite their friends that have Android devices
	// to smash them back in Friend Smash
	protected void sendFilteredRequest(final HomeFragment homeFragment) {
		// Okay, we're going to filter our friends by their device, we're looking for friends with an Android device
		
		// Get a list of the user's friends' names and devices
		final Session session = Session.getActiveSession();
		Request friendDevicesGraphPathRequest = Request.newGraphPathRequest(session,
				"me/friends",
				new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
						FacebookRequestError error = response.getError();
						if (error != null) {
							Log.e(FriendSmashApplication.TAG, error.toString());
							((HomeActivity)homeFragment.getActivity()).showError(homeFragment.getResources().getString(R.string.network_error));
						} else if (session == Session.getActiveSession()) {
							if (response != null) {
								// Get the result
								GraphObject graphObject = response.getGraphObject();
								JSONArray dataArray = (JSONArray)graphObject.getProperty("data");
								
								if (dataArray.length() > 0) {
									// Ensure the user has at least one friend ...
									
									// Store the filtered friend ids in the following Vector
									Vector<String> filteredFriendIDs = new Vector<String>();
									
									for (int i=0; i<dataArray.length(); i++) {
										JSONObject currentUser = dataArray.optJSONObject(i);
										if (currentUser != null) {
											JSONArray currentUserDevices = currentUser.optJSONArray("devices");
											if (currentUserDevices != null) {
												// The user has at least one (mobile) device logged into Facebook
												for (int j=0; j<currentUserDevices.length(); j++) {
													JSONObject currentUserDevice = currentUserDevices.optJSONObject(j);
													if (currentUserDevice != null) {
														String currentUserDeviceOS = currentUserDevice.optString("os");
														if (currentUserDeviceOS != null) {
															if (currentUserDeviceOS.equals("Android")) {
																filteredFriendIDs.add(currentUser.optString("id"));
															}
														}
													}
												}
											}
										}
									}
									
									// Now we have a list of friends with an Android device, we can send requests to them
							    	Bundle params = new Bundle();
							    	
							    	// Uncomment following link once uploaded on Google Play for deep linking
							    	// params.putString("link", "https://play.google.com/store/apps/details?id=com.facebook.android.friendsmash");
							    	
							    	// We create our parameter dictionary as we did before
							    	params.putString("message", "I just smashed " + homeFragment.application.getScore() + " friends! Can you beat it?");
							    	
							    	// We have the same list of suggested friends
							    	String [] suggestedFriends = {
							    			"695755709",
							    			"685145706",
							    			"569496010",
							    			"100003900225427",
							    			"100003963180539"
							    	};
									    	
							    	// Of course, not all of our suggested friends will have Android devices - we need to filter them down
							    	Vector<String> validSuggestedFriends = new Vector<String>();
				             
				                    // So, we loop through each suggested friend
				                    for (String suggestedFriend : suggestedFriends)
				                    {
				                        // If they are on our device filtered list, we know they have an Android device
				                        if (filteredFriendIDs.contains(suggestedFriend))
				                        {
				                            // So we can call them valid
				                        	validSuggestedFriends.add(suggestedFriend);
				                        }
				                    }
				                    params.putString("suggestions", Utility.joinArrayOfStrings(validSuggestedFriends.toArray(new String[validSuggestedFriends.size()]), ","));
									    	
							    	// Show FBDialog without a notification bar
							    	showDialogWithoutNotificationBar(homeFragment, "apprequests", params);
								}
							}
						}
					}});
		// Pass in the fields as extra parameters, then execute the Request
		Bundle extraParamsBundle = new Bundle();
		extraParamsBundle.putString("fields", "name,devices");
		friendDevicesGraphPathRequest.setParameters(extraParamsBundle);
		Request.executeBatchAsync(friendDevicesGraphPathRequest);
	}
	
	// Pop up a feed dialog for the user to brag to their friends about their score and to offer
	// them the opportunity to smash them back in Friend Smash
	protected void sendBrag(final HomeFragment homeFragment) {
		// Show a feed dialog to the user to brag on their Facebook timeline and newsfeed
		// This will be using the dialog web views as supported in the old SDK
    	Bundle params = new Bundle();
    	
    	// This first parameter is used for deep linking so that anyone who clicks the link will start smashing this user
    	// who sent the post
    	GraphUser currentFBUser = homeFragment.application.getCurrentFBUser();
    	if (currentFBUser != null) {
    		params.putString("link", "https://www.friendsmash.com/challenge_brag_" +
    				currentFBUser.getId());
    	}
    	
    	params.putString("name", "Checkout my Friend Smash greatness!");
    	params.putString("caption", "Come smash me back!");
    	params.putString("description", "I just smashed " + homeFragment.application.getScore() + " friends! Can you beat my score?");
    	params.putString("picture", "http://www.friendsmash.com/images/logo_large.jpg");
    	
    	// Show FBDialog without a notification bar
    	showDialogWithoutNotificationBar(homeFragment, "feed", params);
	}
	
	// Show a dialog (feed or request) without a notification bar (i.e. full screen)
	protected void showDialogWithoutNotificationBar(final HomeFragment homeFragment, String action, Bundle params) {
		// Create the dialog
		homeFragment.dialog = new WebDialog.Builder(homeFragment.getActivity(), Session.getActiveSession(), action, params).setOnCompleteListener(
				new WebDialog.OnCompleteListener() {
			
			@Override
			public void onComplete(Bundle values, FacebookException error) {
				if (error != null && !(error instanceof FacebookOperationCanceledException)) {
					((HomeActivity)homeFragment.getActivity()).showError(homeFragment.getResources().getString(R.string.network_error));
				}
				homeFragment.dialog = null;
				homeFragment.dialogAction = null;
				homeFragment.dialogParams = null;
			}
		}).build();
		
		// Hide the notification bar and resize to full screen
		Window dialog_window = homeFragment.dialog.getWindow();
    	dialog_window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	
    	// Store the dialog information in attributes
    	homeFragment.dialogAction = action;
    	homeFragment.dialogParams = params;
    	
    	// Show the dialog
    	homeFragment.dialog.show();
	}
	
	
	// Post score to Facebook
	protected void postScore(final HomeFragment homeFragment) {
		final int score = homeFragment.application.getScore();
		if (score > 0) {
			// Only post the score if they smashed at least one friend!
			
			// First check to make sure this is the highest score the user has posted to Facebook
			// by getting their current score from Facebook
			final Session session = Session.getActiveSession();
			Request userScoreGraphPathRequest = Request.newGraphPathRequest(session,
					"me/score",
					new Request.Callback() {
						@Override
						public void onCompleted(Response response) {
							FacebookRequestError error = response.getError();
							if (error != null) {
								Log.e(FriendSmashApplication.TAG, error.toString());
								((HomeActivity)homeFragment.getActivity()).showError(homeFragment.getResources().getString(R.string.network_error));
							} else if (session == Session.getActiveSession()) {
								if (response != null) {
									// We have a response, so store the retrieved score in the following attribute
									int fetchedUserScore = -1;
									
									// Get the result
									GraphObject graphObject = response.getGraphObject();
									JSONArray dataArray = (JSONArray)graphObject.getProperty("data");
									
									if (dataArray.length() > 0) {
										JSONObject userObject = dataArray.optJSONObject(0);
										if (userObject != null) {
											String fetchedUserScoreAsString = userObject.optString("score");
											if (fetchedUserScoreAsString != null) {
												fetchedUserScore = Integer.parseInt(fetchedUserScoreAsString);
											}
										}
									}
									
									// Ensure a user score was fetched successfully
									if (fetchedUserScore >= 0) {
										// Now test the fetched user score against their new score
										if (score > fetchedUserScore) {
											// They have a new high score, so post it
											// Now post the score
											Bundle params = new Bundle();
											params.putString("score", "" + score);
											Request postScoreRequest = new Request(Session.getActiveSession(),
													"me/scores",
													params,
								                    HttpMethod.POST,
								                    new Request.Callback() {
	
														@Override
														public void onCompleted(Response response) {
															FacebookRequestError error = response.getError();
															if (error != null) {
																Log.e(FriendSmashApplication.TAG, "Posting Score failed: " + error.getErrorMessage());
																((HomeActivity)homeFragment.getActivity()).showError("Posting score failed");
															} else {
																Log.i(FriendSmashApplication.TAG, "Score posted successfully");
															}
														}
													});
											Request.executeBatchAsync(postScoreRequest);
										} else {
											Log.e(FriendSmashApplication.TAG, "Score not posted as user has already scored higher (" + fetchedUserScore + " points)");
										}
									} else {
										Log.e(FriendSmashApplication.TAG, "Score not posted as failed to fetch current score");
										((HomeActivity)homeFragment.getActivity()).showError("Posting score failed - " +
										homeFragment.getResources().getString(R.string.network_error));
									}
								}
							}
						}});
			Request.executeBatchAsync(userScoreGraphPathRequest);
		}
	}
	
	// Post achievement to Facebook
	protected void postAchievement(final HomeFragment homeFragment) {
		int score = homeFragment.application.getScore();
		String achievementURL = null;
		if (score >=200) {
			achievementURL = "http://www.friendsmash.com/opengraph/achievement_200.html";
		} else if (score >=150) {
			achievementURL = "http://www.friendsmash.com/opengraph/achievement_150.html";
		} else if (score >=100) {
			achievementURL = "http://www.friendsmash.com/opengraph/achievement_100.html";
		} else if (score >=50) {
			achievementURL = "http://www.friendsmash.com/opengraph/achievement_50.html";
		}
		if (achievementURL != null) {
			// Only post the relevant achievement if the user has achieved one
			Bundle params = new Bundle();
			params.putString("achievement", achievementURL);
			Request postScoreRequest = new Request(Session.getActiveSession(),
					"me/achievements",
					params,
                    HttpMethod.POST,
                    new Request.Callback() {

						@Override
						public void onCompleted(Response response) {
							FacebookRequestError error = response.getError();
							if (error != null) {
								Log.e(FriendSmashApplication.TAG, "Posting Achievement failed: " + error.getErrorMessage());
							} else {
								Log.i(FriendSmashApplication.TAG, "Achievement posted successfully");
							}
						}
					});
			Request.executeBatchAsync(postScoreRequest);
		}
	}
	
	// Post custom OG action to Facebook
	protected void postOG(final HomeFragment homeFragment) {
		if (homeFragment.application.getLastFriendSmashedID() != null) {
			Bundle params = new Bundle();
			params.putString("profile", homeFragment.application.getLastFriendSmashedID());
			Request postOGRequest = new Request(Session.getActiveSession(),
					"me/friendsmashsample:smash",
					params,
                    HttpMethod.POST,
                    new Request.Callback() {

						@Override
						public void onCompleted(Response response) {
							String id = "";
							FacebookRequestError error = response.getError();
							if (error != null) {
								Log.e(FriendSmashApplication.TAG, "Posting OG story failed: " + error.getErrorMessage());
								((HomeActivity)homeFragment.getActivity()).showError("Posting OG story failed");
							} else {
								GraphObject graphObject = response.getGraphObject();
								if (graphObject != null) {
									id = (String) graphObject.getProperty("id");
									Log.i(FriendSmashApplication.TAG, "OG action posted successfully: " + id);
								}
							}
						}
					});
			Request.executeBatchAsync(postOGRequest);
		}
	}
	
	// Fetch a Vector of ScoreboardEntry objects with the scores and details
	// of the user and their friends' scores who have played FriendSmash
	protected void fetchScoreboardEntries (final ScoreboardFragment scoreboardFragment) {
		// Fetch the scores ...
		final Session session = Session.getActiveSession();
		Request scoresGraphPathRequest = Request.newGraphPathRequest(session,
				FriendSmashApplication.appID + "/scores",
				new Request.Callback() {
					@Override
					public void onCompleted(Response response) {
						FacebookRequestError error = response.getError();
						if (error != null) {
							Log.e(FriendSmashApplication.TAG, error.toString());
							scoreboardFragment.closeAndShowError(scoreboardFragment.getResources().getString(R.string.network_error));
						} else if (session == Session.getActiveSession()) {
							// Instantiate the scoreboardEntriesVector
							Vector<ScoreboardEntry> scoreboardEntriesVector = new Vector<ScoreboardEntry>();
							
							if (response != null) {
								
								// Get the results
								GraphObject graphObject = response.getGraphObject();
								JSONArray dataArray = (JSONArray)graphObject.getProperty("data");
								
								if (dataArray.length() > 0) {
									
									// Loop through all users that have been retrieved
									for (int i=0; i<dataArray.length(); i++) {
										// Store the user details in the following attributes
										String userID = null;
										String userName = null;
										int userScore = -1;
										
										// Extract the user information
										JSONObject currentUser = dataArray.optJSONObject(i);
										if (currentUser != null) {
											JSONObject userObject = dataArray.optJSONObject(i);
											if (userObject != null) {
												JSONObject userInfo = userObject.optJSONObject("user");
												if (userInfo != null) {
													userID = userInfo.optString("id");
													userName = userInfo.optString("name");
												}
												String fetchedScoreAsString = userObject.optString("score");
												if (fetchedScoreAsString != null) {
													userScore = Integer.parseInt(fetchedScoreAsString);
												}
												if (userID != null && userName != null && userScore >= 0) {
													// All attributes have been successfully fetched, so create a new
													// ScoreboardEntry and add it to the Vector
													ScoreboardEntry currentUserScoreboardEntry =
															new ScoreboardEntry(userID, userName, userScore);
													scoreboardEntriesVector.add(currentUserScoreboardEntry);
												}
											}
										}
									}
								}
							}
							
							// Now that all scores should have been fetched and added to the scoreboardEntriesVector, sort it,
							// set it within scoreboardFragment and then callback to scoreboardFragment to populate the scoreboard
							Comparator<ScoreboardEntry> comparator = Collections.reverseOrder();
							Collections.sort(scoreboardEntriesVector, comparator);
							scoreboardFragment.application.scoreboardEntriesVector = scoreboardEntriesVector;
							scoreboardFragment.populateScoreboard();
						}
					}});
		Request.executeBatchAsync(scoresGraphPathRequest);
	}
	
}
