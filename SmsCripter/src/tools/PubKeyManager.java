package tools;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import pl.smscripter.smscripter.SmsCripter;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

public class PubKeyManager {
	public static final String tag = "PubKeyManager";
	private static String KEY_SERVER_ADDRESS = "http://pgp.rediris.es:11371";
    private static String CHARSET = "UTF-8";
    
    private static String PREFS_NAME = "PHONE_TO_KEYS";
	public SharedPreferences sharedPreferences;
	
	public PubKeyManager(Context context) {
		this.sharedPreferences = context.getSharedPreferences(PREFS_NAME, 0);
	}

	public boolean hasPublicKey(String origin) {
		Log.i(tag, "looking for : " + origin);
		return sharedPreferences.contains(origin);
	}

	public String getPublicKey(String origin) {
		return sharedPreferences.getString(origin, null);
	}

	public void searchKey(String identity, String phoneNumber, Context context) {
		new SearchKeyTask(context, phoneNumber).execute(identity);
	}

	class SearchKeyTask extends AsyncTask<String, Integer, String> {
		Context context;
		String phoneNumber;
		
		public SearchKeyTask(Context context, String phoneNumber) {
			this.context = context;
			this.phoneNumber = phoneNumber;
		}

	    private String createUrlQuery(String identity) throws UnsupportedEncodingException {
	        StringBuilder urlAddress = new StringBuilder();
	        urlAddress.append(KEY_SERVER_ADDRESS);
	        urlAddress.append("/pks/lookup?");
	        urlAddress.append("search="); urlAddress.append(URLEncoder.encode(identity, CHARSET));
	        return urlAddress.toString();
	    }

		@Override
		protected String doInBackground(String... params) {
			Log.e(tag, "starting to look for key");
			String identity = params[0];
			try {
	            String address = createUrlQuery(identity);
	            Document doc = Jsoup.connect(address).get();
	            
	            // if connect didn't throw 404 it means a key was found
	            // we assume there is always at least one key
	            String linkHref = doc.getElementsByTag("a").first().attr("abs:href");
	            doc = Jsoup.connect(linkHref).get();
	            
	            // we assume that there is at least 1 one link, we pick the first/newest one.
	            String key = doc.getElementsByTag("pre").first().text();
	            Log.e(tag, "Succesfully downloaded");
	            return key;
	        } catch (IOException ex) {
	        	Log.e(tag, "Exception while searching");
	            return null;
	        }
		}

		@Override
		protected void onPostExecute(String result) {
			if (result == null) {
				Toast.makeText(context, "Key not found", Toast.LENGTH_LONG).show();
			} else {
				Toast.makeText(context, "Key succesfully downloaded", Toast.LENGTH_LONG).show();
				SharedPreferences.Editor editor = sharedPreferences.edit();
				editor.putString(phoneNumber, result);
				editor.commit();
				Log.i(tag, "saved : " + phoneNumber + ", pub: " + result);
			}
		}
	}
}
