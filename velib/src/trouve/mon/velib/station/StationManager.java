package trouve.mon.velib.station;

import java.util.Map;

import android.util.SparseArray;


public class StationManager {
	
	//----------------- Static Fields ------------------
	
	//private static final String TAG = StationManager.class.getName();
	
	public static StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 2000;
	
	//----------------- Static Methods ------------------
	
	//----------------- Instance Fields ------------------
	
	private SparseArray<Station> stationMap = new SparseArray<Station>(STATION_COUNT);
	private Map<String, ?> favorites;

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
				station.setFavorite(favorites.containsKey(String.valueOf(station.getNumber())));
			}
		}
		stationMap.put(station.getNumber(), station);
	}
	
	public Station get(int index) {
		return stationMap.get(index);
	}
	
	public void setFavorites(Map<String, ?> favorites){
		this.favorites = favorites;
	}
	
}
