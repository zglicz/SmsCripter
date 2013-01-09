package pl.smscripter.smscripter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.TextView;
import encryption.RSACryptor;

public class SmsList extends ListActivity implements OnItemClickListener {
	public static final String ADDRESS = "address";
	public static final String DATE = "date";
	public static final String BODY = "body";

	public static final String SMS_URI = "content://sms/inbox";
	public String passPhrase;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Intent intent = getIntent();
		this.passPhrase = intent.getStringExtra(MainActivity.MESSAGE);

		Cursor cursor =
				this.getContentResolver().query(Uri.parse(SMS_URI),
						new String[] {"_id", "address", "date", "body"},
						"body LIKE \'**********%\'", null, null);
		startManagingCursor(cursor);

		ListAdapter listAdapter = new SmsListAdapter(
				this,
				R.layout.sms_row_item,
				cursor,
				new String[] {ADDRESS, DATE, BODY},
				new int[] {R.id.sms_origin, R.id.sms_date, R.id.sms_body});
		setListAdapter(listAdapter);
		getListView().setOnItemClickListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_sms_list, menu);
		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		String origin = ((TextView) view.findViewById(R.id.sms_origin)).getText().toString();
		String date = ((TextView) view.findViewById(R.id.sms_date)).getText().toString();
		String body = ((TextView) view.findViewById(R.id.sms_body)).getText().toString();
		String decipheredBody = "Unable to decipher";
		try {
			decipheredBody = RSACryptor.decryptFromString(passPhrase, RSACryptor.privatePath(), body);
		} catch (Exception e) {
			decipheredBody += "\n" + e;
		}
		alert.setMessage(
				  "Nadawca : " + origin +
				"\nData : " + date +
				"\nZaszyfrowane : " + body.substring(10, 25) +
				"\nOdszyfrowane : " + decipheredBody);
		Dialog dialog = alert.create();
		dialog.show();
	}
}
