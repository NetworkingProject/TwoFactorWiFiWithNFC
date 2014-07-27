package com.example.nfcproject;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity {
	private NfcAdapter mNfcAdapter;
    private IntentFilter[] mWriteTagFilters;
	private PendingIntent mNfcPendingIntent;
	private boolean isWrite = false;
	private boolean isTwoFac = false;
	private Context context;
	private CheckBox isTwoFacCheckBox;
	
	
	@Override
	public void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		
		// For Toast and WiFi Manager
		context = getApplicationContext();
		
		Intent intent = new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP
						| Intent.FLAG_ACTIVITY_CLEAR_TOP);
		
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        
        IntentFilter discovery = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);        
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

        // Intent filters for writing to a tag
        mWriteTagFilters = new IntentFilter[] { ndefDetected, discovery, techDetected };

		isTwoFacCheckBox = (CheckBox) findViewById(R.id.readScreenTwoFac);
		isTwoFacCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				isTwoFac = isChecked;
			}
		});

	}

	@Override
	protected void onResume() {
		super.onResume();
		
		if (mNfcAdapter != null) {
			
			if (!mNfcAdapter.isEnabled()) {
				LayoutInflater inflater = getLayoutInflater();
				View dialoglayout = inflater.inflate(R.layout.activity_main,
						(ViewGroup) findViewById(R.id.nfc_settings_layout));
				new AlertDialog.Builder(this)
						.setView(dialoglayout)
						.setPositiveButton("Update Settings",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface arg0,
											int arg1) {
										Intent setnfc = new Intent(
												Settings.ACTION_WIRELESS_SETTINGS);
										startActivity(setnfc);
									}
								})
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									public void onCancel(DialogInterface dialog) {
										System.out.println("cancel");
										finish(); // exit application if user
													// cancels
									}
								}).create().show();
			}
			mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
					mWriteTagFilters, null);
		} else {
			mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
					mWriteTagFilters, null);
			Toast.makeText(context,
					"Sorry, No NFC Adapter found.", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (mNfcAdapter != null)
			mNfcAdapter.disableForegroundDispatch(this);
	}

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		// Read and parse tag
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {

			NdefMessage[] messages = null;
			Parcelable[] rawMsgs = intent
					.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				messages = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					messages[i] = (NdefMessage) rawMsgs[i];
				}
			}
			if (messages[0] != null) {
				
		        ConfigSerialization confReadSerializer = new ConfigSerialization(messages[0]);
		        WifiConfiguration conf = confReadSerializer.toWifiConfig();
				
				WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE); 
				
				// TODO: Returns -1 on failure. Add error handling.
				int networkId = wifiManager.addNetwork(conf);

				// TODO: Returns -1 on failure. Add error handling.
				wifiManager.enableNetwork(networkId, true);
				// Returns true on success
				wifiManager.reconnect();

				Toast.makeText(context,"Connecting to " + confReadSerializer.SSID, Toast.LENGTH_SHORT).show();

			}
		}
	}

	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item){
	    switch(item.getItemId()){
	    case R.id.action_admin:
	        Intent intent = new Intent(MainActivity.this, WriteToTag.class);
	        startActivity(intent);
	        return true;  
	    case R.id.action_settings:
	        // TODO: Add settings menu or remove menu
	        return true;   
	    }
		    
	    return false;
	}
}
