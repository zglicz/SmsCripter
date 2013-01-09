package pl.smscripter.smscripter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

public class MainActivity extends Activity {
	public static final String MESSAGE = "MAIN_ACTIVITY_MESSAGE";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void smsSendStart(View view) {
		Intent intent = new Intent(this, SmsCripter.class);
		startActivity(intent);
	}

	public void smsListStart(View view) {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		final EditText passPhraseText = new EditText(this);
		passPhraseText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		alert
			.setTitle(R.string.rsa_pass_phrase)
			.setMessage(R.string.secret_key_pass_phrase)
			.setView(passPhraseText)
			.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String passPhrase = passPhraseText.getText().toString();
					Intent intent = new Intent(MainActivity.this, SmsList.class);
					intent.putExtra(MESSAGE, passPhrase);
					startActivity(intent);
				}
			})
			.setNegativeButton(R.string.cancel, null);
		Dialog dialog = alert.create();
		dialog.show();
	}
}
