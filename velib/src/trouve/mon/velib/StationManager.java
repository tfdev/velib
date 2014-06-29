package trouve.mon.velib;

import java.io.IOException;
import java.io.InputStream;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.MarkerOptions;

import android.util.Log;
import android.util.SparseArray;


public class StationManager {
	
	private static final String TAG = StationManager.class.getName();
	
	public static StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 1800;
	
	private static final BitmapDescriptor MARKER_BLUE = BitmapDescriptorFactory.fromResource(R.drawable.markerblue);
	private static final BitmapDescriptor MARKER_RED = BitmapDescriptorFactory.fromResource(R.drawable.markerred);
	
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
	            		 	 .icon(MARKER_BLUE);
		    }else{
		    	markerOptions.snippet("Station fermŽe")
  		 			  		 .icon(MARKER_RED);
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
