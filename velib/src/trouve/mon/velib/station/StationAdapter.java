package trouve.mon.velib.station;

import java.util.List;

import trouve.mon.velib.R;
import trouve.mon.velib.util.Helper;
import trouve.mon.velib.util.LocationClientSingleton;
import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;



public class StationAdapter extends ArrayAdapter<Station> {

	Context context;
	int resource;
	List<Station> stations;
	
	public StationAdapter(Context context, int resource, List<Station> objects) {
		super(context, resource, objects);
		this.context = context;
		this.resource = resource;
		this.stations = objects;
	}

	@Override
	public View getView (int position, View convertView, ViewGroup parent){
		StationHolder holder = null;
		View row = convertView;
		Station station = stations.get(position);
		
		if(row == null){
			LayoutInflater inflater = ((Activity)context).getLayoutInflater();
            row = inflater.inflate(resource, parent, false);
            holder = new StationHolder();
            holder.name = (TextView) row.findViewById(R.id.station_info);
            holder.distance = (TextView) row.findViewById(R.id.distance);
            holder.bikes = (TextView) row.findViewById(R.id.bike_number);
            holder.stands = (TextView) row.findViewById(R.id.parking_number);
            holder.star = (ImageButton) row.findViewById(R.id.favorite);
            holder.star.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View v) {
					Object tag = v.getTag();
					if(tag != null && tag instanceof Integer){
						Integer number = (Integer) tag;
						Station station = StationManager.INSTANCE.get(number);
						setFavorite(station, !station.isFavorite());
						setFavImageOnDetailView(station, v);
					}
				}
			});
            int color = row.getResources().getColor(R.color.logo_blue);
            holder.bikes.setTextColor(color);
            holder.stands.setTextColor(color);
            ((ImageView) row.findViewById(R.id.bike)).setColorFilter(color);
    		((ImageView) row.findViewById(R.id.parking)).setColorFilter(color);

            row.setTag(holder);
		}else{
			holder = (StationHolder) row.getTag();
		}
		
		if(station != null){
			holder.name.setText(station.toString());
			holder.bikes.setText(String.valueOf(station.getAvailableBikes()));
			holder.stands.setText(String.valueOf(station.getAvailableBikeStands()));
			int distance = LocationClientSingleton.distanceFromLastLocation(station);
			holder.distance.setText(Helper.formatDistance(distance));
			holder.star.setTag(Integer.valueOf(station.getNumber()));
			setFavImageOnDetailView(station, holder.star);
		}
		
		return row;
	}
	
	
	private void setFavorite(Station station, boolean newValue){
		StationManager.INSTANCE.setFavorite(station, newValue);
		MarkerManager markerManager = MarkerManager.getInstance(null, null);
		if(markerManager != null){
			markerManager.updateMarker(station);
		}
	}
	
	private void setFavImageOnDetailView(Station station, View v){
		if(v instanceof ImageButton){
			ImageButton imageButton = (ImageButton) v;
			if(station.isFavorite())
				imageButton.setImageResource(R.drawable.ic_fav);
			else
				imageButton.setImageResource(R.drawable.ic_action_not_important);
		}
	}
	
	
	static class StationHolder{
		TextView name;
		TextView bikes;
		TextView stands;
		TextView distance;
		ImageButton star;
	}
}
