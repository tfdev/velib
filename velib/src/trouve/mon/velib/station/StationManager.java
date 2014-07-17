package trouve.mon.velib.station;

import android.util.SparseArray;


public class StationManager {
	
	//----------------- Static Fields ------------------
	
	//private static final String TAG = StationManager.class.getName();
	
	public static StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 1800;
	
	//----------------- Static Methods ------------------
	
	//----------------- Instance Fields ------------------
	
	private SparseArray<Station> stationMap = new SparseArray<Station>(STATION_COUNT);
	

	//----------------- Instance Methods ------------------
	
	private StationManager() {
		
	}
	
	public SparseArray<Station> getStationMap() {
		return stationMap;
	}
	
	public void add(Station station) {
		stationMap.put(station.getNumber(), station);
	}
	
	public Station get(int index) {
		return stationMap.get(index);
	}
	
	
}
