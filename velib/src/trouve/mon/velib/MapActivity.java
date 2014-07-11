package trouve.mon.velib;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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
	
	//----------------- Static Fields ------------------
	
	private static final String TAG = MapActivity.class.getName();
	
	private static final String URL_STATION = "https://api.jcdecaux.com/vls/v1/stations?contract=Paris&apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	
	private static final int RED = Color.rgb(181, 12, 22);
	private static final int ORANGE = Color.rgb(215, 119, 34);
	private static final int GREEN = Color.rgb(133, 161, 82);
	private static final float HIGHLIGHT_ALPHA = 0.5f;
	private static final float NORMAL_ALPHA = 1.0f;
	
	private static final float CENTER_ZOOM_LEVEL = 15.5f;
	private static final float MIN_ZOOM_LEVEL = 14.5f;
	private static final long REFRESH_PERIOD = 60; //SECONDS
	
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    //-----------------  Instance Fields ------------------
    
    private GoogleMap map;
    private LocationClient locationClient;	
	private View refreshButton;
	private Animation refreshButtonAnimation;
	private SparseArray<Marker> visibleMarkers = new SparseArray<Marker>(20);
	
	private boolean refreshing = false;
	private boolean detailing = false;
	private boolean centering = false;
	
	private int detailedStationNumber;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;

	private Bitmap markerGreen;
	private Bitmap markerOrange;
	private Bitmap markerRed;
	
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
		setContentView(R.layout.activity_main);
		horribleHackToMoveMyLocationButton();
		setUpRefreshButton();
		setUpStationDetailView();
	}
	
    @Override
    protected void onResume() {
        super.onResume();
        setDetailViewVisible(false);
        setUpMapIfNeeded();
        setUpLocationClientIfNeeded();
        locationClient.connect();
        scheduleUpdateData();
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
    
	//-----------------  Instance Methods ------------------
    
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
		bikeTextView = (TextView) findViewById(R.id.bike_number);
		standTextView = (TextView) findViewById(R.id.parking_number);
		stationTextView = (TextView) findViewById(R.id.station_info);
		bikeImageView = (ImageView) findViewById(R.id.bike);
		standImageView = (ImageView) findViewById(R.id.parking);
	}
	
	private void hideDetails() {
		detailing = false;
		setDetailViewVisible(false);
		unhighlightMarker();
	}
	
	//TODO should listen to station changes
	private void showDetails(Marker marker){	
		int stationNumber = Integer.parseInt(marker.getTitle());
		Station station = StationManager.INSTANCE.get(stationNumber);
		centerMap(station);
		detailing = true;
		detailedStationNumber = station.getNumber();
		updateDetailInfo(station);
		setDetailViewVisible(true);
		highlightMarker(marker);
	}
	
	private void refreshDetails(){
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
			color = RED;
		}else if(bikes <= 3){
			color = ORANGE;
		}else{
			color = GREEN;
		}
		bikeTextView.setTextColor(color);
		bikeImageView.setColorFilter(color);
		if(stands == 0){
			color = RED;
		}else if(stands <= 3){
			color = ORANGE;
		}else{
			color = GREEN;
		}
		standTextView.setTextColor(color);
		standImageView.setColorFilter(color);
		
		stationTextView.setText(String.valueOf(station.getFormattedName()));
	}
	
	private void setDetailViewVisible(boolean visible){
		int normalSize = getResources().getDimensionPixelSize(R.dimen.refresh_top_margin_standard);	
		RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) refreshButton.getLayoutParams();
		
		if(visible){
			detailContainerView.setVisibility(View.VISIBLE);
			int detailSize = getResources().getDimensionPixelSize(R.dimen.detail_height);
			layoutParams.topMargin = detailSize + normalSize;
		}else{
			detailContainerView.setVisibility(View.INVISIBLE);
			layoutParams.topMargin =  normalSize;
		}
		refreshButton.setLayoutParams(layoutParams);
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
        // Do a null check to confirm that we have not already instantiated the map.
        if (map == null) {
            // Try to obtain the map from the MapFragment.
            map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
            // Check if we were successful in obtaining the map.
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

	private Bitmap getMarkerGreen() {
		if(markerGreen == null){
			markerGreen = BitmapFactory.decodeResource(getResources(), R.drawable.markergreen, null);
		}
		return markerGreen;
	}
	private Bitmap getMarkerOrange() {
		if(markerOrange == null){
			markerOrange = BitmapFactory.decodeResource(getResources(), R.drawable.markerorange, null);
		}
		return markerOrange;
	}
	private Bitmap getMarkerRed() {
		if(markerRed == null){
			markerRed = BitmapFactory.decodeResource(getResources(), R.drawable.markerred, null);
		}
		return markerRed;
	}
	private int getCenterClosed(){
		return getResources().getDimensionPixelSize(R.dimen.center_closed);
	}
	private int getTextSize(){
		return getResources().getDimensionPixelSize(R.dimen.text_size);
	}
	private int getCenterStand(){
		return getResources().getDimensionPixelSize(R.dimen.center_stand);
	}
	private int getCenterBike(){
		return getResources().getDimensionPixelSize(R.dimen.center_bike);
	}
	private BitmapDescriptor getClosedStationMarkerBitmapDescriptor() { // TODO refactor
		Bitmap bitmap = getMarkerRed().copy(Bitmap.Config.ARGB_8888, true);

		Canvas canvas = new Canvas(bitmap);		
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(getTextSize());
		textPaint.setTypeface(Typeface.MONOSPACE);
		textPaint.setColor(RED);
		canvas.drawText("0", bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterClosed(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	private BitmapDescriptor getMarkerBitmapDescriptor(int bikes, int stands) {

		int color;
		Bitmap bitmap = null;
		if(bikes == 0 || stands == 0){
			bitmap = getMarkerRed().copy(Bitmap.Config.ARGB_8888, true);
			color = RED;
		}else if (bikes <= 3 || stands <= 3){
			bitmap = getMarkerOrange().copy(Bitmap.Config.ARGB_8888, true);
			color = ORANGE;
		}else{
			bitmap = getMarkerGreen().copy(Bitmap.Config.ARGB_8888, true);
			color = GREEN;
		}

		Canvas canvas = new Canvas(bitmap);		
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(getTextSize());
		textPaint.setTypeface(Typeface.MONOSPACE);
		textPaint.setColor(color);
		canvas.drawText(String.valueOf(bikes), bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterBike(), textPaint);
		canvas.drawText(String.valueOf(stands), bitmap.getWidth()/2, bitmap.getHeight()/2 + getCenterStand(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
    
	private void refreshMarkers(boolean forceRefresh) {

		if(this.map != null){		
			if(map.getCameraPosition().zoom < MIN_ZOOM_LEVEL){ //TODO const
				resetMarkers();
				return;
			}
			
	        LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;
	    	SparseArray<Station> stationMap = StationManager.INSTANCE.getStationMap();
			for(int i = 0, nsize = stationMap.size(); i < nsize; i++) {
			    Station station = stationMap.valueAt(i);
	            if(bounds.contains(station.getPosition())){
	                if(visibleMarkers.get(station.getNumber()) == null){
	                	visibleMarkers.put(station.getNumber(), addMarker(station));
	                }else if(forceRefresh){
	                	visibleMarkers.get(station.getNumber()).remove();
	                	visibleMarkers.put(station.getNumber(), addMarker(station));
	                }
	            }
	            else{
	            	if(visibleMarkers.get(station.getNumber()) != null){
	            		visibleMarkers.get(station.getNumber()).remove();
	            		visibleMarkers.remove(station.getNumber());
	                }
	            }
	        }
	    }
	}	
	
    private void resetMarkers() {
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++) {
			visibleMarkers.valueAt(i).remove();
		}
		visibleMarkers.clear();
	}

    private Marker addMarker(Station station) {
    	MarkerOptions markerOptions = new MarkerOptions().position(station.getPosition())
    													.title(String.valueOf(station.getNumber()));
									            
    	if(station.getStatus() == Status.OPEN){
    		markerOptions.icon(getMarkerBitmapDescriptor(station.getAvailableBikes(), station.getAvailableBikeStands()));
    	}
    	else{
		    markerOptions.icon(getClosedStationMarkerBitmapDescriptor());
    	}
    	
    	if(detailing && detailedStationNumber != station.getNumber()){
    		markerOptions.alpha(HIGHLIGHT_ALPHA);
    	}
    	
		return map.addMarker(markerOptions);
	}
    
    private void setUpLocationClientIfNeeded() {
        if (locationClient == null) {
            locationClient = new LocationClient(getApplicationContext(), this,  // ConnectionCallbacks
                    													 this); // OnConnectionFailedListener    
        }
    }

    private boolean isGpsEnabled(){
    	 final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    	 return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    private void centerMapOnMyLocation(){
    	if(!isGpsEnabled()){
    		showMessage("Activez le GPS"); //TODO intl
    	}
    	else if(locationClient != null){
    		Location lastLocation = locationClient.getLastLocation();
    		if(lastLocation != null){
        		LatLng latLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        		map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, CENTER_ZOOM_LEVEL));
    		}
    		else {
    			Toast.makeText(this, "En attente d'informations de localisation...", Toast.LENGTH_LONG).show(); //TODO intl
    		}
    	}
    }
    
	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
	}
	
	private void highlightMarker(Marker marker){	
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++) {
		    visibleMarkers.valueAt(i).setAlpha(HIGHLIGHT_ALPHA);
		}
		marker.setAlpha(1.0f);
	}
	
	private void unhighlightMarker(){	
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++) {
		    visibleMarkers.valueAt(i).setAlpha(NORMAL_ALPHA);
		}
	}
	
    
    //------------------- Handling Station Data --------------------- 
    
	private void scheduleUpdateData(){
		
		/*ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			Toast.makeText(this, "Activez la connexion internet", Toast.LENGTH_LONG).show(); //TODO intl
		} */
		if(scheduledRefresh != null){
        	scheduledRefresh.cancel(true);
        }
		scheduledRefresh = scheduler.scheduleWithFixedDelay(getRefreshRunnable(), 0, REFRESH_PERIOD, TimeUnit.SECONDS);
	}
	
	private Runnable getRefreshRunnable() {
		return new Runnable() {
			public void run() {
				HttpURLConnection connection = null;
				runOnUiThread(new Runnable() {
					public void run() {
						refreshing = true;
						refreshButton.startAnimation(refreshButtonAnimation);
					}
				});
				try {
					URL url = new URL(URL_STATION+API_KEY);
			        connection = (HttpURLConnection) url.openConnection();
			        connection.setReadTimeout(10000 /* milliseconds */);
			        connection.setConnectTimeout(15000 /* milliseconds */);
			        connection.setRequestMethod("GET");
			        connection.setDoInput(true);
			        connection.connect();
			        if(connection.getResponseCode() == 200){
			        	StationParser.parse(connection.getInputStream());
				        runOnUiThread(new Runnable() {
							public void run() {
								refreshMarkers(true);
								refreshDetails();
							}
						});
			        }
			        else{
			        	runOnUiThread(new Runnable() {
							public void run() {
								showMessage("Service indisponible"); // TODO intl
							}
						});
			        }
				}
				catch (Exception e) {
					Log.e(TAG, "Exception while downloading info", e);
					runOnUiThread(new Runnable() {
						public void run() {
							showMessage("Vérifiez votre connexion internet"); // TODO intl
						}
					});
				}
				finally{
					if(connection != null){
						connection.disconnect();
					}
					runOnUiThread(new Runnable() {
						public void run() {
							refreshing = false;
							refreshButton.clearAnimation();
						}
					});
				}
			}
		};
	}
	
	
	//----------------- Interface Implementation ------------------
    
    @Override
    public boolean onMyLocationButtonClick() {
		centerMapOnMyLocation();
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
		// TODO Auto-generated method stub	
	}
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		// TODO Auto-generated method stub
	}
	@Override
	public void onConnected(Bundle connectionHint) {
		locationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
		centerMapOnMyLocation();
	}
	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub	
	}

	@Override
	public boolean onMarkerClick(Marker marker) {
		showDetails(marker);
		return true;
	}

	@Override
	public void onMapClick(LatLng point) {
		// TODO should check if tap on detail bar
		hideDetails();
	}

	@Override
	public void onMapLongClick(LatLng point) {
		// TODO should check if tap on detail bar
		hideDetails();
	}


}
