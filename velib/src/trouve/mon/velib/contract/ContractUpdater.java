package trouve.mon.velib.contract;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

import trouve.mon.velib.R;
import android.util.Log;

public class ContractUpdater implements Runnable{

	
	//-----------------  Class Fields ------------------
	
	private static final String TAG = ContractUpdater.class.getName();
	
	private static final String URL_CONTRACT = "https://api.jcdecaux.com/vls/v1/contracts?apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	
	//-----------------  Instance Fields ------------------
	
	private ContractListFragment fragment;
	private ContractListActivity activity;
	
	//-----------------  Instance Methods ------------------
	
	public ContractUpdater(ContractListActivity activity) {
		this.activity= activity;
		this.fragment = (ContractListFragment) activity.getSupportFragmentManager().findFragmentById(R.id.contract_fragment);		
	}
	
	public void run() {
		HttpURLConnection connection = null;
		fragment.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				activity.showRefreshing();
				fragment.showLoading();
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
	        	final List<Contract> contracts = ContractParser.parse(connection.getInputStream());
	        	fragment.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						fragment.setContract(contracts);
					}
				});
	        }
	        else{
	        	fragment.getActivity().runOnUiThread(new Runnable() {
					public void run() {
						fragment.showError(R.string.msg_no_data); 
					}
				});
	        }
		}
		catch (Exception e) {
			Log.e(TAG, "Exception while downloading info", e);
			fragment.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					fragment.showError(R.string.msg_check_internet);
				}
			});
		}
		finally{
			if(connection != null){
				connection.disconnect();
			}
			fragment.getActivity().runOnUiThread(new Runnable() {
				public void run() {
					activity.hideRefreshing();
				}
			});
		}
	}
	
}
