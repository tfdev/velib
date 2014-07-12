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
	
	enum MarkerSize{
		TINY,
		MID,
		BIG
	}
	
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
	private static final float MID_ZOOM_LEVEL = 14.8f;
	private static final float TINY_ZOOM_LEVEL = 13.8f;
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
	private SparseArray<Marker> midVisibleMarkers = new SparseArray<Marker>(100);
	private SparseArray<Marker> tinyVisibleMarkers = new SparseArray<Marker>(2000);
	
	private boolean refreshing = false;
	private boolean detailing = false;
	private boolean centering = false;
	private boolean onCreate = true;
	
	private int detailedStationNumber;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;

	private Bitmap markerGreen;
	private Bitmap markerOrange;
	private BitmapDescriptor markerRed;
	private BitmapDescriptor markerGreenMid;
	private BitmapDescriptor markerOrangeMid;
	private BitmapDescriptor markerRedMid;
	private BitmapDescriptor markerGreenTiny;
	private BitmapDescriptor markerOrangeTiny;
	private BitmapDescriptor markerRedTiny;
	
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
        setUpMapIfNeeded();
        setUpAndConnectLocationClient();
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
		detailing = false;
		setDetailViewVisible(false);
		unhighlightMarker();
	}
	
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

    private BitmapDescriptor getMarkerGreenMid() {
		if(markerGreenMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_green, null);
			markerGreenMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenMid;
	}
	private BitmapDescriptor getMarkerOrangeMid() {
		if(markerOrangeMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_orange, null);
			markerOrangeMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeMid;
	}
	private BitmapDescriptor getMarkerRedMid() {
		if(markerRedMid == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mid_dot_red, null);
			markerRedMid = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedMid;
	}
    private BitmapDescriptor getMarkerGreenTiny() {
		if(markerGreenTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_green, null);
			markerGreenTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerGreenTiny;
	}
	private BitmapDescriptor getMarkerOrangeTiny() {
		if(markerOrangeTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_orange, null);
			markerOrangeTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerOrangeTiny;
	}
	private BitmapDescriptor getMarkerRedTiny() {
		if(markerRedTiny == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.tiny_dot_red, null);
			markerRedTiny = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRedTiny;
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
	private BitmapDescriptor getMarkerRedBitmapDescriptor() {
		if(markerRed == null){
			Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.markerred, null)
									.copy(Bitmap.Config.ARGB_8888, true);
			Canvas canvas = new Canvas(bitmap);		
			Paint textPaint = new Paint();
			textPaint.setTextAlign(Paint.Align.CENTER);
			textPaint.setTextSize(getBigTextSize());
			textPaint.setTypeface(Typeface.DEFAULT_BOLD);
			textPaint.setColor(RED);
			canvas.drawText("0", bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterClosed(), textPaint);

			markerRed = BitmapDescriptorFactory.fromBitmap(bitmap);
		}
		return markerRed;
	}
	private int getCenterClosed(){
		return getResources().getDimensionPixelSize(R.dimen.center_closed);
	}
	private int getTextSize(){
		return getResources().getDimensionPixelSize(R.dimen.text_size);
	}
	private int getBigTextSize(){
		return getResources().getDimensionPixelSize(R.dimen.big_text_size);
	}
	private int getCenterStand(){
		return getResources().getDimensionPixelSize(R.dimen.center_stand);
	}
	private int getCenterBike(){
		return getResources().getDimensionPixelSize(R.dimen.center_bike);
	}
	private BitmapDescriptor getOpenMarkerBitmapDescriptor(int bikes, int stands) {

		int color;
		Bitmap bitmap = null;
		if (bikes <= 3 || stands <= 3){
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
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setColor(color);
		canvas.drawText(String.valueOf(bikes), bitmap.getWidth()/2, bitmap.getHeight()/2 - getCenterBike(), textPaint);
		canvas.drawText(String.valueOf(stands), bitmap.getWidth()/2, bitmap.getHeight()/2 + getCenterStand(), textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
	private BitmapDescriptor getTinyMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), 
				stands=	station.getAvailableBikeStands();
		if(bikes == 0 && stands == 0){
			return getMarkerRedTiny();
		}else if (bikes <= 3 || stands <= 3){
			return getMarkerOrangeTiny();
		}else{
			return getMarkerGreenTiny();
		}
	}
	private BitmapDescriptor getMidMarkerBitmapDescriptor(Station station) {
		int bikes = station.getAvailableBikes(), 
			stands=	station.getAvailableBikeStands();
		if(bikes == 0 && stands == 0){
			return getMarkerRedMid();
		}else if (bikes <= 3 || stands <= 3){
			return getMarkerOrangeMid();
		}else{
			return getMarkerGreenMid();
		}
	}
    
	private void refreshMarkers(boolean forceRefresh) {
		if(this.map != null){	
			SparseArray<Marker> markers;
			MarkerSize size;
	
			if(map.getCameraPosition().zoom > MID_ZOOM_LEVEL){ 
				resetMarkers(tinyVisibleMarkers);
				resetMarkers(midVisibleMarkers);
				markers = visibleMarkers;
				size = MarkerSize.BIG;
			}else if(map.getCameraPosition().zoom > TINY_ZOOM_LEVEL){
				resetMarkers(visibleMarkers);
				resetMarkers(tinyVisibleMarkers);
				markers = midVisibleMarkers;	
				size = MarkerSize.MID;
			}else{
				resetMarkers(visibleMarkers);
				resetMarkers(midVisibleMarkers);
				markers = tinyVisibleMarkers;
				size = MarkerSize.TINY;
			}
			
	        LatLngBounds bounds = this.map.getProjection().getVisibleRegion().latLngBounds;
	    	SparseArray<Station> stationMap = StationManager.INSTANCE.getStationMap();
			for(int i = 0, nsize = stationMap.size(); i < nsize; i++) {
			    Station station = stationMap.valueAt(i);
	            if(bounds.contains(station.getPosition())){
	                if(markers.get(station.getNumber()) == null){
	                	markers.put(station.getNumber(), addMarker(station, size));             		
	                }else if(forceRefresh){// not necessary
	                	markers.get(station.getNumber()).remove();
	                	markers.put(station.getNumber(), addMarker(station, size)); 
	                }
	            }
	            else{
	            	if(markers.get(station.getNumber()) != null){
	            		markers.get(station.getNumber()).remove();
	            		markers.remove(station.getNumber());
	                }
	            }
	        }
	    }
	}	
	
    private void resetMarkers(SparseArray<Marker> markers) {
		for(int i = 0, nsize = markers.size(); i < nsize; i++) {
			markers.valueAt(i).remove();
		}
		markers.clear();
	}
    private Marker addMarker(Station station, MarkerSize markerSize){
    	MarkerOptions markerOptions = new MarkerOptions().position(station.getPosition())
				.title(String.valueOf(station.getNumber()));	
    	
    	BitmapDescriptor descriptor = null;
    	switch(markerSize){
    		case TINY: descriptor = getTinyMarkerBitmapDescriptor(station); break;
    		case MID: descriptor = getMidMarkerBitmapDescriptor(station); break;
    		case BIG: descriptor = getBigMarkerBitmapDescriptor(station); break;
    	}
    	markerOptions.icon(descriptor);
    	if(detailing && detailedStationNumber != station.getNumber()){
    		markerOptions.alpha(HIGHLIGHT_ALPHA);
    	}
    	
		return map.addMarker(markerOptions);
    }
    private BitmapDescriptor getBigMarkerBitmapDescriptor(Station station) {								            
    	if(station.getStatus() == Status.OPEN){
    		return getOpenMarkerBitmapDescriptor(station.getAvailableBikes(), station.getAvailableBikeStands());
    	}else{
		    return getMarkerRedBitmapDescriptor();
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
    
    private void centerMapOnMyLocation(){
    	if(!isGpsEnabled()){
    		showMessage("Activez le GPS"); //TODO intl
    	}
    	if(locationClient != null){
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
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++)
		    visibleMarkers.valueAt(i).setAlpha(HIGHLIGHT_ALPHA);
		for(int i = 0, nsize = midVisibleMarkers.size(); i < nsize; i++)
			midVisibleMarkers.valueAt(i).setAlpha(HIGHLIGHT_ALPHA);
		for(int i = 0, nsize = tinyVisibleMarkers.size(); i < nsize; i++)
			tinyVisibleMarkers.valueAt(i).setAlpha(HIGHLIGHT_ALPHA);
		marker.setAlpha(NORMAL_ALPHA);
	}
	
	private void unhighlightMarker(){	
		for(int i = 0, nsize = visibleMarkers.size(); i < nsize; i++)
		    visibleMarkers.valueAt(i).setAlpha(NORMAL_ALPHA);
		for(int i = 0, nsize = midVisibleMarkers.size(); i < nsize; i++)
			midVisibleMarkers.valueAt(i).setAlpha(NORMAL_ALPHA);
		for(int i = 0, nsize = tinyVisibleMarkers.size(); i < nsize; i++)
			tinyVisibleMarkers.valueAt(i).setAlpha(NORMAL_ALPHA);
	}
	
    
    //------------------- Handling Station Data --------------------- 
    
	private void scheduleUpdateData(){
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
	}
	@Override
	public void onConnectionFailed(ConnectionResult result) {
		showMessage("Connexion GPS impossible");
	}
	@Override
	public void onConnected(Bundle connectionHint) {
		locationClient.requestLocationUpdates(REQUEST, this);  // LocationListener
		if(onCreate){
			centerMapOnMyLocation();
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
