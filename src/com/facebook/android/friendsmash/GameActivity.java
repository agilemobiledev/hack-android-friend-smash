package com.facebook.android.friendsmash;

import android.support.v4.app.Fragment;

/**
 *  Activity used once a user starts a new game - all logic is within GameFragment
 */
public class GameActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new GameFragment();
	}
	
}
