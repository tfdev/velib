package trouve.mon.velib.station;

import trouve.mon.velib.R;
import trouve.mon.velib.ResourceFactory;
import trouve.mon.velib.util.Helper;
import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.location.LocationClient;
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


public class GoogleMapFragment extends MapFragment implements OnMyLocationButtonClickListener, 
															OnCameraChangeListener, 
															OnMarkerClickListener, 
															OnMapClickListener, 
															OnMapLongClickListener{
	

	
	//----------------- Static Fields ------------------
	
	private static final String TAG = GoogleMapFragment.class.getName();
	
	private static final float CENTER_ZOOM_LEVEL = 15.5f;

    
    //-----------------  Instance Fields ------------------
    
    private ResourceFactory resourceFactory;
    private GoogleMap map;	
    
	private boolean centering = false;
	
	private View locationButton;
	
	private MarkerManager markerManager;
	private LocationClient locationClient;
	
	//-----------------  Fragment Lifecycle ------------------
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

		View view = super.onCreateView(inflater, container, savedInstanceState);
		setUpResourceDelegate();
		horribleHackToMoveMyLocationButton();
		//setUpStationDetailView();
		return view;
	}

	@Override
    public void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        setUpLocationClient();
    }

	@Override
    public void onPause() {
        super.onPause();
        if(markerManager != null){
        	hideDetails();
        }
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
	
	private void setUpLocationClient(){
		if(locationClient == null){
			locationClient = LocationClientDelegate.getClient(getActivity());
			locationClient.registerConnectionCallbacks(new ConnectionCallbacks() {
				
				public void onDisconnected() {
				}
				
				public void onConnected(Bundle connectionHint) {
					centerMapOnMyLocation(false);
				}
			});
		}
	}
	
	public void refresh(){
		markerManager.refreshMarkers(true);
	}
	
	public void reset(){
		markerManager.resetAllMarkers();
		markerManager.actionIfNoStation = true;
	}
	
    private void setUpResourceDelegate(){
    	if(resourceFactory == null){
    		resourceFactory = new ResourceFactory(getResources());
    	}
    }
	
	private void showDetails(Marker marker){	
		int stationNumber = Integer.parseInt(marker.getTitle());
		Station station = StationManager.INSTANCE.get(stationNumber);
		centerMap(station);
		
		markerManager.highlightMarker(stationNumber);
		
		// setDetailViewVisible(true);
		// TODO call DetailFragment.show(station)
	}
	
	private void hideDetails() {
		if(markerManager.detailing){
			//setDetailViewVisible(false);
			markerManager.unhighlightMarker();
		}
	}	
	
	private void centerMap(Station station){
		centering = true;
		map.animateCamera(CameraUpdateFactory.newLatLng(station.getPosition()), 500, null);
	}
	
	
	/*
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
	}*/
	
	private void horribleHackToMoveMyLocationButton() {
	    View mapView = getActivity().findViewById(R.id.map);
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
            map = getMap();
            if (map != null) {
            	markerManager = MarkerManager.getInstance(map, resourceFactory);
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
     

    private boolean isGpsEnabled(){
    	 final LocationManager manager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
    	 return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
    
    
    private void centerMapOnMyLocation(boolean animateCamera){
    	if(!isGpsEnabled()){
    		Helper.showMessage(getActivity(), getString(R.string.msg_go_gps));
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
    			Helper.showMessage(getActivity(), getString(R.string.msg_waiting_gps));
    		}
    	}
    }

	
	//----------------- Interface Implementation ------------------
    
	
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
