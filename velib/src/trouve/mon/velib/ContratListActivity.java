package trouve.mon.velib;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

public class ContratListActivity extends ListActivity {

	
	// TODO should check for connectivity
	// TODO should check for lifecycle behavior
	// TODO should check for errors
	// TODO should refactor Runnable code
	
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contract_list);
        loadContract();
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
		Contrat selectedContract = (Contrat) getListView().getItemAtPosition(position);
		if(savePreference(selectedContract.getName())){
			Intent intent = new Intent(this, MapActivity.class);
			startActivity(intent);
		}
	}
	
	private void loadContract(){
		new Thread(new ContractUpdater(this)).start();
	}
	
	public void setContract(List<Contrat> contracts){
		findViewById(R.id.progressBar).setVisibility(View.GONE);
		ListAdapter adapter = new ArrayAdapter<>(this, R.layout.contract_row, R.id.contract_name, contracts);
        setListAdapter(adapter);
	}
	
	
	private boolean savePreference(String contractName){		
		SharedPreferences settings = getSharedPreferences(Contrat.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(Contrat.CONTRACT_PREFERENCE_KEY, contractName);
	    return editor.commit();
	}
}
