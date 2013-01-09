package pl.smscripter.smscripter;

import tools.Util;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class SmsListAdapter extends SimpleCursorAdapter {
	Cursor cursor;
	Context context;
	
	public SmsListAdapter(Context context, int layout, Cursor c, String[] from,
			int[] to) {
		super(context, layout, c, from, to);
		this.cursor = c;
		this.context = context;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.sms_row_item, null);
		}
		View row = convertView;
		
		cursor.moveToPosition(position);
		TextView address = (TextView) row.findViewById(R.id.sms_origin);
		TextView date = (TextView) row.findViewById(R.id.sms_date);
		TextView body = (TextView) row.findViewById(R.id.sms_body);
		
		address.setText(cursor.getString(cursor.getColumnIndex("address")));
		date.setText(Util.getDate(cursor.getLong(cursor.getColumnIndex("date"))));
		body.setText(cursor.getString(cursor.getColumnIndex("body")));
		
		return row;
	}
}
