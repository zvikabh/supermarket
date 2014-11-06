package com.zvikabh.supermarket;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import android.accounts.AccountManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.common.AccountPicker;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.util.ServiceException;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

public class ShoppingListActivity extends ActionBarActivity implements
		OnClickListener {

	static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
	static final int REQUEST_CODE_ADD_PRODUCT = 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_shopping_list);

		mScanButton = (Button) findViewById(R.id.button_scan);
		mListView = (ListView) findViewById(R.id.listView1);

		mScanButton.setOnClickListener(this);

		if (savedInstanceState == null) {
			// Activity is loading for the first time. Get auth token and load db.
			loadAuthToken();
		} else {
			// Activity is restarting. Get auth token and barcode db from saved state.
			loadInstanceState(savedInstanceState);
		}
		mListView.setAdapter(mShoppingList);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.add_product, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onSaveInstanceState(Bundle outBundle) {
		outBundle.putString("mUserEmail", mUserEmail);
		outBundle.putString("mAuthToken", mAuthToken);
		outBundle.putSerializable("mProductDb", mProductDb);
		outBundle.putSerializable("mShoppingList", mShoppingList);
	    super.onSaveInstanceState(outBundle);
	}
	
	protected void loadInstanceState(Bundle inBundle) {
		mUserEmail = inBundle.getString("mUserEmail");
		mAuthToken = inBundle.getString("mAuthToken");
		mProductDb = (AbstractProductDatabase) inBundle.getSerializable("mProductDb");
		mShoppingList = (ShoppingList) inBundle.getSerializable("mShoppingList");
	}

	protected Button mScanButton;
	private ListView mListView;
	private String mUserEmail;
	private String mAuthToken;
	private AbstractProductDatabase mProductDb;
	private ShoppingList mShoppingList;

	@Override
	public void onClick(View view) {
		IntentIntegrator integrator = new IntentIntegrator(this);
		integrator.initiateScan();
	}

	protected void addProductFromBarcode(String barcode) {
		if (mProductDb == null) {
			Toast.makeText(this, "Product DB unavailable.", Toast.LENGTH_LONG).show();
			return;
		}
		if (mShoppingList == null) {
			Toast.makeText(this, "Shopping list not loaded yet.", Toast.LENGTH_LONG).show();
			return;
		}
		Product product = mProductDb.get(barcode);
		if (product == null) {
			launchNewProductActivity(barcode);
			return;
		}
		Log.i("ShoppingListActivity", "Adding product: " + product.toString());
		mShoppingList.addItem(product, 1);
	}

	private void launchNewProductActivity(String barcode) {
		if (mProductDb == null) {
			Toast.makeText(this, "Product database not loaded yet.", Toast.LENGTH_LONG).show();
			return;
		}
		Intent intent = new Intent(this, NewProductActivity.class);
		intent.putExtra("mBarcode", barcode);
		startActivityForResult(intent, REQUEST_CODE_ADD_PRODUCT);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		switch (requestCode) {
		case ShoppingListActivity.REQUEST_CODE_PICK_ACCOUNT:
			if (resultCode == RESULT_OK) {
				setUserEmail(intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
				// With the account name acquired, go get the auth token
				loadAuthToken();
			} else {
				// The account picker dialog closed without selecting an
				// account.
				// Notify users that they must pick an account to proceed.
				Toast.makeText(this, "Error: Must pick an account",
						Toast.LENGTH_SHORT).show();
			}
			return;
		case ShoppingListActivity.REQUEST_CODE_ADD_PRODUCT:
			if (resultCode != RESULT_OK) {
				// Use canceled action, no action is required.
				return;
			}
			Product newProduct = (Product) intent.getSerializableExtra("product");
			mProductDb.add(newProduct);
			return;
		case AuthTokenGetter.REQUEST_AUTHORIZATION:
			Log.i("ShoppingListActivity", "AuthTokenGetter.REQUEST_AUTHORIZATION");
			if (resultCode != RESULT_OK) {
				Toast.makeText(this, "Error: Authorization failed.",
						Toast.LENGTH_LONG).show();
				return;
			}
			return;
		case IntentIntegrator.REQUEST_CODE:
			IntentResult scanResult = IntentIntegrator.parseActivityResult(
					requestCode, resultCode, intent);
			if (resultCode == RESULT_OK && scanResult != null) {
				addProductFromBarcode(scanResult.getContents());
			} else {
				Toast.makeText(this, "Scan failed.", Toast.LENGTH_SHORT).show();
			}
			return;
		default:
			Log.e("ShoppingListActivity", "Unknown activity requestCode: "
					+ requestCode);
			return;
		}
	}

	private void setUserEmail(String userEmail) {
		mUserEmail = userEmail;
		
		// Store user's choice in shared preferences, for use next time the app is run.
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		prefs.edit().putString("mUserEmail", userEmail).commit();
	}

	private void loadAuthToken() {
		Log.i("ShoppingListActivity", "loadAuthToken: mUserEmail=[" + mUserEmail + "]");
		if (mUserEmail == null) {
			pickUserAccount();
			return;
		}
		String SCOPES = "oauth2:https://spreadsheets.google.com/feeds https://docs.google.com/feeds";
		new AuthTokenGetter(mUserEmail, SCOPES, this).execute();
	}

	private void pickUserAccount() {
		// First see if the user email was saved in a previous run of the app.
		Log.i("ShoppingListActivity", "pickUserAccount: mUserEmail=[" + mUserEmail + "]");
		SharedPreferences prefs = getPreferences(MODE_PRIVATE);
		mUserEmail = prefs.getString("mUserEmail", null);
		if (mUserEmail != null) {
			Log.i("ShoppingListActivity", "Loaded user email from prefs: [" + mUserEmail + "]");
			loadAuthToken();
			return;
		}
		
		// No user selection. Show the account picker activity.
		String[] accountTypes = new String[] { "com.google" };
		Intent intent = AccountPicker.newChooseAccountIntent(null, null,
				accountTypes, false, null, null, null, null);
		startActivityForResult(intent, REQUEST_CODE_PICK_ACCOUNT);
	}

	public void setAuthToken(String authToken) throws IOException,
			ServiceException {
		Log.i("ShoppingListActivity", "Got auth token, creating product db and shopping list.");
		mAuthToken = authToken;
		mProductDb = new GoogleSheetsProductDatabase(mAuthToken);
		mShoppingList = new ShoppingList(this, android.R.layout.simple_list_item_1, mAuthToken);
		mListView.setAdapter(mShoppingList);
	}

}
