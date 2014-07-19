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
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
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
    
	private View refreshButton;
	private View configButton;
	private Animation refreshButtonAnimation;
	
	private SparseArray<Station> visibleMarkers = new SparseArray<Station>(20);
	private SparseArray<Station> midVisibleMarkers = new SparseArray<Station>(150);
	private SparseArray<Station> tinyVisibleMarkers = new SparseArray<Station>(600);
	
	private boolean refreshing = false;
	private boolean detailing = false;
	private boolean centering = false;
	private boolean onCreate = true;
	private boolean showMessageIfNoStation = false;
	
	private int detailedStationNumber;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;
	
	// Station Details
	private View detailContainerView;
	private TextView bikeTextView;
	private TextView standTextView;
	private TextView stationTextView;
	private ImageView bikeImageView;
	private ImageView standImageView;

	
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setUpResourceDelegate();
		setContentView(R.layout.map_activity);
		horribleHackToMoveMyLocationButton();
		setUpRefreshButton();
		setUpConfigButton();
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
    
	//-----------------  Instance Methods ------------------
	
    private void setUpResourceDelegate(){
    	if(delegate == null){
    		delegate = new ResourceDelegate(getResources());
    	}
    }
	
    private void setUpConfigButton() {
		configButton = findViewById(R.id.btn_config);
		configButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				startConfigActivity(true);
			}
		});
	}
    
	private void setUpRefreshButton(){
		refreshButton = findViewById(R.id.btn_refresh);
		refreshButtonAnimation = AnimationUtils.loadAnimation(this, R.anim.refresh);
		refreshButtonAnimation.setRepeatCount(Animation.INFINITE);
		
		refreshButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if(!refreshing){
					refreshing = true;
					scheduleUpdateData();
				}
			}
		});
	}
	
	private void setUpStationDetailView(){
		detailContainerView = findViewById(R.id.detail_layout);
		detailContainerView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {}
		});
		
		bikeTextView = (TextView) findViewById(R.id.bike_number);
		standTextView = (TextView) findViewById(R.id.parking_number);
		stationTextView = (TextView) findViewById(R.id.station_info);
		bikeImageView = (ImageView) findViewById(R.id.bike);
		standImageView = (ImageView) findViewById(R.id.parking);
	}
	
	private void hideDetails() {
		if(detailing){
			detailing = false;
			setDetailViewVisible(false);
			unhighlightMarker();
		}
	}
	
	private void showDetails(Marker marker){	
		detailing = true;
		int stationNumber = Integer.parseInt(marker.getTitle());
		detailedStationNumber = stationNumber;
		
		Station station = StationManager.INSTANCE.get(stationNumber);
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
	
	private void updateDetailInfo(Station station) {
		int bikes = station.getAvailableBikes();
		int stands = station.getAvailableBikeStands();
		
		bikeTextView.setText(String.valueOf(bikes));
		standTextView.setText(String.valueOf(stands));
		
		int color;
		if(bikes == 0){
			color = ResourceDelegate.RED;
		}else if(bikes <= 3){
			color = ResourceDelegate.ORANGE;
		}else{
			color = ResourceDelegate.GREEN;
		}
		bikeTextView.setTextColor(color);
		bikeImageView.setColorFilter(color);
		if(stands == 0){
			color = ResourceDelegate.RED;
		}else if(stands <= 3){
			color = ResourceDelegate.ORANGE;
		}else{
			color = ResourceDelegate.GREEN;
		}
		standTextView.setTextColor(color);
		standImageView.setColorFilter(color);
		
		stationTextView.setText(String.valueOf(station.getFormattedName()));
	}
	
	private void setDetailViewVisible(boolean visible){
		int refreshNormalSize = getResources().getDimensionPixelSize(R.dimen.refresh_top_margin_standard);	
		int configNormalSize = getResources().getDimensionPixelSize(R.dimen.config_top_margin_standard);	
		
		RelativeLayout.LayoutParams refreshLayoutParams = (RelativeLayout.LayoutParams) refreshButton.getLayoutParams();
		RelativeLayout.LayoutParams configLayoutParams = (RelativeLayout.LayoutParams) configButton.getLayoutParams();
		
		if(visible){
			detailContainerView.setVisibility(View.VISIBLE);
			int detailSize = getResources().getDimensionPixelSize(R.dimen.detail_height);
			refreshLayoutParams.topMargin = detailSize + refreshNormalSize;
			configLayoutParams.topMargin = detailSize + configNormalSize;
		}else{
			detailContainerView.setVisibility(View.GONE);
			refreshLayoutParams.topMargin =  refreshNormalSize;
			configLayoutParams.topMargin = configNormalSize;
		}
		refreshButton.setLayoutParams(refreshLayoutParams);
		configButton.setLayoutParams(configLayoutParams);
	}
	
	private void horribleHackToMoveMyLocationButton() {
	    View mapView = findViewById(R.id.map);
	    try{
		    // Get the button view 
		    View locationButton = ((View) mapView.findViewById(1).getParent()).findViewById(2);
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
	            	showMessageIfNoStation = false;
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
			
			showMessageIfNoStation();	
	    }
	}	
	
	private void showMessageIfNoStation(){
		if(showMessageIfNoStation){
			showMessageIfNoStation = false;
			String service = getPreferredService();
			String msg = getString(R.string.msg_no_station);
			if(service != null)
				msg= msg+" "+service;
			msg= msg+" "+getString(R.string.msg_here);
			showMessage(msg);
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
		refreshButton.startAnimation(refreshButtonAnimation);
    }
    public void stopRefreshing(){
    	refreshing = false;
		refreshButton.clearAnimation();
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
	
	private String getPreferredService(){
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    return settings.getString(Contract.SERVICE_PREFERENCE_KEY, null);
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
				showMessageIfNoStation = true;
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