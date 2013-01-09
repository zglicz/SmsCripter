package tools;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.util.Log;

public class Util {
	public static String readFileAsString(String filePath) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			String line, results = "";
			while((line = reader.readLine()) != null) {
				results += line + "\n";
			}
			reader.close();
			return results;
		} catch (Exception e) {
			Log.e("ReadPubKey", "Error while reading pub key: " + e);
		}
		return null;
	}
	
	public static String getDate(long milliSeconds) {
		String dateFormat = "dd/MM/yyyy hh:mm:ss";
		DateFormat formatter = new SimpleDateFormat(dateFormat);
 
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(milliSeconds);
		return formatter.format(calendar.getTime());
	}
}
