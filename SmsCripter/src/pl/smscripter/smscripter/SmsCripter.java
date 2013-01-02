package pl.smscripter.smscripter;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SmsCripter extends Activity {

	// Menu items' Id's
	final static int CONTACT_NOT_PICKED = 0;
	final static int GENERATE_KEY_BUTTON = 1;
	final static int PRIVATE_KEY_MISSING = 2;

	// Intent result Id
	final static int CONTACT_PICKER_RESULT = 1001;

	// Picked contact
	private String phoneNumber = "";
	private String personName = "";
	private boolean isPicked;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_cripter);
		this.setTitle("SmsCripter (" + (RSACryptor.existsKeyPair() ? "Posiadasz klucz" : "Wygeneruj klucz") + ")");
		isPicked = false;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sms_cripter, menu);
		return true;
	}

	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT); 
	}

	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case CONTACT_PICKER_RESULT:
					Uri contactData = data.getData();
					Cursor cursor = managedQuery(contactData, null, null, null, null);
					cursor.moveToFirst();
					try {
						String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
						String hasPhone = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));
						if (hasPhone.equalsIgnoreCase("1")) {
							personName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
							Cursor phones = getContentResolver()
									.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
											null,
											ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = "+ contactId,
											null,
											null);
							phones.moveToFirst();
							phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
							phones.close();
							
							Button help = (Button) findViewById(R.id.contact_picker);
							help.setText(personName);

							isPicked = true;
							Log.i("SmsCripter.onActivityResult", "Chosen phone number: " + phoneNumber);
							Log.i("SmsCripter.onActivityResult", "Chosen person name : " + personName);
						} else {
							throw new Exception("Contact doesn't have a phone number");
						}
					} catch (Exception e) {
						Toast.makeText(this, "Niepoprawny kontakt, brak numeru telefonu", Toast.LENGTH_LONG).show();
						e.printStackTrace();
						return ;
					}
					break;
			}
		} else {
			Log.v("SmsCripter.onActivityResult", "Not OK, resultCode : " + resultCode);
		}
	}

	public void onClickSendButton(View view) {
		String body = ((EditText) findViewById(R.id.input_body)).getText().toString();
		try {
			if (!isPicked) {
				showDialog(CONTACT_NOT_PICKED);
				return ;
			}
			if (!RSACryptor.existsKeyPair()) {
				showDialog(PRIVATE_KEY_MISSING);
				return ;
			}
			String encryptedBody = RSACryptor.encryptToStringKeyFromFile(body, RSACryptor.publicPath());
			Uri uri = Uri.parse("smsto:" + phoneNumber);
			Intent intent = new Intent(Intent.ACTION_SENDTO, uri);
			intent.putExtra("sms_body", encryptedBody);
			startActivity(intent);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		switch(item.getItemId()) {
			case R.id.generate_key:
				showDialog(GENERATE_KEY_BUTTON);
				break;
			}
		return super.onMenuItemSelected(featureId, item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		Dialog dialog = null;
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		switch (id) {
			case GENERATE_KEY_BUTTON:
				LayoutInflater inflater = getLayoutInflater();
				final View textEntryView = inflater.inflate(R.layout.rsa_gen, null);
	
				alert.setTitle(R.string.options);
				alert.setView(textEntryView);
				final EditText identity = (EditText) textEntryView.findViewById(R.id.rsa_identity);
				final EditText passPhrase = (EditText) textEntryView.findViewById(R.id.rsa_pass_phrase);
				alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							try{
								// TODO: change to BouncyCastle implementation
								String inputIdentity = identity.getText().toString();
								String inputPassPhrase = passPhrase.getText().toString();
								RSACryptor.generateBCKeyPair(inputIdentity, inputPassPhrase);
							} catch (Exception e) {
								Toast.makeText(SmsCripter.this, "Error while generating", Toast.LENGTH_LONG).show();
								e.printStackTrace();
								return ;
							}
							Toast.makeText(SmsCripter.this, "Succesfully generated", Toast.LENGTH_LONG).show();
						}
					})
					.setNegativeButton(R.string.cancel, null);
				break;
			case CONTACT_NOT_PICKED:
				alert.setMessage(R.string.contact_not_picked);
				break;
			case PRIVATE_KEY_MISSING:
				alert.setMessage(R.string.private_key_missing);
				alert.setPositiveButton("Wygeneruj klucz", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						showDialog(GENERATE_KEY_BUTTON);
					}
				})
				.setNegativeButton(R.string.cancel, null);
		}
		dialog = alert.create();
		return dialog;
	}

}
