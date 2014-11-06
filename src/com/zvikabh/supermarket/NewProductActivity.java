package com.zvikabh.supermarket;

import android.support.v7.app.ActionBarActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class NewProductActivity extends ActionBarActivity {
	
	private String mBarcode;
	private EditText mEditTextProductName;
	private EditText mEditTextManufacturer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_new_product);
		
		Intent intent = getIntent();
		mBarcode = intent.getStringExtra("mBarcode");
		
		TextView textviewBarcode = (TextView) findViewById(R.id.textViewBarcode);
		textviewBarcode.setText("Barcode: " + mBarcode);
		
		mEditTextProductName = (EditText) findViewById(R.id.editTextProductName);
		mEditTextManufacturer = (EditText) findViewById(R.id.editTextManufacturer);
		
		((Button) findViewById(R.id.buttonAdd)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// TODO: Return added product as intent result
				Intent intent = new Intent();
				intent.putExtra("product", 
						new Product(mBarcode, 
								mEditTextProductName.getText().toString(), 
								mEditTextManufacturer.getText().toString()));
				setResult(RESULT_OK, intent);
				finish();
			}
		});
		
		((Button) findViewById(R.id.buttonCancel)).setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				setResult(RESULT_CANCELED);
				finish();
			}
		});
	}

	@Override
	protected void onRestoreInstanceState(Bundle inBundle) {
		mBarcode = inBundle.getString("mBarcode");
		super.onRestoreInstanceState(inBundle);
	}

	@Override
	protected void onSaveInstanceState(Bundle outBundle) {
		outBundle.putString("mBarcode", mBarcode);
	    super.onSaveInstanceState(outBundle);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.new_product, menu);
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
}
