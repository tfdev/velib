package trouve.mon.velib;

import java.io.IOException;
import java.io.InputStream;

import android.util.Log;
import android.util.SparseArray;


public class StationManager {
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = StationManager.class.getName();
	
	public static StationManager INSTANCE = new StationManager();
	
	private static final int STATION_COUNT = 1800;
	
	//----------------- Static Methods ------------------
	
	public static void update(InputStream inputStream){
		try {
			StationParser.parse(inputStream);
		} catch (IOException e) {
			Log.e(TAG, "Exception while parsing", e);
		}
	}
	
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
