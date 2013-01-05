package pl.smscripter.smscripter;

import java.io.BufferedReader;
import java.io.FileReader;

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
}
