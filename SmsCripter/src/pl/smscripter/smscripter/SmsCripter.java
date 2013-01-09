package pl.smscripter.smscripter;

import encryption.RSACryptor;

import tools.PubKeyManager;
import tools.Util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class SmsCripter extends Activity {
	public static String KEY_SERVER_UPLOAD = "http://www.rediris.es/keyserver/";
	private PubKeyManager pubKeyManager;

	// Menu items' Id's
	final static int CONTACT_NOT_PICKED = 0;
	final static int GENERATE_KEY_BUTTON = 1;
	final static int PRIVATE_KEY_MISSING = 2;
	final static int SEARCH_KEY_BUTTON = 3;
	final static int UPLOAD_PUBLIC_KEY = 5;
	final static int PUBLIC_KEY_RESOLVER = 6;

	// Intent result Id
	final static int CONTACT_PICKER_RESULT = 1001;

	// Picked contact
	private String phoneNumber = "";
	private String personName = "";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_sms_cripter);
		this.setTitle("SmsCripter (" + (RSACryptor.existsKeyPair() ? "Posiadasz klucz" : "Wygeneruj klucz") + ")");
		pubKeyManager = new PubKeyManager(this);
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

							showDialog(PUBLIC_KEY_RESOLVER);
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
			if (phoneNumber.equals("")) {
				showDialog(CONTACT_NOT_PICKED);
				return ;
			}
			if (!pubKeyManager.hasPublicKey(phoneNumber)) {
				showDialog(PUBLIC_KEY_RESOLVER);
				return ;
			}
			if (!RSACryptor.existsKeyPair()) {
				showDialog(PRIVATE_KEY_MISSING);
				return ;
			}
			String publicKey = pubKeyManager.getPublicKey(phoneNumber);
			String encryptedBody = RSACryptor.encryptToStringKeyFormString(body, publicKey);
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
			case R.id.upload_public_key:
				showDialog(UPLOAD_PUBLIC_KEY);
				break;
			case R.id.search_key:
				if (phoneNumber.equals("")) {
					showDialog(CONTACT_NOT_PICKED);
				} else {
					showDialog(SEARCH_KEY_BUTTON);
				}
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
				final EditText identity = (EditText) textEntryView.findViewById(R.id.rsa_identity);
				final EditText passPhrase = (EditText) textEntryView.findViewById(R.id.rsa_pass_phrase);
				final EditText passPhrase2 =(EditText) textEntryView.findViewById(R.id.rsa_pass_phrase2);
				alert
					.setTitle(R.string.options)
					.setView(textEntryView)
					.setPositiveButton("OK",
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								try{
									String inputIdentity = identity.getText().toString();
									String inputPassPhrase = passPhrase.getText().toString();
									String inputPassPhrase2= passPhrase2.getText().toString();
									if (inputPassPhrase.equals(inputPassPhrase2)) {
										RSACryptor.generateBCKeyPair(inputIdentity, inputPassPhrase);
									} else {
										throw new Exception("Podane has³a ró¿ni¹ siê.");
									}
								} catch (Exception e) {
									Toast.makeText(SmsCripter.this, "Error while generating: " + e, Toast.LENGTH_LONG).show();
									return ;
								}
								Toast.makeText(SmsCripter.this, "Succesfully generated", Toast.LENGTH_LONG).show();
								showDialog(UPLOAD_PUBLIC_KEY);
							}
						})
					.setNegativeButton(R.string.cancel, null);
				break;
			case CONTACT_NOT_PICKED:
				alert.setMessage(R.string.contact_not_picked);
				break;
			case PRIVATE_KEY_MISSING:
				alert
					.setMessage(R.string.private_key_missing)
					.setPositiveButton("Wygeneruj klucz", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							showDialog(GENERATE_KEY_BUTTON);
						}
					})
					.setNegativeButton(R.string.cancel, null);
				break;
			case SEARCH_KEY_BUTTON:
				final EditText identityText = new EditText(this);
				alert
					.setTitle(R.string.search_key)
					.setMessage(R.string.search_key_dialog)
					.setView(identityText)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							String identity = identityText.getText().toString();
							Log.e("SmsCripter", "starting to saerch with id: " + identity);
							pubKeyManager.searchKey(identity, phoneNumber, SmsCripter.this);
						}
					});
				break;
			case UPLOAD_PUBLIC_KEY:
				alert
					.setTitle(R.string.upload_public_key)
					.setMessage(R.string.upload_key_message)
					.setPositiveButton("OK", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
						    android.text.ClipboardManager clipboard =
						    		(android.text.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
						    clipboard.setText(Util.readFileAsString(RSACryptor.publicPath()));
							Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(KEY_SERVER_UPLOAD));
							startActivity(intent);
						}
					});
				break;
			case PUBLIC_KEY_RESOLVER:
				if (pubKeyManager.hasPublicKey(phoneNumber)) {
					alert
						.setMessage(R.string.public_key_already_exists)
						.setPositiveButton(R.string.use_saved_public_key, null)
						.setNegativeButton(R.string.search_key, new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								showDialog(SEARCH_KEY_BUTTON);
							}
						});
				} else {
					alert
						.setMessage(R.string.public_key_missing)
						.setPositiveButton("OK", new DialogInterface.OnClickListener() {
							
							@Override
							public void onClick(DialogInterface dialog, int which) {
								showDialog(SEARCH_KEY_BUTTON);
							}
						})
						.setNegativeButton(R.string.cancel, null);
				}
				break;
		}
		dialog = alert.create();
		return dialog;
	}
}
