package trouve.mon.velib.util;

import trouve.mon.velib.contract.Contract;
import trouve.mon.velib.station.Station;
import android.content.Context;
import android.content.SharedPreferences;

public class MyPreferenceManager {

	
	private static Context context;
	
	public static void setUp(Context c){
		context = c;
	}
	
	public static String getPreferredContract(){
		checkInit();
		SharedPreferences settings = context.getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, Context.MODE_PRIVATE);
	    String preferredContract = settings.getString(Contract.CONTRACT_PREFERENCE_KEY, null);
	    return preferredContract;
	}
	
	public static SharedPreferences getFavoriteSharedPreferences(){
		checkInit();
		String contract = getPreferredContract();
	    return context.getSharedPreferences(Station.KEY_FAVORITE + contract, Context.MODE_PRIVATE);
	}
	
	public static String getPreferredService(){
		checkInit();
		SharedPreferences settings = context.getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, Context.MODE_PRIVATE);
	    String service = settings.getString(Contract.SERVICE_PREFERENCE_KEY, null);
	    return service;
	}
	
	public static void setFavorite(Station station, boolean newValue){
		checkInit();
		SharedPreferences.Editor editor = getFavoriteSharedPreferences().edit();
		if(newValue){
			editor.putBoolean(String.valueOf(station.getNumber()), true);
		}else{
			editor.remove(String.valueOf(station.getNumber()));
		}
		editor.apply();
	}
	
	public static boolean savePreferredService(String contractName, String serviceName){		
		checkInit();
		SharedPreferences settings = context.getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, Context.MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(Contract.CONTRACT_PREFERENCE_KEY, contractName);
	    editor.putString(Contract.SERVICE_PREFERENCE_KEY, serviceName);
	    return editor.commit();
	}
	
	private static void checkInit(){
		if(context == null){
			throw new ExceptionInInitializerError();
		}
	}
}
