package trouve.mon.velib.station;

import java.net.HttpURLConnection;
import java.net.URL;

import trouve.mon.velib.R;
import android.util.Log;

public class StationUpdater implements Runnable{

	
	//-----------------  Class Fields ------------------
	
	private static final String TAG = StationUpdater.class.getName();
	
	private static final String URL_STATION = "https://api.jcdecaux.com/vls/v1/stations?contract=";
	private static final String API_KEY = "&apiKey=df89b09292638d3c4a2731f771db3f43c514685d";
	
	//-----------------  Instance Fields ------------------
	
	private MapActivity mapActivity;
	private String contractName;
	
	//-----------------  Instance Methods ------------------
	
	public StationUpdater(MapActivity activity, String contractName) {
		this.mapActivity = activity;
		this.contractName = contractName;
	}
	
	public void run() {
		HttpURLConnection connection = null;
		mapActivity.runOnUiThread(new Runnable() {
			public void run() {
				mapActivity.showRefreshing();
			}
		});
		try {
			URL url = new URL(URL_STATION + contractName + API_KEY);
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
