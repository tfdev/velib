package trouve.mon.velib.contract;

import java.util.List;

import trouve.mon.velib.R;
import trouve.mon.velib.R.id;
import trouve.mon.velib.R.layout;
import trouve.mon.velib.station.MapActivity;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ContractListActivity extends ListActivity {

	
	// TODO should check for connectivity
	// TODO should check for lifecycle behavior
	// TODO should check for errors
	// TODO should refactor Runnable code
	
	private Contract selectedContract;
	private Button okButton;
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contract_list);
		setUpOkButton();
		loadContract();
	}
	
	private void setUpOkButton() {
		okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				savePreference(selectedContract.getName());
				startMapActivity();
			}
		});
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
		selectedContract = (Contract) getListView().getItemAtPosition(position);
		okButton.setEnabled(true);
	}
	
	private void loadContract(){
		new Thread(new ContractUpdater(this)).start();
	}
	
	public void setContract(List<Contract> contracts){
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		ListAdapter adapter = new ArrayAdapter<>(this, R.layout.contract_row, R.id.contract_name, contracts);
        setListAdapter(adapter);
	}
	
	private boolean savePreference(String contractName){		
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(Contract.CONTRACT_PREFERENCE_KEY, contractName);
	    return editor.commit();
	}
	
	private String getPreferredContract(){
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    return settings.getString(Contract.CONTRACT_PREFERENCE_KEY, null);
	}
	
	private void startMapActivity(){
		Intent intent = new Intent(this, MapActivity.class);
		startActivity(intent);
		finish();
	}
}
