package com.zvikabh.supermarket;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import android.os.AsyncTask;
import android.util.Log;

public class AuthTokenGetter extends AsyncTask<Void, Integer, String> {
	public AuthTokenGetter(String mUserEmail, String mScope,
			ShoppingListActivity mParentActivity) {
		this.mUserEmail = mUserEmail;
		this.mScope = mScope;
		this.mParentActivity = mParentActivity;
	}

	public final static int REQUEST_AUTHORIZATION = 998;
	
	@Override
	protected String doInBackground(Void... params) {
		try {
			Log.i("AuthTokenGetter", "Trying to get token for " + mUserEmail);
			Log.i("AuthTokenGetter", "Scope: " + mScope);
			String authToken = GoogleAuthUtil.getToken(mParentActivity,
					mUserEmail, mScope);
			Log.i("AuthTokenGetter", "Got token!");
			return authToken;
		} catch (UserRecoverableAuthException e) {
			mParentActivity.startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
		} catch (Exception e) {
			Log.e("AuthTokenGetter", "Failed to fetch token", e);
		}
		return null;
	}

	@Override
	protected void onPostExecute(String authToken) {
		try {
			if (authToken != null) {
				mParentActivity.setAuthToken(authToken);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	String mUserEmail;
	String mScope;
	ShoppingListActivity mParentActivity;
}
