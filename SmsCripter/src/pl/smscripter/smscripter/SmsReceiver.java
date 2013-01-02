package pl.smscripter.smscripter;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

public class SmsReceiver extends BroadcastReceiver 
{
	// All available column names in SMS table
    // [_id, thread_id, address, 
	// person, date, protocol, read, 
	// status, type, reply_path_present, 
	// subject, body, service_center, 
	// locked, error_code, seen]
	
	public static final String SMS_EXTRA_NAME = "pdus";
	public static final String SMS_URI = "content://sms";
	
	public static final String ADDRESS = "address";
    public static final String PERSON = "person";
    public static final String DATE = "date";
    public static final String READ = "read";
    public static final String STATUS = "status";
    public static final String TYPE = "type";
    public static final String BODY = "body";
    public static final String SEEN = "seen";
    
    public static final int MESSAGE_TYPE_INBOX = 1;
    public static final int MESSAGE_TYPE_SENT = 2;
    
    public static final int MESSAGE_IS_NOT_READ = 0;
    public static final int MESSAGE_IS_READ = 1;
    
    public static final int MESSAGE_IS_NOT_SEEN = 0;
    public static final int MESSAGE_IS_SEEN = 1;

	public void onReceive( Context context, Intent intent ) 
	{
        Bundle extras = intent.getExtras();
        if (extras != null) {
            Object[] smsExtra = (Object[]) extras.get(SMS_EXTRA_NAME);
            
            ContentResolver contentResolver = context.getContentResolver();

            // Concatenates body from single SMS messages
            String wholeBody = "";

            // Represents SMS data 
            SmsMessage sms = null;

            for (int i = 0; i < smsExtra.length; ++i) {
            	sms = SmsMessage.createFromPdu((byte[])smsExtra[i]);
            	String body = sms.getMessageBody().toString();
                wholeBody += body;
            }
            if (RSACryptor.isEncrypted(wholeBody)) {
	            String decryptedBody;
				try {
					decryptedBody = RSACryptor.decryptFromString(null, null, null);
		            Log.i("BroadcastReceiver.onReceive", "Received: " + decryptedBody);
		            putSmsToDatabase(contentResolver, sms, decryptedBody);
		            this.abortBroadcast();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        }
	}
	
	private void putSmsToDatabase( ContentResolver contentResolver, SmsMessage sms, String body )
	{
        ContentValues values = new ContentValues();
        values.put( ADDRESS, sms.getOriginatingAddress() );
        values.put( DATE, sms.getTimestampMillis() );
        values.put( READ, MESSAGE_IS_NOT_READ );
        values.put( STATUS, sms.getStatus() );
        values.put( TYPE, MESSAGE_TYPE_INBOX );
        values.put( SEEN, MESSAGE_IS_NOT_SEEN );
        try {
        	values.put(BODY, body);
            contentResolver.insert( Uri.parse( SMS_URI ), values );
        } catch ( Exception e ) { 
        	e.printStackTrace(); 
    	}
	}
}
