package trouve.mon.velib.util;

import trouve.mon.velib.R;
import trouve.mon.velib.station.GoogleMapFragment;
import trouve.mon.velib.station.Station;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationClientManager {
	
	//----------------- Static methods ------------------
	
	public static LocationClient setUp(Context c, FragmentManager fm){
		context = c;
		fragmentManager = fm;
		locationClient = new LocationClient(context, new Listener(),  // ConnectionCallbacks
													 new Listener()); // OnConnectionFailedListener   
		return locationClient;
	}
	
	
	public static LocationClient getClient(){
		return locationClient;
	}
	
	public static int distanceFromLastLocation(Station station){
    	if(locationClient != null && locationClient.isConnected()){
    		Location lastLocation = locationClient.getLastLocation();
    		if(lastLocation != null){
    			float result = lastLocation.distanceTo(station.getLocation());
    			int d = Math.round(result);
    			station.setDistanceFromLocation(d);
    			return d;
    		}
    	}
    	return 0;
    }
	
	
	//----------------- Instance fields ------------------
	
	private static Context context;	
	private static LocationClient locationClient;
	private static FragmentManager fragmentManager;
	
	//----------------- Instance methods ------------------
	
	private LocationClientManager(){
		
	}
	
	
	//----------------- Interface Implementation ------------------
	
	public static class Listener implements LocationListener,
											ConnectionCallbacks,
											OnConnectionFailedListener{
		
		
	    private static final LocationRequest REQUEST = LocationRequest.create().setInterval(5000)
													  .setFastestInterval(16)
													  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Helper.showMessageLong(R.string.msg_no_gps);
		}
		
		@Override
		public void onConnected(Bundle connectionHint) {
			locationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
			Fragment fragment = fragmentManager.findFragmentByTag(GoogleMapFragment.MAP_FRAGMENT_TAG);
			if(fragment != null){
				GoogleMapFragment mapFragment = (GoogleMapFragment) fragment;
				mapFragment.centerMapOnMyLocation(false);
			}
		}
		
		@Override
		public void onDisconnected() {
		}
		
		@Override
		public void onLocationChanged(Location location) {
		}
	}
}
