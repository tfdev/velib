package trouve.mon.velib.station;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import trouve.mon.velib.util.json.JsonReader;
//import java.sql.Date;

import com.google.android.gms.maps.model.LatLng;

import android.util.Log;

/*
 [{
	"number": 19001,
	"name": "19001 - OURCQ CRIMEE",
	"address": "243 RUE DE CRIMEE - 75019 PARIS",
	"position": {
		"lat": 48.894104140316266,
		"lng": 2.372946088467279
	},
	"banking": true,
	"bonus": false,
	"status": "OPEN",
	"contract_name": "Paris",
	"bike_stands": 56,
	"available_bike_stands": 35,
	"available_bikes": 18,
	"last_update": 1403809815000
},
...
]

 */


public abstract class StationParser {
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = StationParser.class.getName();

	public static String ATTRIBUTE_NUMBER 				= "number";
	public static String ATTRIBUTE_NAME 				= "name";
	public static String ATTRIBUTE_ADDRESS 				= "address";
	public static String ATTRIBUTE_POSITION 			= "position";
	public static String ATTRIBUTE_LAT 					= "lat";
	public static String ATTRIBUTE_LNG 					= "lng";
	public static String ATTRIBUTE_BANKING 				= "banking";
	public static String ATTRIBUTE_BONUS 				= "bonus";
	public static String ATTRIBUTE_STATUS 				= "status";
	public static String ATTRIBUTE_BIKE_STANDS 			= "bike_stands";
	public static String ATTRIBUTE_AVAILABLE_BIKE_STANDS 	= "available_bike_stands";
	public static String ATTRIBUTE_AVAILABLE_BIKE 			= "available_bikes";
	public static String ATTRIBUTE_LAST_UPDATE 				= "last_update";
	
	//----------------- Static Methods ------------------
	
	public static void parse(InputStream inputStream) throws IOException {
		JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
	    try {
	    	readStationArray(reader);
	    }
	    finally {
	    	reader.close();
	    }
	}
	
	private static void readStationArray(JsonReader reader) throws IOException {
		reader.beginArray();
	    while (reader.hasNext()) {
	    	StationManager.INSTANCE.add(readStation(reader));
	    }
	    reader.endArray();
	}


	private static Station readStation(JsonReader reader) throws IOException {
		Station station = new Station();

	    reader.beginObject();
	    while (reader.hasNext()) {
	    	String attribute = reader.nextName();
	    	if(ATTRIBUTE_ADDRESS.equals(attribute)){
	    		station.setAddress(reader.nextString());
	    	}
	    	else if(ATTRIBUTE_AVAILABLE_BIKE.equals(attribute)){
	    		station.setAvailableBikes(reader.nextInt());
	    	}
	    	else if(ATTRIBUTE_AVAILABLE_BIKE_STANDS.equals(attribute)){
	    		station.setAvailableBikeStands(reader.nextInt());
	    	}	
			else if(ATTRIBUTE_BANKING.equals(attribute)){
				station.setBanking(reader.nextBoolean());    		
			}
			else if(ATTRIBUTE_BIKE_STANDS.equals(attribute)){
				station.setBikeStands(reader.nextInt());
			}
			else if(ATTRIBUTE_BONUS.equals(attribute)){
				station.setBonus(reader.nextBoolean()); 
			}
			//else if(ATTRIBUTE_LAST_UPDATE.equals(attribute)){
			//	station.setLastUpDate(new Date(reader.nextLong()));
			//}
			else if(ATTRIBUTE_NAME.equals(attribute)){
				station.setName(reader.nextString());
			}
			else if(ATTRIBUTE_NUMBER.equals(attribute)){
				station.setNumber(reader.nextInt());
			}
			else if(ATTRIBUTE_POSITION.equals(attribute)){
				station.setPosition(readPosition(reader));
			}
			else if(ATTRIBUTE_STATUS.equals(attribute)){
				station.setStatus(reader.nextString());
			}
			else {
				reader.skipValue();
		    }
	     }
	     reader.endObject();
	     return station;
	}
	
	private static LatLng readPosition(JsonReader reader) throws IOException {
		double latitude = 0;
		double longitude = 0;

	    reader.beginObject();
	    while (reader.hasNext()) {
	    	String attribute = reader.nextName();
	    	if(ATTRIBUTE_LAT.equals(attribute)){
	    		latitude = reader.nextDouble();
	    	}
	    	else if(ATTRIBUTE_LNG.equals(attribute)){
	    		longitude = reader.nextDouble();
	    	}
			else {
				Log.e(TAG, "Unknow position attribute: "+ attribute);
				reader.skipValue();
		    }
	     }
	     reader.endObject();
	     return new LatLng(latitude, longitude);
	}
	
	
}
