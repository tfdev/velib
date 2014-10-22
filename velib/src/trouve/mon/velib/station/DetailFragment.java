package trouve.mon.velib.station;


import trouve.mon.velib.R;
import trouve.mon.velib.ResourceFactory;
import trouve.mon.velib.util.Formatter;
import trouve.mon.velib.util.Helper;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.Marker;

public class DetailFragment extends Fragment {

	//----------------- Static Fields ------------------
	
	public static final String DETAIL_FRAGMENT_TAG = "DETAIL";
	
	//-----------------  Instance Fields ------------------
	
	private View rootView;
	
	private View detailContainerView;
	private TextView distanceTextView;
	private TextView bikeTextView;
	private TextView standTextView;
	private TextView stationTextView;
	private ImageView bikeImageView;
	private ImageView standImageView;
	private ImageButton favImageView;
	
	private MarkerManager markerManager;

	
	
	//-----------------  Fragment Lifecycle ------------------
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.station_row, container, false);
		setUpStationDetailView();
		return rootView;
	}
	
	@Override
    public void onResume() {
        super.onResume();

    }

	@Override
    public void onPause() {
        super.onPause();

    }
	
	//-----------------  public methods ------------------
	
	public void refresh(){
		if(getMarkerManager().detailing){
			updateDetailInfo(StationManager.INSTANCE.get(getMarkerManager().detailedStationNumber));
		}
	}
	
	public void show(Marker marker){
		int stationNumber = Integer.parseInt(marker.getTitle());
		Station station = StationManager.INSTANCE.get(stationNumber);
		setFavImageOnDetailView(station);
		updateDetailInfo(station);
		setDetailViewVisible(true);
	}
	
	public void hide(){
		setDetailViewVisible(false);
	}

	
	//-----------------  private methods ------------------
	

	private MarkerManager getMarkerManager(){
		if(markerManager == null){
			Fragment fragment = getFragmentManager().findFragmentByTag(GoogleMapFragment.MAP_FRAGMENT_TAG);
			if(fragment != null){
				MapFragment mapFragment = (MapFragment) fragment;
				markerManager = MarkerManager.getInstance(mapFragment.getMap(), ResourceFactory.getInstance(getResources()));
			}
		}
		return markerManager;
	}
	
	private void setUpStationDetailView(){
		
		detailContainerView = rootView.findViewById(R.id.detail_layout);
		detailContainerView.setVisibility(View.INVISIBLE);
		detailContainerView.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {}
		});
		
		distanceTextView = (TextView) rootView.findViewById(R.id.distance);
		bikeTextView = (TextView) rootView.findViewById(R.id.bike_number);
		standTextView = (TextView) rootView.findViewById(R.id.parking_number);
		stationTextView = (TextView) rootView.findViewById(R.id.station_info);
		bikeImageView = (ImageView) rootView.findViewById(R.id.bike);
		standImageView = (ImageView) rootView.findViewById(R.id.parking);
		favImageView = (ImageButton) rootView.findViewById(R.id.favorite);
		favImageView.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Station station = StationManager.INSTANCE.get(getMarkerManager().detailedStationNumber);
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
	
	private void setFavorite(Station station, boolean isFavorite){
		station.setFavorite(isFavorite);
		getMarkerManager().updateMarker(station);
		SharedPreferences.Editor editor = Helper.getFavoriteSharedPreferences(getActivity()).edit();
		if(isFavorite){
			editor.putBoolean(String.valueOf(station.getNumber()), true);
		}else{
			editor.remove(String.valueOf(station.getNumber()));
		}
		editor.apply();
	}
	
	private void setFavImageOnDetailView(Station station){
		if(station.isFavorite())
			favImageView.setImageResource(R.drawable.ic_fav);
		else
			favImageView.setImageResource(R.drawable.ic_action_not_important);			
	}
	
	private void displayDistanceDetail(Station station){
		int distance = LocationClientDelegate.distanceFromLastLocation(station);
		distanceTextView.setText(Formatter.formatDistance(distance, getActivity()));
	}
	
	private void updateDetailInfo(Station station) {
		int bikes = station.getAvailableBikes();
		int stands = station.getAvailableBikeStands();
		
		bikeTextView.setText(String.valueOf(bikes));
		standTextView.setText(String.valueOf(stands));
		
		displayDistanceDetail(station);
		
		stationTextView.setText(station.toString());
	}

	
	private void slideToTop(View view) {
		if (view.getVisibility() != View.VISIBLE) {
			TranslateAnimation animate = new TranslateAnimation(0, 0, view.getHeight(), 0);
			animate.setDuration(500);
			view.startAnimation(animate);
			view.setVisibility(View.VISIBLE);
		}
	}

	private void slideToBottom(View view) {
		TranslateAnimation animate = new TranslateAnimation(0, 0, 0, view.getHeight());
		animate.setDuration(500);
		view.startAnimation(animate);
		view.setVisibility(View.GONE);
	}
	
	private void setDetailViewVisible(boolean visible){
		//moveMyLocationButton(visible);
		if(visible){
			slideToTop(detailContainerView);
		}else{
			slideToBottom(detailContainerView);
		}
	}
	
	//-----------------  Interface implementation ------------------

	
}
