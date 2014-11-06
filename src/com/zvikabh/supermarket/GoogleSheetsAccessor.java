package com.zvikabh.supermarket;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.AsyncTask;
import android.util.Log;

import com.google.common.collect.ImmutableMap;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.data.spreadsheet.WorksheetFeed;
import com.google.gdata.util.ServiceException;

public class GoogleSheetsAccessor implements Serializable {

	private static final long serialVersionUID = 1952623605676050453L;
	
	public GoogleSheetsAccessor(String authToken) {
		mInitStatus = InitStatus.UNINITIALIZED;
		mInitNotifier = Integer.valueOf(0);  // dummy object (must be serializable).
		mAuthToken = authToken;
	}
	
	/**
	 * Choose a spreadsheet to read from and write to, based on its feed URL.
	 */
	public void initFromSpreadsheetUrl(String spreadsheetUrl) {
		mSpreadsheetUrl = spreadsheetUrl;
		synchronized (mInitStatus) {
			mInitStatus = InitStatus.INITIALIZED;
			synchronized(mInitNotifier) {
				mInitNotifier.notifyAll();
			}
		}
	}

	/**
	 * Choose a spreadsheet to read from and write to, based on its title.
	 * The first matching spreadsheet will be used.
	 * If no match is found, creates a new spreadsheet with the specified title.
	 */
	public void initFromSpreadsheetTitleOrCreate(String spreadsheetTitle) {
		Log.i("GoogleSheetsAccessor", "initFromSpreadsheetTitleOrCreate");
		synchronized(mInitStatus) {
			mInitStatus = InitStatus.INITIALIZING;
		}
		new SpreadsheetFinder().execute(spreadsheetTitle);
		// When the spreadsheet is found or created, the async task will set 
		// mInitStatus to INITIALIZED and then call mInitNotifier.notifyAll().
	}

	/**
	 * Starts an AsyncTask which reads all items in the Google Sheet
	 * and saves them in the specified target map.
	 */
	public void asyncLoadDataInto(Map<String, Product> target) {
		new DataGetter(new MapDataGetterTarget(target)).execute();
	}
	
	public void asyncLoadDataInto(ShoppingList shoppingList) {
		new DataGetter(new ShoppingListDataGetterTarget(shoppingList)).execute();
	}

	/**
	 * Starts an AsyncTask which adds the product to the spreadsheet.
	 */
	@SuppressWarnings("unchecked")
	public void asyncAddProduct(Product product) {
		Map<String, String> productKV = ImmutableMap.of(
				"barcode", product.barCode, 
				"productname", product.name, 
				"manufacturer", product.manufacturer);
		new DataAdder().execute(productKV);
	}
	
	@SuppressWarnings("unchecked")
	public void asyncAddProductWithCount(Product product, int count) {
		Map<String, String> productKV = ImmutableMap.of(
				"barcode", product.barCode, 
				"productname", product.name, 
				"manufacturer", product.manufacturer,
				"count", String.valueOf(count));
		new DataAdder().execute(productKV);
	}

	private SpreadsheetService getSpreadsheetService() {
		SpreadsheetService service = new SpreadsheetService("GoogleSheetsAccessor");
		service.setAuthSubToken(mAuthToken);
		return service;
	}
	
	private ListFeed getListFeed() throws MalformedURLException, IOException, ServiceException, InterruptedException {
		SpreadsheetService service = getSpreadsheetService();
		
		// Verify that we are initialized, or wait for initialization to complete.
		switch (mInitStatus) {
		case UNINITIALIZED:
			throw new IllegalArgumentException("Attempting to load an uninitialized spreadsheet.");
		case INITIALIZING:
			Log.w("GoogleSheetsAccessor", "Waiting for initialization to complete...");
			mInitStatus.wait();
			Log.w("GoogleSheetsAccessor", "Initialization is now complete!");
			break;
		case INITIALIZED:
			// we're fine, do nothing.
			break;
		}
		
		ListFeed listFeed;
		Log.i("GoogleSheetsAccessor", "mSpreadsheetUrl: [" + mSpreadsheetUrl + "]");
		listFeed = service.getFeed(new URL(mSpreadsheetUrl), ListFeed.class);
		return listFeed;
	}
	
	private interface DataGetterTarget {
		abstract void add(Product product);
	}
	
	private class MapDataGetterTarget implements DataGetterTarget {

		private Map<String, Product> mTarget;
		
		public MapDataGetterTarget(Map<String, Product> target) {
			mTarget = target;
		}

		@Override
		public void add(Product product) {
			mTarget.put(product.barCode, product);
		}
		
	}
	
	private class ShoppingListDataGetterTarget implements DataGetterTarget {

		private ShoppingList mShoppingList;
		
