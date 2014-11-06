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

import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.CustomElementCollection;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.util.ServiceException;

/**
 * Loads the barcode data from a Google Spreadsheet.
 *
 */
public class GoogleSheetsProductDatabase implements AbstractProductDatabase {
	
	private static final long serialVersionUID = -8991018198900355945L;

	public GoogleSheetsProductDatabase(String authToken) {
		mSheetsAccessor = new GoogleSheetsAccessor(authToken);
		mSheetsAccessor.initFromSpreadsheetUrl(SPREADSHEET_URL);
		mDatabase = new HashMap<String, Product>();
		mSheetsAccessor.asyncLoadDataInto(mDatabase);
	}
	
	@Override
	public Product get(String barCode) {
		return mDatabase.get(barCode);
	}

	@Override
	public void add(Product product) {
		mDatabase.put(product.barCode, product);
		mSheetsAccessor.asyncAddProduct(product);
	}
	
	private static final String SPREADSHEET_URL = "https://spreadsheets.google.com/feeds/list/1mi2L9kB7PW0-vqKzsdzPYgww9M9cUBIx36RugS_icTg/od6/private/full";
	private GoogleSheetsAccessor mSheetsAccessor;
	private Map<String, Product> mDatabase;
}
