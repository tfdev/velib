package trouve.mon.velib;

import trouve.mon.velib.util.Helper;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class AboutActivity extends Activity {

	private Button mailButton;
	private Button shareButton;
	private Button rateButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Helper.setUpActionBarCustomTheme(this);
		setContentView(R.layout.about);
		setUpView();
	}
	
	
	private void setUpView(){
		mailButton = (Button) findViewById(R.id.mail);
		shareButton = (Button) findViewById(R.id.share);
		rateButton = (Button) findViewById(R.id.rate);
		
		mailButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				
			}
		});
		shareButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				
			}
		});
		rateButton.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				
			}
		});
	}
}
