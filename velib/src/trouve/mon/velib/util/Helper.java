package trouve.mon.velib.util;

import trouve.mon.velib.R;
import trouve.mon.velib.contract.Contract;
import trouve.mon.velib.station.Station;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

public class Helper {

	public static final void setUpActionBarCustomTheme(Activity activity) {
		activity.setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = activity.getActionBar();
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(true);
	}
	
	public static String getPreferredContract(Context activity){
		SharedPreferences settings = activity.getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, Context.MODE_PRIVATE);
	    String preferredContract = settings.getString(Contract.CONTRACT_PREFERENCE_KEY, null);
	    return preferredContract;
	}
	
	public static SharedPreferences getFavoriteSharedPreferences(Context activity){
		String contract = getPreferredContract(activity);
	    return activity.getSharedPreferences(Station.KEY_FAVORITE + contract, Context.MODE_PRIVATE);
	}
	
	public static String getPreferredService(Context activity){
		SharedPreferences settings = activity.getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, Context.MODE_PRIVATE);
	    String service = settings.getString(Contract.SERVICE_PREFERENCE_KEY, null);
	    return service;
	}
	
	public static void showMessage(Context context, String msg) {
		Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
	}
}
