package trouve.mon.velib.station;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import trouve.mon.velib.R;
import trouve.mon.velib.util.Helper;
import trouve.mon.velib.util.LocationClientManager;
import trouve.mon.velib.util.MyPreferenceManager;
import android.R.bool;
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;


public class StationManager {
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = StationManager.class.getName();
	
	public static final StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 2000;
	
	private static final float DELTA = 0.006f;
	private static final float MAX_DELTA = 0.0121f;
	
	//----------------- Static Methods ------------------
	
	//----------------- Instance Fields ------------------
	
	private SparseArray<Station> stationMap = new SparseArray<Station>(STATION_COUNT);
	private Set<String> favorites;

	//----------------- Instance Methods ------------------
	
	private StationManager() {
		
	}
	
	public SparseArray<Station> getStationMap() {
		return stationMap;
	}
	
	public void add(Station station) {
		Station oldStation = stationMap.get(station.getNumber());
		if(oldStation != null){
			station.setFavorite(oldStation.isFavorite());
		}
		// first load
		else{ 
			if(favorites != null){
				station.setFavorite(favorites.contains(String.valueOf(station.getNumber())));
			}
		}
		stationMap.put(station.getNumber(), station);
	}
	
	public Station get(int index) {
		return stationMap.get(index);
	}
	
	public void loadFavorites(){
		Map<String, ?> favoriteMap = MyPreferenceManager.getFavoriteSharedPreferences().getAll();
		this.favorites = favoriteMap.keySet();
	}
	
	public void setFavorite(Station station, boolean newValue){
		if(newValue){
			Helper.showMessageQuick(R.string.fav_created);
		}else{
			Helper.showMessageQuick(R.string.fav_removed);
		}
		station.setFavorite(newValue);
		MyPreferenceManager.setFavorite(station, newValue);
	}
	
	public List<Station> getFavorites(){
		loadFavorites();
		List<Station> favoriteStations = new ArrayList<Station>(favorites.size());
		for(String s : favorites){
			Integer stationNumber = Integer.parseInt(s);
			if(stationNumber == null){
				Log.e(TAG, "Impossible to retrieve favorites from "+s);
			}else{
				Station station = get(stationNumber);
				LocationClientManager.distanceFromLastLocation(station);
				favoriteStations.add(station);
			}
		}
		
		Collections.sort(favoriteStations, new StationComparator());
		return favoriteStations;
	}
	
	// WARNING !!!
	// TODO we shall wait for station list first load to be completed
	public List<Station> getNearByStationsSmart(){
		List<Station> closeStations = new ArrayList<Station>(100);
		if(!LocationClientManager.getClient().isConnected()){
			Helper.showMessageLong(R.string.msg_waiting_gps);
		}
		else{
			
			float delta = DELTA;	
			Location lastLocation = LocationClientManager.getClient().getLastLocation();
			boolean finished = false;
			while( ! finished){
				for (int i = 0, nsize = stationMap.size(); i < nsize; i++) {
					Station station = stationMap.valueAt(i);
					if(Math.abs(station.getPosition().latitude - lastLocation.getLatitude()) < delta &&
						Math.abs(station.getPosition().longitude - lastLocation.getLongitude()) < delta ){
						LocationClientManager.distanceFromLastLocation(station);
						closeStations.add(station);
					}
				}
				if(delta > MAX_DELTA || closeStations.size() >= 3){
					finished = true;
				}
				else {
					closeStations.clear();
					delta += DELTA;
				}
			}
		}
		
		Collections.sort(closeStations, new StationComparator());
		return closeStations;
	}
	
	
	static class StationComparator implements Comparator<Station>{
		 
	    @Override
	    public int compare(Station s1, Station s2) {
	        return (s1.getCacheDistanceFromLocation() > s2.getCacheDistanceFromLocation() ? 1 : 
	        	(s1.getCacheDistanceFromLocation() == s2.getCacheDistanceFromLocation() ? 0 : -1));
	    }
	} 
	
}
