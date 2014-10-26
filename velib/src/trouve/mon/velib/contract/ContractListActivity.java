package trouve.mon.velib.contract;

import trouve.mon.velib.MainActivity;
import trouve.mon.velib.R;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

public class ContractListActivity extends ActionBarActivity {

	public final static int REQUEST_CODE_USE_EXISTING_MAP = 11;
	public final static int REQUEST_CODE_MOVE_AWAY_IF_EMPTY = 12;
	public final static String EXTRA_CODE = "requestCode";
	
	MenuItem menuItem;
	
	@Override
    protected void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.contract_activity);
		setUpActionBar();
		loadContract();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    getMenuInflater().inflate(R.menu.contract_actions, menu);
	    menuItem = menu.findItem(R.id.action_refresh);
	    return true;
	}
	
	public void showRefreshing(){
		if(menuItem != null){
        	MenuItemCompat.setActionView(menuItem, R.layout.refresh_view);
    	}
	}
	
	public void hideRefreshing(){
		if(menuItem != null){
        	MenuItemCompat.setActionView(menuItem, null);
    	}
	}
	
	private void loadContract(){
		new Thread(new ContractUpdater(this)).start();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

	    if(item.getItemId() == R.id.action_refresh) {
	    	loadContract();
	    	return true;
	    }
	    return super.onOptionsItemSelected(item);
	}
	
	private void setUpActionBar() {
		ActionBar actionBar = getSupportActionBar();
		actionBar.setIcon(R.drawable.ic_launcher);
	    actionBar.setDisplayShowTitleEnabled(true);
	    actionBar.setDisplayShowHomeEnabled(true);
	    actionBar.setDisplayUseLogoEnabled(false);
	}

		
	public void startMainActivity(){	
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
