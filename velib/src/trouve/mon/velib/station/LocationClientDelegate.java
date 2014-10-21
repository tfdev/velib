package trouve.mon.velib.station;

import trouve.mon.velib.R;
import trouve.mon.velib.util.Helper;
import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;

public class LocationClientDelegate {
	
	//----------------- Static methods ------------------
	
	public static LocationClient getClient(Context c){

		if (locationClient == null) {
			context = c;
			locationClient = new LocationClient(context, new Listener(),  // ConnectionCallbacks
														new Listener()); // OnConnectionFailedListener    
			locationClient.connect();
		}
		
		return locationClient;
	}
	
	public static int distanceFromLastLocation(Station station){
    	if(locationClient != null){
    		Location lastLocation = locationClient.getLastLocation();
    		if(lastLocation != null){
    			LatLng latLng = station.getPosition();
    			float[] results = new float[1];
    			Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), 
    					latLng.latitude, latLng.longitude, results);
    			return Math.round(results[0]);
    		}
    	}
    	return 0;
    }
	
	
	//----------------- Instance fields ------------------
	
	private static Context context;	
	private static LocationClient locationClient;
	
	//----------------- Instance methods ------------------
	
	private LocationClientDelegate(){
		
	}
	
	
	//----------------- Interface Implementation ------------------
	
	public static class Listener implements LocationListener,
											ConnectionCallbacks,
											OnConnectionFailedListener{
		
		
	    private static final LocationRequest REQUEST = LocationRequest.create()
				  .setInterval(5000)
				  .setFastestInterval(16)
				  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	    
	
		@Override
		public void onConnectionFailed(ConnectionResult result) {
			Helper.showMessage(context, context.getString(R.string.msg_no_gps));
		}
		@Override
		public void onConnected(Bundle connectionHint) {
			locationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
		}
		@Override
		public void onDisconnected() {
		}
		@Override
		public void onLocationChanged(Location location) {
		}
	}
}
