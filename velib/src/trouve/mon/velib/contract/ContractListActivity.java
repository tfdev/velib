package trouve.mon.velib.contract;

import java.util.List;

import trouve.mon.velib.MainActivity;
import trouve.mon.velib.R;
import trouve.mon.velib.util.Helper;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ContractListActivity extends ListActivity {

	public final static int REQUEST_CODE_USE_EXISTING_MAP = 11;
	public final static int REQUEST_CODE_MOVE_AWAY_IF_EMPTY = 12;
	public final static String EXTRA_CODE = "requestCode";
	
	
	private Contract selectedContract;

	private Button okButton;
	private TextView errorTextView ;
	private ProgressBar progressBar;
	private View refreshButton;
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		Helper.setUpActionBarCustomTheme(this);
		setContentView(R.layout.contract_list);
		setUpOkButton();
		setUpRefreshButton();
		loadContract();
		errorTextView = (TextView) findViewById(R.id.msgTextView);
		progressBar = (ProgressBar) findViewById(R.id.progressBar);
	}
	
	private void setUpRefreshButton() {
		refreshButton = (View) findViewById(R.id.btn_refresh);
		refreshButton.setVisibility(View.INVISIBLE);
		refreshButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				loadContract();
			}
		});
	}
	
	private void setUpOkButton() {
		okButton = (Button) findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				savePreference(selectedContract.getName(), selectedContract.getServiceName());
				startMapActivity();
			}
		});
	}
	
	@Override
	protected void onListItemClick (ListView l, View v, int position, long id){
		selectedContract = (Contract) getListView().getItemAtPosition(position);
		okButton.setEnabled(true);
	}
	
	public void showError(int stringResourceId){
		refreshButton.setVisibility(View.VISIBLE);
		
		progressBar.setVisibility(View.GONE);
		errorTextView.setVisibility(View.VISIBLE);
		errorTextView.setText(getString(stringResourceId));
	}
	
	private void loadContract(){
		//TODO asynctask
		new Thread(new ContractUpdater(this)).start();
	}
	
	public void showLoading() {
		progressBar.setVisibility(View.VISIBLE);
		errorTextView.setVisibility(View.INVISIBLE);
		refreshButton.setVisibility(View.INVISIBLE);
	}
	
	public void setContract(List<Contract> contracts){
		progressBar.setVisibility(View.GONE);
		errorTextView.setVisibility(View.INVISIBLE);
		ListAdapter adapter = new ContractAdapter(this, R.layout.contract_row, contracts);
        setListAdapter(adapter);
	}
	
	private boolean savePreference(String contractName, String serviceName){		
		SharedPreferences settings = getSharedPreferences(Contract.CONTRACT_PREFERENCE_KEY, MODE_PRIVATE);
	    SharedPreferences.Editor editor = settings.edit();
	    editor.putString(Contract.CONTRACT_PREFERENCE_KEY, contractName);
	    editor.putString(Contract.SERVICE_PREFERENCE_KEY, serviceName);
	    return editor.commit();
	}
	
	private void startMapActivity(){	
		Bundle bundle = getIntent().getExtras();
		if( bundle == null || bundle.getInt(EXTRA_CODE) != REQUEST_CODE_USE_EXISTING_MAP){
			Intent intent = new Intent(this, MainActivity.class);
			intent.putExtra(ContractListActivity.EXTRA_CODE, ContractListActivity.REQUEST_CODE_MOVE_AWAY_IF_EMPTY);
			startActivity(intent);
		}else{
			Intent resultIntent = new Intent();
			setResult(RESULT_OK, resultIntent);
		}
		finish();
	}
}
