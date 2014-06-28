package trouve.mon.velib;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;


public class MainActivity extends Activity implements 	ConnectionCallbacks,
														OnConnectionFailedListener,
														LocationListener{
	
	private static final String TAG = MainActivity.class.getName();
	private static final String URL_STATION = "https://api.jcdecaux.com/vls/v1/stations?contract=Paris&apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	
    // These settings are the same as the settings for the map. They will in fact give you updates
    // at the maximal rates currently possible.
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    private GoogleMap mMap;
    private LocationClient mLocationClient;
    

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

	}
	
    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        mLocationClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }
    
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the MapFragment.
            mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                mMap.setMyLocationEnabled(true);
                mMap.setOnMyLocationButtonClickListener(new OnMyLocationButtonClickListener(){

					public boolean onMyLocationButtonClick() {
						centerMapOnMyLocation();
						return true;
					}
                	
                });
                updateMap();
            }
        }
    }
    
    
    private void setUpLocationClientIfNeeded() {
        if (mLocationClient == null) {
            mLocationClient = new LocationClient(
                    getApplicationContext(),
                    this,  // ConnectionCallbacks
                    this); // OnConnectionFailedListener    
        }
    }

    private void centerMapOnMyLocation(){
    	if(mLocationClient != null){
    		Location lastLocation = mLocationClient.getLastLocation();
    		LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
    		mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15.0f));
    	}
    }
    
    
	@Override
	public void onLocationChanged(Location location) {
		
		
	}

	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onConnected(Bundle connectionHint) {
		mLocationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
		centerMapOnMyLocation();
	}

	@Override
	public void onDisconnected() {
		// Do nothing
	}
	
	private void updateMap(){
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo != null && networkInfo.isConnected()) {
            new DownloadTask().execute(URL_STATION, API_KEY);
		} else {
			Toast.makeText(this, "Internet connection disabled", Toast.LENGTH_LONG).show();
		}
	}
	
	private class DownloadTask extends AsyncTask<String, Void, Boolean> {
		String TAG = DownloadTask.class.getName();
		
	    protected void onPostExecute(Boolean done) {
	    	if(done){
	    		StationManager.addMarkers(mMap);
	    	}
	    }

		protected Boolean doInBackground(String... param) {

			try {
				URL url = new URL(param[0]+param[1]);
		        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
		        connection.setReadTimeout(10000 /* milliseconds */);
		        connection.setConnectTimeout(15000 /* milliseconds */);
		        connection.setRequestMethod("GET");
		        connection.setDoInput(true);
		        connection.connect();
		        int response = connection.getResponseCode();
		        Log.d(TAG, "The response is: " + response);
		        StationManager.update(connection.getInputStream());
			} catch (Exception e) {
				Log.e(TAG, "Exception while downloading info", e);
				return false;
			}
			return true;
		}
	 }

}
