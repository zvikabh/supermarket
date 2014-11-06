package com.zvikabh.supermarket;

import java.io.Serializable;
import java.util.ArrayList;

import android.content.Context;
import android.util.Log;
import android.widget.ArrayAdapter;

public class ShoppingList extends ArrayAdapter<ShoppingList.Item> implements Serializable {
	private static final long serialVersionUID = 3746896656431096526L;

	public ShoppingList(Context context, int resource, String authToken) {
		super(context, resource, new ArrayList<Item>());
		Log.i("ShoppingList", "Initializing shopping list");
		mSheetsAccessor = new GoogleSheetsAccessor(authToken);
		mSheetsAccessor.initFromSpreadsheetTitleOrCreate(SHOPPING_LIST_SPREADSHEET_TITLE);
		mSheetsAccessor.asyncLoadDataInto(this);
	}

	static class Item implements Serializable {
		private static final long serialVersionUID = 3513193628133220277L;
		public Item(Product product, int count) {
			this.product = product;
			this.count = count;
		}
		
		public Product getProduct() {
			return product;
		}
		public int getCount() {
			return count;
		}
		
		@Override
		public String toString() {
			return product.toString() + " Count:" + count;
		}

		private final Product product;
		private final int count;
	}
	
	public void addItem(Product product, int count) {
		Log.i("ShoppingList", "Adding item: " + product.toString());
		add(new Item(product, count));
		mSheetsAccessor.asyncAddProductWithCount(product, count);
	}
	
	private GoogleSheetsAccessor mSheetsAccessor;
	
	private static final String SHOPPING_LIST_SPREADSHEET_TITLE = "Shopping List (auto-updated by Supermarket app)";
}
