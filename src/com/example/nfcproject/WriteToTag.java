package com.example.nfcproject;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ToggleButton;

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
	private boolean isWrite = false;
	private final boolean writeProtect = false;
	private boolean isNone, isWEP;
    private final boolean isWPA = false;
	private boolean isTwoFac = false;
	private CheckBox isTwoFacCheckBox;
	private EditText twoFacPwField;
	private EditText routerPwField;
	private Button pwGenButton;
	private ImageButton copyButton;
	private RelativeLayout twoFacLayout;
	private ToggleButton isWriteButton;

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
		eap_spinner.setSelection(2); // WPA/WPA2 PSK
		eap_spinner.setOnItemSelectedListener(new OnItemSelectedListener() {

				@Override
				public void onItemSelected(AdapterView<?> parentView, View selectedView,
						int position, long id) {
					if (position == 0) { // if 'None'
						passField.setVisibility(View.GONE);
					}
					else passField.setVisibility(View.VISIBLE);
				}

				@Override
				public void onNothingSelected(AdapterView<?> arg0) {

				}
		});


		twoFacLayout = (RelativeLayout) findViewById(R.id.twoFacLayout);
		twoFacLayout.setVisibility(View.GONE);
		twoFacPwField = (EditText) findViewById(R.id.two_fac_pw_field);
		routerPwField = (EditText) findViewById(R.id.router_pw_field);
		pwGenButton = (Button) findViewById(R.id.pw_gen_button);
		copyButton = (ImageButton) findViewById(R.id.copy_button);
		copyButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				ClipboardManager clipboard = (ClipboardManager)   getSystemService(Context.CLIPBOARD_SERVICE);
			    ClipData clip = ClipData.newPlainText("Copied", routerPwField.getText());
			    clipboard.setPrimaryClip(clip);
			    Toast.makeText(getApplicationContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
			}
		});


		isHiddenButton = (CheckBox) findViewById(R.id.isHiddenCheckBox);
		isHiddenButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				isHidden = isChecked;
			}
		});

		isTwoFacCheckBox = (CheckBox) findViewById(R.id.isTwoFactorCheckBox);
		isTwoFacCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				isTwoFac = isChecked;
				int vis = View.GONE;
				if (isChecked)
				{
					vis = View.VISIBLE;
				}
				twoFacLayout.setVisibility(vis);
			}
		});

		isWriteButton = (ToggleButton) findViewById(R.id.isWriteButton);
		isWriteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				isWrite = isChecked;
			}
		}

		);

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
									@Override
                                    public void onClick(DialogInterface arg0,
											int arg1) {
										Intent setnfc = new Intent(
												Settings.ACTION_WIRELESS_SETTINGS);
										startActivity(setnfc);
									}
								})
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									@Override
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

		if(isWrite){

			if(NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
	        	// validate that this tag can be written....
				Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

				// check if tag is writable (to the extent that we can
				if(writableTag(detectedTag)) {
					//writeTag here
					WriteResponse wr = writeTag(CreateNdef(), detectedTag);
					String message = (wr.getStatus() == 1? "Success: " : "Failed: ") + wr.getMessage();
					Toast.makeText(context,message,Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(context,"This tag is not writable",Toast.LENGTH_SHORT).show();
				}
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

    private NdefMessage CreateNdef() {

		String uniqueId = ssidField.getText().toString();
		String currentPass = passField.getText().toString();
		String secondaryPassword = twoFacPwField.getText().toString();
		int keymgmt = eap_spinner.getSelectedItemPosition();

		ConfigSerialization confSerialization = new ConfigSerialization(uniqueId, currentPass, secondaryPassword, isTwoFac, isHidden, keymgmt);
		NdefMessage ndef = null;
        ndef = confSerialization.toNdefMessage();
		return ndef;

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
