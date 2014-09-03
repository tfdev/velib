package trouve.mon.velib.station;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import trouve.mon.velib.AboutActivity;
import trouve.mon.velib.R;
import trouve.mon.velib.ResourceFactory;
import trouve.mon.velib.contract.Contract;
import trouve.mon.velib.contract.ContractListActivity;
import trouve.mon.velib.util.Formatter;
import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;


public class MapActivity extends Activity implements 	ConnectionCallbacks,
														OnConnectionFailedListener,
														LocationListener,
														OnMyLocationButtonClickListener, 
														OnCameraChangeListener, 
														OnMarkerClickListener, 
														OnMapClickListener, 
														OnMapLongClickListener{
	

	
	//----------------- Static Fields ------------------
	
	private static final String TAG = MapActivity.class.getName();
	

	private static final long REFRESH_PERIOD = 60; //SECONDS
	private static final float CENTER_ZOOM_LEVEL = 15.5f;
    private static final LocationRequest REQUEST = LocationRequest.create()
    															  .setInterval(5000)
            													  .setFastestInterval(16)
            													  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    //-----------------  Instance Fields ------------------
    
    private ResourceFactory resourceFactory;
    private GoogleMap map;
    private LocationClient locationClient;	
    
    private Menu menu;
	private Animation refreshButtonAnimation;
	
	private boolean refreshing = false;

	private boolean centering = false;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;
	
	private View locationButton;
	
	// Station Details
	private View detailContainerView;
	private TextView distanceTextView;
	private TextView bikeTextView;
	private TextView standTextView;
	private TextView stationTextView;
	private ImageView bikeImageView;
	private ImageView standImageView;
	private ImageButton favImageView;
	
	private MarkerManager markerManager;
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setActionBarTitle();
		loadFavorites();
		setUpResourceDelegate();
		setContentView(R.layout.map_activity);
		horribleHackToMoveMyLocationButton();
		setUpStationDetailView();
	}

	@Override
    protected void onResume() {
        super.onResume();
        if(getPreferredContract() != null){
        	setUpMapIfNeeded();
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
        if(markerManager != null){
        	hideDetails();
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
	        case R.id.action_about:
	        	startAboutActivity();
	            return true;
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
	//-----------------  Instance Methods ------------------
	
	//TODO fixme
	/*
	private void retrieveExtraInfo(){
		Bundle bundle = getIntent().getExtras();
		if( bundle != null && 
			bundle.getInt(ContractListActivity.EXTRA_CODE) == ContractListActivity.REQUEST_CODE_MOVE_AWAY_IF_EMPTY){
			actionIfNoStation = true;
		}
	}*/
	
	public MarkerManager getMarkerManager(){
		return markerManager;
	}
	
    private void setUpResourceDelegate(){
    	if(resourceFactory == null){
    		resourceFactory = new ResourceFactory(getResources());
    	}
    }
    
	private void startRefresh(){
		if(!refreshing){
			refreshing = true;
			scheduleUpdateData();
		}
	}
	
	private void setUpStationDetailView(){
		getLayoutInflater().inflate(R.layout.station_row, (ViewGroup) findViewById(R.id.map));
		detailContainerView = findViewById(R.id.detail_layout);
		detailContainerView.setVisibility(View.INVISIBLE);
		detailContainerView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {}
		});
		
		distanceTextView = (TextView) findViewById(R.id.distance);
		bikeTextView = (TextView) findViewById(R.id.bike_number);
		standTextView = (TextView) findViewById(R.id.parking_number);
		stationTextView = (TextView) findViewById(R.id.station_info);
		bikeImageView = (ImageView) findViewById(R.id.bike);
		standImageView = (ImageView) findViewById(R.id.parking);
		favImageView = (ImageButton) findViewById(R.id.favorite);
		favImageView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Station station = StationManager.INSTANCE.get(markerManager.detailedStationNumber);
				setFavorite(station, !station.isFavorite());
				setFavImageOnDetailView(station);
			}
		});
		
		int color = getResources().getColor(R.color.logo_blue);
		bikeTextView.setTextColor(color);
		bikeImageView.setColorFilter(color);
		standTextView.setTextColor(color);
		standImageView.setColorFilter(color);
	}
	
	private void setFavImageOnDetailView(Station station){
		if(station.isFavorite())
			favImageView.setImageResource(R.drawable.ic_fav);
		else
			favImageView.setImageResource(R.drawable.ic_action_not_important);			
	}
	
	private void showDetails(Marker marker){	
		int stationNumber = Integer.parseInt(marker.getTitle());
		Station station = StationManager.INSTANCE.get(stationNumber);
		setFavImageOnDetailView(station);
		centerMap(station);
		updateDetailInfo(station);
		setDetailViewVisible(true);
		
		markerManager.highlightMarker(stationNumber);
	}
	
	private void hideDetails() {
		if(markerManager.detailing){
			setDetailViewVisible(false);
			markerManager.unhighlightMarker();
		}
	}
	
	public void refreshDetails(){
		if(markerManager.detailing){
			updateDetailInfo(StationManager.INSTANCE.get(markerManager.detailedStationNumber));
		}
	}
	
	private void centerMap(Station station){
		centering = true;
		map.animateCamera(CameraUpdateFactory.newLatLng(station.getPosition()), 500, null);
	}
	
	private void displayDistanceDetail(Station station){
		int distance = distanceFromLastLocation(station);
		distanceTextView.setText(Formatter.formatDistance(distance, this));
	}
	
	private void updateDetailInfo(Station station) {
		int bikes = station.getAvailableBikes();
		int stands = station.getAvailableBikeStands();
		
		bikeTextView.setText(String.valueOf(bikes));
		standTextView.setText(String.valueOf(stands));
		
		displayDistanceDetail(station);
		
		stationTextView.setText(station.toString());
	}
	
	public void slideToTop(View view) {
		if (view.getVisibility() != View.VISIBLE) {
			TranslateAnimation animate = new TranslateAnimation(0, 0, view.getHeight(), 0);
			animate.setDuration(500);
			view.startAnimation(animate);
			view.setVisibility(View.VISIBLE);
		}
	}

	public void slideToBottom(View view) {
		TranslateAnimation animate = new TranslateAnimation(0, 0, 0, view.getHeight());
		animate.setDuration(500);
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	
	private void setDetailViewVisible(boolean visible){
		moveMyLocationButton(visible);
		if(visible){
			slideToTop(detailContainerView);
		}else{
			slideToBottom(detailContainerView);
		}
	}
	
	private void moveMyLocationButton(boolean detailViewVisible) {  
	    try{
		    RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
		    int detailSize = detailViewVisible ? getResources().getDimensionPixelSize(R.dimen.detail_height) : 0;
		    relativeLayoutParams.bottomMargin = 30 + detailSize;
		    locationButton.setLayoutParams(relativeLayoutParams);
	    }
	    catch(Exception e){
	    	Log.e(TAG, "not able to move myLocation button", e);
	    }
	}
	
	private void horribleHackToMoveMyLocationButton() {
	    View mapView = findViewById(R.id.map);
	    try{
		    // Get the button view 
		    locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
		    // and next place it, for example, on bottom right (as Google Maps app)
		    RelativeLayout.LayoutParams relativeLayoutParams = (RelativeLayout.LayoutParams) locationButton.getLayoutParams();
		    // position on right bottom
		    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0);
		    relativeLayoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
		    relativeLayoutParams.setMargins(0, 0, 30, 30);
	    }
	    catch(Exception e){
	    	Log.e(TAG, "not able to move myLocation button", e);
	    }
	}
	
    private void setUpMapIfNeeded() {
        if (map == null) {
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            if (map != null) {
            	markerManager = new MarkerManager(map, resourceFactory);
                map.setMyLocationEnabled(true);
                map.setOnMyLocationButtonClickListener(this);
                map.getUiSettings().setZoomControlsEnabled(false);
                map.setOnCameraChangeListener(this);
                map.setOnMarkerClickListener(this);
                map.setOnMapClickListener(this);
                map.setOnMapLongClickListener(this);
            }
        }
    }
    
    private void setUpAndConnectLocationClient() {
        if (locationClient == null) {
            locationClient = new LocationClient(getApplicationContext(), this,  // ConnectionCallbacks
                    													 this); // OnConnectionFailedListener    
        }
        locationClient.connect();
    }

    private boolean isGpsEnabled(){
    	 final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	 return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
	private int distanceFromLastLocation(Station station){
    	if(locationClient != null){
    		Location lastLocation = locationClient.getLastLocation();
    		if(lastLocation != null){
    			LatLng latLng = station.getPosition();
    			float[] results = new float[1];
    			Location.distanceBetween(lastLocation.getLatitude(), lastLocation.getLongitude(), 
    					latLng.latitude, latLng.longitude, results);
    			return Math.round(results[0]);
    		}
    	}
    	return 0;
    }
    
    private void centerMapOnMyLocation(boolean animateCamera){
    	if(!isGpsEnabled()){
    		showMessage(getString(R.string.msg_go_gps));
    	}
    	if(locationClient != null){
    		Location lastLocation = locationClient.getLastLocation();
    		if(lastLocation != null){
    			hideDetails();
        		LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        		if(animateCamera)
        			map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CENTER_ZOOM_LEVEL));
        		else
        			map.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, CENTER_ZOOM_LEVEL));
    		}
    		else {
    			showMessage(getString(R.string.msg_waiting_gps));
    		}
    	}
    }
    
	public void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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
		scheduledRefresh = scheduler.scheduleWithFixedDelay(new StationUpdater(this, getPreferredContract()), 0, REFRESH_PERIOD, TimeUnit.SECONDS);
	}
	
	private String getPreferredContract(){
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    String preferredContract = settings.getString(Contract.CONTRACT_PREFERENCE_KEY, null);
	    if(preferredContract == null){
	    	startConfigActivity(false);
	    }
	    return preferredContract;
	}
	
	private void setActionBarTitle(){
		setTheme(R.style.CustomActionBarTheme);
		ActionBar actionBar = getActionBar();
		actionBar.setTitle(getPreferredContract());
		actionBar.setSubtitle(getPreferredService());
	    actionBar.setDisplayShowTitleEnabled(true);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(false);
	}
	
	private String getPreferredService(){
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    String service = settings.getString(Contract.SERVICE_PREFERENCE_KEY, null);
	    if(service == null){
	    	startConfigActivity(false);
	    }
	    return service;
	}
	
	public void setFavorite(Station station, boolean isFavorite){
		station.setFavorite(isFavorite);
		markerManager.updateMarker(station);
		SharedPreferences.Editor editor = getFavoriteSharedPreferences().edit();
		if(isFavorite){
			editor.putBoolean(String.valueOf(station.getNumber()), true);
		}else{
			editor.remove(String.valueOf(station.getNumber()));
		}
		editor.apply();
	}
	
	
	// Needs to be called from onCreate()
	// before any station has been loaded
	private void loadFavorites(){
		StationManager.INSTANCE.setFavorites(getFavoriteSharedPreferences().getAll());
	}
	
	public SharedPreferences getFavoriteSharedPreferences(){
		// TODO favorite needs to contract specific
		// key needs to include the contract
		String contract = getPreferredContract();
	    return getSharedPreferences(Station.KEY_FAVORITE + contract, MODE_PRIVATE);
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
	
	private void startAboutActivity() {
		Intent intent = new Intent(this, AboutActivity.class);
		startActivity(intent);
	}
	
	
	//----------------- Interface Implementation ------------------
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ContractListActivity.REQUEST_CODE_USE_EXISTING_MAP){
			if(resultCode == RESULT_OK){
				setActionBarTitle();
				loadFavorites();
				StationManager.INSTANCE.getStationMap().clear();
				markerManager.resetAllMarkers();
				markerManager.actionIfNoStation = true;
			}
		}
	}
	
    @Override
    public boolean onMyLocationButtonClick() {
		centerMapOnMyLocation(true);
		return true;
	}
	@Override
	public void onCameraChange(CameraPosition position) {
		markerManager.refreshMarkers(false);
		if(!centering){
			hideDetails();
		}
		centering = false;
	}
	@Override
	public void onLocationChanged(Location location) {
	}
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		showMessage(getString(R.string.msg_no_gps));
	}
	@Override
	public void onConnected(Bundle connectionHint) {
		locationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
		centerMapOnMyLocation(false);
	}
	@Override
	public void onDisconnected() {
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		showDetails(marker);
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {
		hideDetails();
	}

	@Override
	public void onMapLongClick(LatLng point) {
		hideDetails();
	}


}