		public ShoppingListDataGetterTarget(ShoppingList shoppingList) {
			mShoppingList = shoppingList;
		}

		@Override
		public void add(Product product) {
			mShoppingList.add(new ShoppingList.Item(product, 1));
		}
		
	}

	private class DataGetter extends AsyncTask<Void, Void, List<Product>> {
		public DataGetter(DataGetterTarget target) {
			mTarget = target;
		}
		
		private DataGetterTarget mTarget;
		
		@Override
		protected List<Product> doInBackground(Void... params) {
			List<Product> products = new ArrayList<Product>();
			
			try {
				ListFeed barcodeList = getListFeed();
				Log.i("GoogleSheetsAccessor", "Successfully loaded barcode worksheet");
				for (ListEntry barcodeEntry : barcodeList.getEntries()) {
					CustomElementCollection elements = barcodeEntry.getCustomElements();
					Product product = new Product(elements.getValue("barcode"),
							elements.getValue("productname"),
							elements.getValue("manufacturer"));
					products.add(product);
				}
				Log.i("GoogleSheetsAccessor", "Loaded " + products.size() + " products.");
			} catch (Exception e) {
				Log.e("GoogleSheetsAccessor", "Failed to load barcode list", e);
				return null;
			}
			
			return products;
		}

		@Override
		protected void onPostExecute(List<Product> products) {
			if (products == null) {
				// TODO: Handle loading errors
				return;
			}
			
			for (Product product : products) {
				mTarget.add(product);
			}
		}
		
	}
	
	private class DataAdder extends AsyncTask<Map<String, String>, Void, Void> {
		
		@Override
		protected Void doInBackground(Map<String, String>... rows) {
			try {
				ListFeed barcodeList = getListFeed();
				for (Map<String, String> rowData : rows) {
					ListEntry row = new ListEntry();
					for (Map.Entry<String, String> kv : rowData.entrySet()) {
						row.getCustomElements().setValueLocal(kv.getKey(), kv.getValue());
					}
					barcodeList.insert(row);
				}
			}
			catch (Exception e) {
				Log.e("GoogleSheetsAccessor", "Failed to add products", e);
			}
			return null;
		}
		
	}
	
	private class SpreadsheetFinder extends AsyncTask<String, Void, Void> {

		@Override
		protected Void doInBackground(String... params) {
			String spreadsheetTitle = params[0];

			SpreadsheetService service = getSpreadsheetService();

			SpreadsheetFeed feed;
			try {
				feed = service.getFeed(new URL(SPREADSHEET_FEED_URL), SpreadsheetFeed.class);
			} catch (Exception e) {
				Log.e("SpreadsheetFinder", "Failed to load spreadsheet list", e);
				return null;
			}

			for (SpreadsheetEntry spreadsheet : feed.getEntries()) {
				String actualTitle = spreadsheet.getTitle().getPlainText();
				if (actualTitle.equals(spreadsheetTitle)) {
					try {
						WorksheetFeed worksheetFeed = service.getFeed(
						        spreadsheet.getWorksheetFeedUrl(), WorksheetFeed.class);
						List<WorksheetEntry> worksheets = worksheetFeed.getEntries();
						WorksheetEntry worksheet = worksheets.get(0);
						foundSpreadsheet(worksheet.getListFeedUrl());
					} catch (Exception e) {
						Log.e("SpreadsheetFinder", "Failed to load spreadsheet", e);
					}
					return null;
				}
			}

			// TODO: Spreadsheet not found; must create a new one, then call foundSpreadsheet on it.
			Log.e("SpreadsheetFinder", "Spreadsheet not found: [" + spreadsheetTitle + "]");
			
			return null;
		}
		
		private void foundSpreadsheet(URL listFeedUrl) {
			mSpreadsheetUrl = listFeedUrl.toString();
			Log.i("SpreadsheetFinder", "Found spreadsheet: " + mSpreadsheetUrl);
			synchronized (mInitStatus) {
				mInitStatus = InitStatus.INITIALIZED;
				synchronized(mInitNotifier) {
					mInitNotifier.notifyAll();
				}
			}
		}
		
		private static final String SPREADSHEET_FEED_URL = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
		
	}
	
	private String mSpreadsheetUrl;
	private String mAuthToken;
	
	private enum InitStatus { UNINITIALIZED, INITIALIZING, INITIALIZED };
	/**
	 * Indicates whether mSpreadsheetUrl points to a valid spreadsheet URL.
	 * Finding the correct URL may take a long time, in which case it is performed on
	 * a separate thread.
	 * mInitNotifier.notifyAll() is called when the state is changed to INITIALIZED.
	 */
	private InitStatus mInitStatus;
	private final Integer mInitNotifier;
}
