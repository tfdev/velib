package trouve.mon.velib;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import trouve.mon.velib.contract.ContractListActivity;
import trouve.mon.velib.station.DetailFragment;
import trouve.mon.velib.station.GoogleMapFragment;
import trouve.mon.velib.station.LocationClientDelegate;
import trouve.mon.velib.station.StationManager;
import trouve.mon.velib.station.StationUpdater;
import trouve.mon.velib.util.Helper;
import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.gms.location.LocationClient;

public class MainActivity extends Activity implements ActionBar.TabListener{

	//----------------- Static Fields ------------------

	private static final long REFRESH_PERIOD = 60; //SECONDS
	private static final String MAP_FRAGMENT_TAG = "MAP";

	
    //-----------------  Instance Fields ------------------
	
	private Menu menu;
	
	private Animation refreshButtonAnimation;

	private boolean refreshing = false;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;
	
	
	private GoogleMapFragment mapFragment;
	private DetailFragment detailFragment;
	
	private LocationClient locationClient;
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadFavorites();
		setActionBarTitle();
		
	    // Notice that setContentView() is not used, because we use the root
	    // android.R.id.content as the container for each fragment

	    // setup action bar for tabs
	    ActionBar actionBar = getActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
	    actionBar.setDisplayShowTitleEnabled(false);

	    Tab tab = actionBar.newTab()
	                       .setText("map") // TODO
	                       .setTabListener(this);
	    actionBar.addTab(tab);
	}
	
	@Override
    public void onResume() {
        super.onResume();
        if(Helper.getPreferredContract(this) == null){
        	startConfigActivity(false);
        }
        else{
        	setUpAndConnectLocationClient();
            scheduleUpdateData();
        }
    }

	@Override
    public void onPause() {
        super.onPause();
        if (locationClient != null) {
            locationClient.disconnect();
        }
        if(scheduledRefresh != null){
        	scheduledRefresh.cancel(true);
        }
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.map_activity_actions, menu);
	    menu.findItem(R.id.action_refresh).getActionView().setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startRefresh();
			}
		}); 
	    return super.onCreateOptionsMenu(menu);
	}
	
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    switch (item.getItemId()) {
	        case R.id.action_refresh:
	            startRefresh();
	            return true;
	        case R.id.action_settings:
	        	startConfigActivity(true);
	            return true;
	        /*case R.id.action_about:
	        	startAboutActivity();
	            return true;*/
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}
    
	@Override
	public void onAttachedToWindow() {
	    super.onAttachedToWindow();
	    Window window = getWindow();
	    window.setFormat(PixelFormat.RGBA_8888);
	}
	
	//-----------------  Activity Logic ------------------
	
	private void setUpAndConnectLocationClient() {
        if(locationClient == null){
        	locationClient = LocationClientDelegate.getClient(getApplicationContext());
        }
    }
	
	private void setActionBarTitle(){
		setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(Helper.getPreferredContract(this));
		actionBar.setSubtitle(Helper.getPreferredService(this));
	    actionBar.setDisplayShowTitleEnabled(true);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(false);
	}
	
	private void startConfigActivity(boolean keepActivity){
		Intent intent = new Intent(this, ContractListActivity.class);
		if(keepActivity){
			intent.putExtra(ContractListActivity.EXTRA_CODE, ContractListActivity.REQUEST_CODE_USE_EXISTING_MAP);
			startActivityForResult(intent, ContractListActivity.REQUEST_CODE_USE_EXISTING_MAP);
		}else{
			startActivity(intent);
			finish();
		}
	}
	
	// Needs to be called from onCreate()
	// before any station has been loaded
	private void loadFavorites(){
		StationManager.INSTANCE.setFavorites(Helper.getFavoriteSharedPreferences(this).getAll());
	}
	
	public void refresh(){
		if(mapFragment != null){
			mapFragment.refresh();
		}
		
		if(detailFragment != null){
			detailFragment.refresh();
		}
	}
	
	public void reset(){
		setActionBarTitle();
		loadFavorites();
		StationManager.INSTANCE.getStationMap().clear();
		
		if(mapFragment != null){
			mapFragment.reset();
		}
	}
	
	//----------------- Refresh logic ------------------
	
	private void startRefresh(){
		if(!refreshing){
			refreshing = true;
			scheduleUpdateData();
		}
	}
	
    public void showRefreshing(){
    	refreshing = true;
    	if(menu != null){
	        MenuItem item = menu.findItem(R.id.action_refresh);

        	if(refreshButtonAnimation == null){
        		refreshButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.refresh);
        		refreshButtonAnimation.setRepeatCount(Animation.INFINITE);
        	}
        	item.getActionView().findViewById(R.id.btn_refresh).startAnimation(refreshButtonAnimation);
    	}
    }
    public void stopRefreshing(){
    	refreshing = false;
    	if(menu != null){
	        MenuItem item = menu.findItem(R.id.action_refresh);
	        if(item.getActionView() != null){
	        	item.getActionView().findViewById(R.id.btn_refresh).clearAnimation();
	        } 
    	}
    }
    
	private void scheduleUpdateData(){
		if(scheduledRefresh != null){
        	scheduledRefresh.cancel(true);
        }
		scheduledRefresh = scheduler.scheduleWithFixedDelay(new StationUpdater(this, Helper.getPreferredContract(this)), 0, REFRESH_PERIOD, TimeUnit.SECONDS);
	}
	
	/*private void startAboutActivity() {
	Intent intent = new Intent(this, AboutActivity.class);
	startActivity(intent);
	}*/
	
	//----------------- Interface Implementation ------------------
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ContractListActivity.REQUEST_CODE_USE_EXISTING_MAP){
			if(resultCode == RESULT_OK){
				reset();
			}
		}
	}
	
	public void onTabSelected(Tab tab, FragmentTransaction fragmentTransaction) {

		if(mapFragment == null){
            mapFragment = (GoogleMapFragment) Fragment.instantiate(this, GoogleMapFragment.class.getName());
		}
            
		fragmentTransaction.add(android.R.id.content, mapFragment, MAP_FRAGMENT_TAG);
    }

    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {
        if (mapFragment != null) {
            fragmentTransaction.remove(mapFragment);
        }
    }

    public void onTabReselected(Tab tab, FragmentTransaction ft) {
        // User selected the already selected tab
    }
	
}
