package trouve.mon.velib;

import java.io.IOException;
import java.io.InputStream;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import android.util.Log;
import android.util.SparseArray;


public class StationManager {
	
	private static final String TAG = StationManager.class.getName();
	
	private static final int STATION_COUNT = 1800;
	public static StationManager INSTANCE = new StationManager();
	
	public static void update(InputStream inputStream){
		try {
			StationParser.parse(inputStream);
		} catch (IOException e) {
			Log.e(TAG, "Exception while parsing", e);
		}
	}
	
	public static void addMarkers(GoogleMap map) {		
		for(int i = 0, nsize = INSTANCE.stationMap.size(); i < nsize; i++) {
		    Station station = INSTANCE.stationMap.valueAt(i);
		    MarkerOptions markerOptions = new MarkerOptions()
									            .position(station.getPosition())
									            .title(station.getName());
									            
		    if(station.getStatus() == Status.OPEN){
		    	markerOptions.snippet(station.getAvailableBikes()+" vŽlos libres - "+
	            		 			  station.getAvailableBikeStands()+" emplacements libres")
	            		 	 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE));
		    }else{
		    	markerOptions.snippet("Station fermŽe")
  		 			  		 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
		    }
		    map.addMarker(markerOptions);
		}
	}
	
	
	
	private SparseArray<Station> stationMap = new SparseArray<Station>(STATION_COUNT);
	
	private StationManager() {
		
	}
	
	public boolean update() {
		return false;
	}
	
	public void add(Station station) {
		stationMap.put(station.getNumber(), station);
	}
	
	public Station get(int index) {
		return stationMap.get(index);
	}
	
	
}
