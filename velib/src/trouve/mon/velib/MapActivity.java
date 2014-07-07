package trouve.mon.velib;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.Toast;


public class MapActivity extends Activity implements 	ConnectionCallbacks,
														OnConnectionFailedListener,
														LocationListener,
														OnMyLocationButtonClickListener, 
														OnCameraChangeListener{
	
	//----------------- Static Fields ------------------
	
	private static final float CENTER_ZOOM_LEVEL = 15.0f;
	private static final String TAG = MapActivity.class.getName();
	private static final String URL_STATION = "https://api.jcdecaux.com/vls/v1/stations?contract=Paris&apiKey=";
	private static final String API_KEY = "df89b09292638d3c4a2731f771db3f43c514685d";
	private static final long REFRESH_PERIOD = 60; //SECONDS
	
    private static final LocationRequest REQUEST = LocationRequest.create()
            .setInterval(5000)         // 5 seconds
            .setFastestInterval(16)    // 16ms = 60fps
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    
    //-----------------  Instance Fields ------------------
    
    private GoogleMap map;
    private LocationClient locationClient;	
	private Bitmap bitmap;
	private View refreshButton;
	private Animation refreshButtonAnimation;
	private SparseArray<Marker> visibleMarkers = new SparseArray<Marker>(20);
	private boolean refreshing = false;
	
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	@SuppressWarnings("rawtypes")
	private ScheduledFuture scheduledRefresh;

	
	
	//-----------------  Activity Lifecycle ------------------
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		horribleHackToMoveMyLocationButton();
		setUpRefreshButton();
	}
	
    @Override
    protected void onResume() {
        super.onResume();
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
					scheduleUpdateData();
				}
			}
		});
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
            }
        }
    }
  
	private Bitmap getBitmap() {
		if(bitmap == null){
			bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.markerblue, null);
		}
		return bitmap;
	}
	
	private BitmapDescriptor getMarkerBitmapDescriptor(int bikes, int stands) {
		
		Bitmap bitmap = getBitmap().copy(Bitmap.Config.ARGB_8888, true);
		Canvas canvas = new Canvas(bitmap);
		
		Paint textPaint = new Paint();
		textPaint.setTextAlign(Paint.Align.CENTER);
		textPaint.setTextSize(22);
		textPaint.setTypeface(Typeface.DEFAULT_BOLD);
		textPaint.setStyle(Style.FILL_AND_STROKE);
		
		textPaint.setColor(Color.argb(200, 0, 102, 204)); //TODO const
		canvas.drawText(""+bikes, bitmap.getWidth()/2, bitmap.getHeight()/2 - 22, textPaint);
		
		textPaint.setColor(Color.argb(200, 204, 204, 0)); //TODO const
		canvas.drawText(""+stands, bitmap.getWidth()/2, bitmap.getHeight()/2, textPaint);

		return BitmapDescriptorFactory.fromBitmap(bitmap);
	}
    
	private void refreshMarkers(boolean forceRefresh) {

		if(this.map != null){		
			if(map.getCameraPosition().zoom < 14.0f){ //TODO const
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
									            		.title(station.getFormattedName());
									            
    	if(station.getStatus() == Status.OPEN){
    		markerOptions.snippet(station.getAvailableBikes()+" vélos libres - "+ //TODO intl
	            		 		station.getAvailableBikeStands()+" emplacements libres") //TODO intl
	            		 .icon(getMarkerBitmapDescriptor(station.getAvailableBikes(), station.getAvailableBikeStands()));
    	}
    	else{
		    markerOptions.snippet("Station fermée") //TODO intl
  		 		  		 .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
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
    
	private void scheduleUpdateData(){
		
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		if (networkInfo == null || !networkInfo.isConnected()) {
			Toast.makeText(this, "Activez la connexion internet", Toast.LENGTH_LONG).show(); //TODO intl
		} 
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
								showMessage("Données mises à jour"); // TODO intl
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

	private void showMessage(String msg) {
		Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
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


}
