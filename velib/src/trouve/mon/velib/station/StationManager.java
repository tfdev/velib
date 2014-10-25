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
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;


public class StationManager {
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = StationManager.class.getName();
	
	public static final StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 2000;
	
	private static final float DELTA = 0.006f;
	
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
				favoriteStations.add(get(stationNumber));
			}
		}
		return favoriteStations;
	}
	
	public List<Station> getNearByStations(){
		List<Station> closeStations = new ArrayList<Station>(100);
		if(!LocationClientManager.getClient().isConnected()){
			Helper.showMessageLong(R.string.msg_waiting_gps);
		}
		else{
			int maxSize = 5;
			int maxDistance = LocationClientManager.distanceFromLastLocation(stationMap.valueAt(0));
			closeStations.add(stationMap.valueAt(0));
			for (int i = 1, nsize = stationMap.size(); i < nsize; i++) {
				Station station = stationMap.valueAt(i);
				int distance = LocationClientManager.distanceFromLastLocation(station);
				if(closeStations.size() < maxSize){
					
					// closeStation is sorted descending
					if(distance <= maxDistance){
						boolean added = false;
						for(int j = 1; !added && j < closeStations.size(); j++){
							if(distance >= closeStations.get(j).getDistanceFromLocation()){
								closeStations.add(j, station);
								added = true;
							}
						}
						if(!added){
							closeStations.add(station);
						}
					}
					else{ // it is a new maxDistance
						closeStations.add(0, station);
						maxDistance = distance;
					}
				}
				else{ 
					if(distance <= maxDistance){
						boolean added = false;
						for(int j = 1; !added && j < closeStations.size(); j++){
							if(distance >= closeStations.get(j).getDistanceFromLocation()){
								closeStations.add(j, station);
								added = true;
							}
						}
						if(!added){
							closeStations.add(station);
						}
					}
				}
			}
		}
		
		List<Station> ascending = new ArrayList<Station>(5);
		for(int i=closeStations.size()-1; i > 0 && ascending.size() < 5; i--){
			ascending.add(closeStations.get(i));
		}
		
		return ascending;
	}
	
	public List<Station> getNearByStationsSmart(){
		List<Station> closeStations = new ArrayList<Station>(100);
		if(!LocationClientManager.getClient().isConnected()){
			Helper.showMessageLong(R.string.msg_waiting_gps);
		}
		else{
			
			float delta = DELTA;
			// TODO enhance this !
			
			Location lastLocation = LocationClientManager.getClient().getLastLocation();
			for (int i = 0, nsize = stationMap.size(); i < nsize; i++) {
				Station station = stationMap.valueAt(i);
				if(Math.abs(station.getPosition().latitude - lastLocation.getLatitude()) < delta &&
					Math.abs(station.getPosition().longitude - lastLocation.getLongitude()) < delta ){
					LocationClientManager.distanceFromLastLocation(station);
					closeStations.add(station);
				}
			}
		}
		
		Collections.sort(closeStations, new StationComparator());
		return closeStations;
	}
	
	
	static class StationComparator implements Comparator<Station>{
		 
	    @Override
	    public int compare(Station s1, Station s2) {
	        return (s1.getDistanceFromLocation() > s2.getDistanceFromLocation() ? 1 : 
	        	(s1.getDistanceFromLocation() == s2.getDistanceFromLocation() ? 0 : -1));
	    }
	} 
	
}
