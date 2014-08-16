package trouve.mon.velib.util;

import trouve.mon.velib.R;
import android.app.ActionBar;
import android.app.Activity;

public class Helper {

	public static final void setUpActionBarCustomTheme(Activity activity) {
		activity.setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = activity.getActionBar();
	    actionBar.setDisplayShowTitleEnabled(false);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(true);
	}
	
	
}
