package trouve.mon.velib.station;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import trouve.mon.velib.R;
import trouve.mon.velib.ResourceDelegate;
import trouve.mon.velib.contract.Contract;
import trouve.mon.velib.contract.ContractListActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;


public class MapActivity extends Activity implements 	ConnectionCallbacks,
														OnConnectionFailedListener,
														LocationListener,
														OnMyLocationButtonClickListener, 
														OnCameraChangeListener, 
														OnMarkerClickListener, 
														OnMapClickListener, 
														OnMapLongClickListener{
	
	enum MarkerSize{
		TINY,
		MID,
		BIG
	}
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = MapActivity.class.getName();
	
	private static final float HIGHLIGHT_ALPHA = 0.5f;
	private static final float NORMAL_ALPHA = 1.0f;
	
	private static final float CENTER_ZOOM_LEVEL = 15.5f;
	private static final float MID_ZOOM_LEVEL = 14.8f;
	private static final float TINY_ZOOM_LEVEL = 13.8f;
	private static final float MAX_ZOOM_LEVEL = 13.0f;
	private static final long REFRESH_PERIOD = 60; //SECONDS
	
    private static final LocationRequest REQUEST = LocationRequest.create()
    															  .setInterval(5000)
            													  .setFastestInterval(16)
            													  .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    //-----------------  Instance Fields ------------------
    
    private ResourceDelegate delegate;
    private GoogleMap map;
    private LocationClient locationClient;	
    
    private Menu menu;
	private Animation refreshButtonAnimation;
	
	private SparseArray<Station> visibleMarkers = new SparseArray<Station>(20);
	private SparseArray<Station> midVisibleMarkers = new SparseArray<Station>(150);
	private SparseArray<Station> tinyVisibleMarkers = new SparseArray<Station>(600);
	
	private boolean refreshing = false;
	private boolean detailing = false;
	private boolean centering = false;
	private boolean onCreate = true;
	private boolean actionIfNoStation = false;
	
	private int detailedStationNumber;
	
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
	private ImageView favImageView;
	
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		loadFavorites();
		retrieveExtraInfo();
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
        hideDetails();
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
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_refresh:
	            startRefresh();
	            return true;
	        case R.id.action_settings:
	        	startConfigActivity(true);
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
	private void retrieveExtraInfo(){
		Bundle bundle = getIntent().getExtras();
		if( bundle != null && 
			bundle.getInt(ContractListActivity.EXTRA_CODE) == ContractListActivity.REQUEST_CODE_MOVE_AWAY_IF_EMPTY){
			actionIfNoStation = true;
		}
	}
	
    private void setUpResourceDelegate(){
    	if(delegate == null){
    		delegate = new ResourceDelegate(getResources());
    	}
    }
    
	private void startRefresh(){
		if(!refreshing){
			refreshing = true;
			scheduleUpdateData();
		}
	}
	
	private void setUpStationDetailView(){
		getLayoutInflater().inflate(R.layout.detail_bar_new_design, (ViewGroup) findViewById(R.id.map));
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
		favImageView = (ImageView) findViewById(R.id.favorite);
		favImageView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Station station = StationManager.INSTANCE.get(detailedStationNumber);
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
	
	private void hideDetails() {
		if(detailing){
			detailing = false;
			setDetailViewVisible(false);
			unhighlightMarker();
		}
	}
	
	private void setFavImageOnDetailView(Station station){
		if(station.isFavorite())
			favImageView.setImageResource(R.drawable.ic_fav);
		else
			favImageView.setImageResource(R.drawable.ic_action_not_important);			
	}
	
	private void showDetails(Marker marker){	
		detailing = true;
		int stationNumber = Integer.parseInt(marker.getTitle());
		detailedStationNumber = stationNumber;
		
		Station station = StationManager.INSTANCE.get(stationNumber);
		setFavImageOnDetailView(station);
		centerMap(station);
		updateDetailInfo(station);
		setDetailViewVisible(true);
		highlightMarker(marker);
	}
	
	public void refreshDetails(){
		if(detailing){
			updateDetailInfo(StationManager.INSTANCE.get(detailedStationNumber));
		}
	}
	
	private void centerMap(Station station){
		centering = true;
		map.animateCamera(CameraUpdateFactory.newLatLng(station.getPosition()), 500, null);
	}
	
	private void formatDistance(Station station){
		String distanceString = "";
		int distance = distanceFromLastLocation(station);
		if(distance != 0){
			distanceString = String.format("%d", distance)+ getString(R.string.meter);
		}
		distanceTextView.setText(distanceString);
	}
	
	private void updateDetailInfo(Station station) {
		int bikes = station.getAvailableBikes();
		int stands = station.getAvailableBikeStands();
		
		bikeTextView.setText(String.valueOf(bikes));
		standTextView.setText(String.valueOf(stands));
		
		formatDistance(station);
		
		stationTextView.setText(String.valueOf(station.getFormattedName()));
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
    
    
    //TODO should check for GC and leak
	@SuppressWarnings("unchecked")
	public void refreshMarkers(boolean forceRefresh) {
		if(this.map != null){	
			SparseArray<Station> stationsOnMap;
			MarkerSize size;
	
			if(map.getCameraPosition().zoom > MID_ZOOM_LEVEL){ 
				resetMarkers(tinyVisibleMarkers, midVisibleMarkers);
				stationsOnMap = visibleMarkers;
				size = MarkerSize.BIG;
			}else if(map.getCameraPosition().zoom > TINY_ZOOM_LEVEL){
				resetMarkers(visibleMarkers, tinyVisibleMarkers);
				stationsOnMap = midVisibleMarkers;	
				size = MarkerSize.MID;
			}else if(map.getCameraPosition().zoom > MAX_ZOOM_LEVEL){
				resetMarkers(visibleMarkers, midVisibleMarkers);
				stationsOnMap = tinyVisibleMarkers;
				size = MarkerSize.TINY;
			}else{
				resetMarkers(visibleMarkers, midVisibleMarkers, tinyVisibleMarkers);
				return;
			}
			
	        LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;
	    	SparseArray<Station> stations = StationManager.INSTANCE.getStationMap();
			for(int i = 0, nsize = stations.size(); i < nsize; i++) {
			    Station station = stations.valueAt(i);
			    Station stationOnMap = stationsOnMap.get(station.getNumber());
			    // if station is visible
	            if(bounds.contains(station.getPosition())){
	            	actionIfNoStation = false;
	                if(stationOnMap == null){ //if there is no marker yet 
	                	station.setMarker(addMarker(station, size));
	                	stationsOnMap.put(station.getNumber(), station);             		
	                }else if(forceRefresh){ //else if data have just been updated
	                	if(stationOnMap.isDifferent(station)){
		                	stationOnMap.getMarker().remove();
		                	station.setMarker(addMarker(station, size));
		                	stationsOnMap.put(station.getNumber(), station); 
	                	}else{
	                		station.setMarker(stationOnMap.getMarker());
	                		stationsOnMap.put(station.getNumber(), station); 
	                	}
	                }
	            }
	            else{ // station is not visible
	            	// if there is a marker and just updated
	            	if(stationOnMap != null && forceRefresh){
	            		if(stationOnMap.isDifferent(station)){
	            			stationOnMap.getMarker().remove();
		            		stationsOnMap.remove(station.getNumber());
	            		}
	                }
	            }
	        }
			
			goAwayIfNoStation();	
	    }
	}	
	
	private void goAwayIfNoStation(){
		if(actionIfNoStation){
			actionIfNoStation = false;
			centerMapFarAway();
		}
	}
	
	@SuppressWarnings("unchecked")
	private void resetAllMarkers(){
		resetMarkers(tinyVisibleMarkers, midVisibleMarkers, visibleMarkers);
	}
	
    @SuppressWarnings("unchecked")
	private void resetMarkers(SparseArray<Station>... arguments) {
    	for(SparseArray<Station> stations : arguments){
    		for(int i = 0, nsize = stations.size(); i < nsize; i++) {
    			stations.valueAt(i).getMarker().remove();
    		}
    		stations.clear();
    	}
	}
    
    private Marker addMarker(Station station, MarkerSize markerSize){
    	MarkerOptions markerOptions = new MarkerOptions().position(station.getPosition())
				.title(String.valueOf(station.getNumber()));	
    	
    	BitmapDescriptor descriptor = null;
    	switch(markerSize){
    		case TINY: descriptor = delegate.getTinyMarkerBitmapDescriptor(station); break;
    		case MID: descriptor = delegate.getMidMarkerBitmapDescriptor(station); break;
    		case BIG: descriptor = delegate.getBigMarkerBitmapDescriptor(station); break;
    	}
    	markerOptions.icon(descriptor);
    	if(detailing && detailedStationNumber != station.getNumber()){
    		markerOptions.alpha(HIGHLIGHT_ALPHA);
    	}
    	
		return map.addMarker(markerOptions);
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
    
    
    private void centerMapFarAway(){
    	SparseArray<Station> stations = StationManager.INSTANCE.getStationMap();
    	if(stations.size() != 0){
    		Station station = stations.valueAt(0);
    		map.animateCamera(CameraUpdateFactory.newLatLngZoom(station.getPosition(), TINY_ZOOM_LEVEL));
    	}
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
	
	private void highlightMarker(Marker marker){	
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++)
		    visibleMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		for(int i = 0, nsize = midVisibleMarkers.size(); i < nsize; i++)
			midVisibleMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		for(int i = 0, nsize = tinyVisibleMarkers.size(); i < nsize; i++)
			tinyVisibleMarkers.valueAt(i).getMarker().setAlpha(HIGHLIGHT_ALPHA);
		marker.setAlpha(NORMAL_ALPHA);
	}
	
	private void unhighlightMarker(){	
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++)
		    visibleMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
		for(int i = 0, nsize = midVisibleMarkers.size(); i < nsize; i++)
			midVisibleMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
		for(int i = 0, nsize = tinyVisibleMarkers.size(); i < nsize; i++)
			tinyVisibleMarkers.valueAt(i).getMarker().setAlpha(NORMAL_ALPHA);
	}
	
    public void showRefreshing(){
    	refreshing = true;
    	if(menu != null){
	        MenuItem item = menu.findItem(R.id.action_refresh);

        	if(refreshButtonAnimation == null){
        		refreshButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.refresh);
        		refreshButtonAnimation.setRepeatCount(Animation.INFINITE);
        	}
        	item.getActionView().startAnimation(refreshButtonAnimation);
    	}
    }
    public void stopRefreshing(){
    	refreshing = false;
        MenuItem item = menu.findItem(R.id.action_refresh);
        if(item.getActionView() != null){
        	item.getActionView().clearAnimation();
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
	
	public void setFavorite(Station station, boolean isFavorite){
		station.setFavorite(isFavorite);
		updateMarker(station);
		SharedPreferences.Editor editor = getFavoriteSharedPreferences().edit();
		if(isFavorite){
			editor.putBoolean(String.valueOf(station.getNumber()), true);
		}else{
			editor.remove(String.valueOf(station.getNumber()));
		}
		editor.apply();
	}
	
	private void updateMarker(Station station){
		MarkerSize size = null;
		if (map.getCameraPosition().zoom > MID_ZOOM_LEVEL) {
			size = MarkerSize.BIG;
		} else if (map.getCameraPosition().zoom > TINY_ZOOM_LEVEL) {
			size = MarkerSize.MID;
		} else if (map.getCameraPosition().zoom > MAX_ZOOM_LEVEL) {
			size = MarkerSize.TINY;
		}

		if (size != null) {
			station.getMarker().remove();
			station.setMarker(addMarker(station, size));
		}
	}
	
	// Needs to be called from onCreate()
	// before any station has been loaded
	private void loadFavorites(){
		StationManager.INSTANCE.setFavorites(getFavoriteSharedPreferences().getAll());
	}
	
	public SharedPreferences getFavoriteSharedPreferences(){
		// TODO favorite needs to contract specific
		// key needs to include the contract
		
	    return getSharedPreferences(Station.KEY_FAVORITE, MODE_PRIVATE);
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
	
	
	//----------------- Interface Implementation ------------------
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if(requestCode == ContractListActivity.REQUEST_CODE_USE_EXISTING_MAP){
			if(resultCode == RESULT_OK){
				StationManager.INSTANCE.getStationMap().clear();
				resetAllMarkers();
				actionIfNoStation = true;
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
		refreshMarkers(false);
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
		if(onCreate){
			centerMapOnMyLocation(false);
			onCreate = false;
		}
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
