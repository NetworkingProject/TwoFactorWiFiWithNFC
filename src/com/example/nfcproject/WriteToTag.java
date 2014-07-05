package com.example.nfcproject;

import java.io.IOException;
import java.nio.charset.Charset;

import android.app.Activity;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.os.Parcel;
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
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.os.Build;
import android.provider.Settings;

public class WriteToTag extends Activity {

	private CheckBox isHiddenButton;
	private NfcAdapter mNfcAdapter;
    private IntentFilter[] mWriteTagFilters;
	private PendingIntent mNfcPendingIntent;
	private Context context;
	private EditText ssidField;
	private EditText passField;
	private Spinner eap_spinner;
	private String eap_val;
	private boolean isHidden;
	private boolean writeProtect;
	private boolean isNone, isWEP, isWPA = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_write_to_tag);

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
		
		ssidField = (EditText) findViewById(R.id.ssidField);
		
		passField = (EditText) findViewById(R.id.passField);
		
		eap_spinner = (Spinner) findViewById(R.id.eap_spinner);
				
		isHiddenButton = (CheckBox) findViewById(R.id.isHiddenCheckBox);
		isHiddenButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				isHidden = isChecked;
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
	
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);

		if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
        	// validate that this tag can be written....
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			// check if tag is writable (to the extent that we can
			if(writableTag(detectedTag)) {
				//writeTag here
				WriteResponse wr = writeTag(getTagAsNdef(), detectedTag);
				String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
				Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
			} else {
				Toast.makeText(context,"This tag is not writable",Toast.LENGTH_SHORT).show();
			}	            
		}
	}
	
	public WriteResponse writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        String mess = "";

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();

                if (!ndef.isWritable()) {
                    return new WriteResponse(0,"Tag is read-only");

                }
                if (ndef.getMaxSize() < size) {
                    mess = "Tag capacity is " + ndef.getMaxSize() + " bytes, message is " + size
                            + " bytes.";
                    return new WriteResponse(0,mess);
                }

                ndef.writeNdefMessage(message);
                
                if(writeProtect)  ndef.makeReadOnly();
                mess = "Wrote message to pre-formatted tag.";
                return new WriteResponse(1,mess);
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        mess = "Formatted tag and wrote message";
                        return new WriteResponse(1,mess);
                    } catch (IOException e) {
                        mess = "Failed to format tag.";
                        return new WriteResponse(0,mess);
                    }
                } else {
                    mess = "Tag doesn't support NDEF.";
                    return new WriteResponse(0,mess);
                }
            }
        } catch (Exception e) {
            mess = "Failed to write tag";
            return new WriteResponse(0,mess);
        }
    }
    
    private class WriteResponse {
    	int status;
    	String message;
    	WriteResponse(int Status, String Message) {
    		this.status = Status;
    		this.message = Message;
    	}
    	public int getStatus() {
    		return status;
    	}
    	public String getMessage() {
    		return message;
    	}
    }
	
    private boolean writableTag(Tag tag) {

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(context,"Tag is read-only.",Toast.LENGTH_SHORT).show();
                    ndef.close(); 
                    return false;
                }
                ndef.close();
                return true;
            } 
        } catch (Exception e) {
            Toast.makeText(context,"Failed to read tag",Toast.LENGTH_SHORT).show();
        }

        return false;
    }
    
    private NdefMessage getTagAsNdef() {
    	
		String uniqueId = ssidField.getText().toString();
		String currentPass = passField.getText().toString();
		
		if(!currentPass.isEmpty()){
			uniqueId += ":" + currentPass;
		}
		
		WifiConfiguration config = ConfigureWifi(uniqueId, currentPass);
		
		Parcel parcel = Parcel.obtain();
		config.writeToParcel(parcel, 0);
		byte[] bytes = parcel.marshall();
		parcel.recycle();
		
//		byte[] SSID = uniqueId.getBytes(Charset.forName("US-ASCII"));
//		byte[] payload = new byte[SSID.length + 1];
//		payload[0] = 0x01;

		NdefRecord rtdTextRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
				NdefRecord.RTD_TEXT, new byte[0], bytes);

		return new NdefMessage(new NdefRecord[] { rtdTextRecord });

	}
    
    public WifiConfiguration ConfigureWifi(String ssid, String password) {
		
		WifiConfiguration conf = new WifiConfiguration() {
			@Override
			public void writeToParcel(Parcel dest, int flags) {
				dest.writeInt(networkId);
				dest.writeInt(status);
				dest.writeString(SSID);
				dest.writeString(BSSID);
				dest.writeString(preSharedKey);
				for (String wepKey : wepKeys)
					dest.writeString(wepKey);
				dest.writeInt(wepTxKeyIndex);
				dest.writeInt(priority);
				dest.writeInt(hiddenSSID ? 1 : 0);
			}
		};
		conf.SSID = "\"" + ssid + "\"";
		conf.status = WifiConfiguration.Status.ENABLED;

		eap_val = eap_spinner.getSelectedItem().toString();
		
		if (eap_val == "None")
			isNone = true;
		else if (eap_val == "WEP")
			isWEP = true;
		else if (eap_val == "WPA/WPA2 PSK")
			isWPA = true;
		
		if(isNone || passField.getText().toString().isEmpty()) {
			//For open networks
			conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
		}
		else if(isWEP || isWPA) {
			conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			
			if(isWEP){		
				//For WEP networks							
				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
				conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
				conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

				conf.wepKeys[0] = "\"".concat(password).concat("\"");
				conf.wepTxKeyIndex = 0;
			}
			else if(isWPA){
				//For WPA and WPA2 networks
				conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
				conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

				conf.preSharedKey = "\"" + password + "\"";
			}						
		}
		
		//For hidden networks
		if(isHidden)
			conf.hiddenSSID = true;

		return conf;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.write_to_tag, menu);
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
