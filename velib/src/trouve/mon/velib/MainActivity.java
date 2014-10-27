package trouve.mon.velib;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import trouve.mon.velib.contract.ContractListActivity;
import trouve.mon.velib.station.DetailFragment;
import trouve.mon.velib.station.FavoriteListFragment;
import trouve.mon.velib.station.GoogleMapFragment;
import trouve.mon.velib.station.NearbyListFragment;
import trouve.mon.velib.station.StationManager;
import trouve.mon.velib.station.StationUpdater;
import trouve.mon.velib.util.Helper;
import trouve.mon.velib.util.LocationClientManager;
import trouve.mon.velib.util.MyPreferenceManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.Tab;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;

import com.google.android.gms.location.LocationClient;

public class MainActivity extends ActionBarActivity implements ActionBar.TabListener{

	//----------------- Static Fields ------------------

	private static final long REFRESH_PERIOD = 60; //SECONDS
	private static final String TAB_FAVORITES = "FAVORITES";
	private static final String TAB_NEARBY = "NEARBY";
	private static final String TAB_MAP = "MAP";
	
    //-----------------  Instance Fields ------------------
	
	private MenuItem menuItem;

	private boolean refreshing = false;
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;
	
	
	private GoogleMapFragment mapFragment;
	private DetailFragment detailFragment;
	private FavoriteListFragment favoriteFragment;
	private NearbyListFragment nearbyFragment;
	
	private LocationClient locationClient;
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Helper.setUp(getApplicationContext());
		MyPreferenceManager.setUp(getApplicationContext());
		if(MyPreferenceManager.getPreferredContract() == null){
        	startConfigActivity(false);
        	return;
        }
		
		loadFavorites();
		setActionBarTitle();
		setActionBarTabs();   
	}
	
	@Override
    public void onResume() {
        super.onResume();
        setUpAndConnectLocationClient();
        scheduleUpdateData();
    }

	@Override
    public void onPause() {
        super.onPause();
        disconnectLocationClient();
        cancelUpdateData();
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.main_actions, menu);
	    menuItem = menu.findItem(R.id.action_refresh);
	    return true;
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
        	locationClient = LocationClientManager.setUp(getApplicationContext(), getSupportFragmentManager());
        }
        locationClient.connect();
    }
	
	private void disconnectLocationClient() {
        if(locationClient != null){
        	locationClient.disconnect();
        }
    }
	
	private void setActionBarTitle(){
		setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = getSupportActionBar();
		actionBar.setSubtitle("");
		actionBar.setTitle(MyPreferenceManager.getPreferredService());
	    actionBar.setDisplayShowTitleEnabled(true);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(false);
	}
	
	private void setActionBarTabs(){
	    ActionBar actionBar = getSupportActionBar();
	    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

	    Tab tab = actionBar.newTab()
	    					.setTag(TAB_MAP)
	    					.setIcon(R.drawable.ic_action_map)
	    					.setTabListener(this);
	    actionBar.addTab(tab);
	    tab = actionBar.newTab()
				.setTag(TAB_FAVORITES)
				.setIcon(R.drawable.ic_action_important)
				.setTabListener(this);
	    actionBar.addTab(tab);
	    tab = actionBar.newTab()
				.setTag(TAB_NEARBY)
				.setIcon(R.drawable.ic_action_place)
				.setTabListener(this);
	    actionBar.addTab(tab);
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
	
	// TODO show a info bar 
	public boolean isGpsEnabled(){
		final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
		return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
   }
	
	// Needs to be called from onCreate()
	// before any station has been loaded
	private void loadFavorites(){
		StationManager.INSTANCE.loadFavorites();
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
	
	public void refresh(){
		if(mapFragment != null){
			mapFragment.refresh();
		}
		
		if(detailFragment != null){
			detailFragment.refresh();
		}
		
		if(favoriteFragment != null){
			favoriteFragment.refresh();
		}
		
		ActionBar actionBar = getSupportActionBar();
		actionBar.setSubtitle(getString(R.string.last_update) + " " + SimpleDateFormat.getTimeInstance().format(new Date()));
	}
	
	private void startRefresh(){
		if(!refreshing){
			refreshing = true;
			scheduleUpdateData();
		}
	}
	
    public void showRefreshing(){
    	refreshing = true;
    	if(menuItem != null){
        	MenuItemCompat.setActionView(menuItem, R.layout.refresh_view);
    	}
    }
    public void stopRefreshing(){
    	refreshing = false;
    	if(menuItem != null){
    		MenuItemCompat.setActionView(menuItem, null);
    	}
    }
    
	private void scheduleUpdateData(){
		cancelUpdateData();
		scheduledRefresh = scheduler.scheduleWithFixedDelay(new StationUpdater(this, MyPreferenceManager.getPreferredContract()), 0, REFRESH_PERIOD, TimeUnit.SECONDS);
	}
	
	private void cancelUpdateData(){
		if(scheduledRefresh != null){
        	scheduledRefresh.cancel(true);
        }
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
		
		if(TAB_MAP.equals(tab.getTag()) ){
			if(mapFragment == null){
	            mapFragment = (GoogleMapFragment) Fragment.instantiate(this, GoogleMapFragment.class.getName());
	            fragmentTransaction.add(android.R.id.content, mapFragment, GoogleMapFragment.MAP_FRAGMENT_TAG);
			}else{
				fragmentTransaction.attach(mapFragment);
			}
			if(detailFragment == null){
				detailFragment = (DetailFragment) Fragment.instantiate(this, DetailFragment.class.getName());
				fragmentTransaction.add(android.R.id.content, detailFragment, DetailFragment.DETAIL_FRAGMENT_TAG);
			}else{
				fragmentTransaction.attach(detailFragment);
			}
		}
		else if(TAB_FAVORITES.equals(tab.getTag())){
			if(favoriteFragment == null){
				favoriteFragment = (FavoriteListFragment) Fragment.instantiate(this, FavoriteListFragment.class.getName());
				fragmentTransaction.add(android.R.id.content, favoriteFragment, FavoriteListFragment.FAVORITE_FRAGMENT_TAG);
			}else{
				fragmentTransaction.attach(favoriteFragment);
			}
		}
		else if(TAB_NEARBY.equals(tab.getTag())){
			if(nearbyFragment == null){
				nearbyFragment = (NearbyListFragment) Fragment.instantiate(this, NearbyListFragment.class.getName());
				fragmentTransaction.add(android.R.id.content, nearbyFragment, NearbyListFragment.NEARBY_FRAGMENT_TAG);
			}else{
				fragmentTransaction.attach(nearbyFragment);
			}
		}
	}

    public void onTabUnselected(Tab tab, FragmentTransaction fragmentTransaction) {

    	if(TAB_MAP.equals(tab.getTag()) ){
	        if (mapFragment != null) {
	            fragmentTransaction.detach(mapFragment);
	        }
	        if (detailFragment != null) {
	            fragmentTransaction.detach(detailFragment);
	        }
    	}
    	else if(TAB_FAVORITES.equals(tab.getTag())){
			if(favoriteFragment != null){
				fragmentTransaction.detach(favoriteFragment);
			}
		}
		else if(TAB_NEARBY.equals(tab.getTag())){
			if(nearbyFragment != null){
				fragmentTransaction.detach(nearbyFragment);
			}
		}
    }

    public void onTabReselected(Tab tab, FragmentTransaction fragmentTransaction) {
        // User selected the already selected tab
    }
	
}
