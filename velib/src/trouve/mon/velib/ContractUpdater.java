package trouve.mon.velib;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import android.util.Log;

public class ContractUpdater implements Runnable{

	
	//-----------------  Class Fields ------------------
	
	private static final String TAG = ContractUpdater.class.getName();
	
	private static final String URL_CONTRACT = "https://api.jcdecaux.com/vls/v1/contracts?apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	
	//-----------------  Instance Fields ------------------
	
	private ContratListActivity activity;
	
	//-----------------  Instance Methods ------------------
	
	public ContractUpdater(ContratListActivity activity) {
		this.activity = activity;
	}
	
	public void run() {
		HttpURLConnection connection = null;
		activity.runOnUiThread(new Runnable() {
			public void run() {
				//activity.showRefreshing();
			}
		});
		try {
			URL url = new URL(URL_CONTRACT + API_KEY);
	        connection = (HttpURLConnection) url.openConnection();
	        connection.setReadTimeout(10000 /* milliseconds */);
	        connection.setConnectTimeout(15000 /* milliseconds */);
	        connection.setRequestMethod("GET");
	        connection.setDoInput(true);
	        connection.connect();
	        if(connection.getResponseCode() == 200){
	        	final List<Contrat> contrats = ContratParser.parse(connection.getInputStream());
	        	activity.runOnUiThread(new Runnable() {
					public void run() {
						activity.setContract(contrats);
					}
				});
	        }
	        else{
	        	activity.runOnUiThread(new Runnable() {
					public void run() {
						//activity.showMessage(activity.getString(R.string.msg_no_data)); 
					}
				});
	        }
		}
		catch (Exception e) {
			Log.e(TAG, "Exception while downloading info", e);
			activity.runOnUiThread(new Runnable() {
				public void run() {
					//activity.showMessage(activity.getString(R.string.msg_check_internet));
				}
			});
		}
		finally{
			if(connection != null){
				connection.disconnect();
			}
			activity.runOnUiThread(new Runnable() {
				public void run() {
					//activity.stopRefreshing();
				}
			});
		}
	}
	
}
