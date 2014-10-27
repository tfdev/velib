package trouve.mon.velib.contract;

import java.util.List;

import trouve.mon.velib.R;
import trouve.mon.velib.util.MyPreferenceManager;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;

public class ContractListFragment extends ListFragment {
	
	
	private Contract selectedContract;

	private Button okButton;
	private ProgressBar progressBar;
	
	
	@Override
	public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
		return inflater.inflate(R.layout.contract_list, container, false);
	}
	
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState){
		super.onViewCreated(view, savedInstanceState);
		setUpOkButton(view);
		progressBar = (ProgressBar) view.findViewById(R.id.progressBar);
	}
	
	private void setUpOkButton(View view) {
		okButton = (Button) view.findViewById(R.id.okButton);
        okButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				MyPreferenceManager.savePreferredService(selectedContract.getName(), selectedContract.getServiceName());
				((ContractListActivity)getActivity()).startMainActivity();
			}
		});
	}
	
	@Override
	public void onListItemClick (ListView l, View v, int position, long id){
		selectedContract = (Contract) getListView().getItemAtPosition(position);
		okButton.setEnabled(true);
		((ContractAdapter) l.getAdapter()).selectItem(position);
		l.invalidateViews();
	}
	
	public void showError(int stringResourceId){
		progressBar.setVisibility(View.GONE);
	}
	
	public void showLoading() {
		progressBar.setVisibility(View.VISIBLE);
	}
	
	public void setContract(List<Contract> contracts){
		progressBar.setVisibility(View.GONE);
		ListAdapter adapter = new ContractAdapter(getActivity(), R.layout.contract_row, contracts);
        setListAdapter(adapter);
	}
	
}
