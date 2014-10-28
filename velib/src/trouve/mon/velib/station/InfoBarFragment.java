package trouve.mon.velib.station;


import trouve.mon.velib.R;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class InfoBarFragment extends Fragment {

	//----------------- Static Fields ------------------
	
	public static final String FRAGMENT_TAG = "INFOBAR";
	
	//-----------------  Instance Fields ------------------
	
	private View rootView;

	private TextView messageView;

	
	
	//-----------------  Fragment Lifecycle ------------------
	
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		rootView = inflater.inflate(R.layout.infobar, container, false);
		setUp();
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
	
	public void show(String msg){
		messageView.setText(msg);
		rootView.setVisibility(View.VISIBLE);
	}
	
	public void hide(){
		rootView.setVisibility(View.GONE);
	}

	
	//-----------------  private methods ------------------
	
	
	private void setUp(){
		messageView = (TextView) rootView.findViewById(R.id.message);
		rootView.setVisibility(View.INVISIBLE);
	}
	
	
	//-----------------  Interface implementation ------------------

	
}
