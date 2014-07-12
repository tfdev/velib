package trouve.mon.velib;

import java.net.HttpURLConnection;
import java.net.URL;

import android.util.Log;

public class DownloadRunnable implements Runnable{

	
	//-----------------  Class Fields ------------------
	
	private static final String TAG = DownloadRunnable.class.getName();
	
	private static final String URL_STATION = "https://api.jcdecaux.com/vls/v1/stations?contract=Paris&apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	
	//-----------------  Instance Fields ------------------
	
	private MapActivity mapActivity;
	
	//-----------------  Instance Methods ------------------
	
	public DownloadRunnable(MapActivity activity) {
		this.mapActivity = activity;
	}
	
	public void run() {
		HttpURLConnection connection = null;
		mapActivity.runOnUiThread(new Runnable() {
			public void run() {
				mapActivity.showRefreshing();
			}
		});
		try {
			URL url = new URL(URL_STATION+API_KEY);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setReadTimeout(10000 /* milliseconds */);
	        connection.setConnectTimeout(15000 /* milliseconds */);
	        connection.setRequestMethod("GET");
	        connection.setDoInput(true);
	        connection.connect();
	        if(connection.getResponseCode() == 200){
	        	StationParser.parse(connection.getInputStream());
	        	mapActivity.runOnUiThread(new Runnable() {
					public void run() {
						mapActivity.refreshMarkers(true);
						mapActivity.refreshDetails();
					}
				});
	        }
	        else{
	        	mapActivity.runOnUiThread(new Runnable() {
					public void run() {
						mapActivity.showMessage(mapActivity.getString(R.string.msg_no_data)); 
					}
				});
	        }
		}
		catch (Exception e) {
			Log.e(TAG, "Exception while downloading info", e);
			mapActivity.runOnUiThread(new Runnable() {
				public void run() {
					mapActivity.showMessage(mapActivity.getString(R.string.msg_check_internet));
				}
			});
		}
		finally{
			if(connection != null){
				connection.disconnect();
			}
			mapActivity.runOnUiThread(new Runnable() {
				public void run() {
					mapActivity.stopRefreshing();
				}
			});
		}
	}
	
}
