package trouve.mon.velib.station;

import trouve.mon.velib.R;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;

public class FavoriteListFragment extends ListFragment {

	public static final String FAVORITE_FRAGMENT_TAG = "FAVORITES";
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return inflater.inflate(R.layout.fav_list, container, false);
	}
	
	@Override 
	public void onCreate(Bundle savedInstanceState) { 
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onResume(){
		super.onResume();
		refresh();
	}

	public void refresh(){
		ListAdapter adapter = new StationAdapter(getActivity(), R.layout.station_row, StationManager.INSTANCE.getFavorites());
        setListAdapter(adapter);
	}
	
}
