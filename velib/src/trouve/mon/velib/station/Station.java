package trouve.mon.velib.station;

import java.sql.Date;
import java.util.Locale;

import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

public class Station {
	
	//----------------- Class Fields ------------------
	
	private static final String TAG = Station.class.getName();

	//----------------- Class methods -----------------
	
	//TODO find something i18n compliant
	static String capitalizeString(String string) {
		char[] chars = string.toLowerCase(Locale.getDefault()).toCharArray();
		boolean found = false;
		for (int i = 0; i < chars.length; i++) {
			if (!found && Character.isLetter(chars[i])) {
				chars[i] = Character.toUpperCase(chars[i]);
				found = true;
			} else if (Character.isWhitespace(chars[i]) || chars[i]=='-' || chars[i]=='\'' ) {
				found = false;
			}
		}
		return String.valueOf(chars);
	}
	
	
	//----------------- Instance Fields ------------------
	
	private int number;
	private String name;
	private String address;
	private LatLng position;
	private boolean banking;
	private boolean bonus;
	private Status status;
	private int bikeStands;
	private int availableBikeStands;
	private int availableBikes;
	private Date lastUpDate;
	
	private Marker marker;
	
	//----------------- Instance Methods ------------------
	
	public Station() {
	}
	
	public int getNumber() {
		return number;
	}
	public void setNumber(int number) {
		this.number = number;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getAddress() {
		return address;
	}
	public void setAddress(String address) {
		this.address = address;
	}
	public LatLng getPosition() {
		return position;
	}
	public void setPosition(LatLng position) {
		this.position = position;
	}
	public boolean isBanking() {
		return banking;
	}
	public void setBanking(boolean banking) {
		this.banking = banking;
	}
	public boolean isBonus() {
		return bonus;
	}
	public void setBonus(boolean bonus) {
		this.bonus = bonus;
	}
	public Status getStatus() {
		return status;
	}
	public void setStatus(String status) {
		if("OPEN".equals(status)){
			this.status = Status.OPEN;
		}else if("CLOSED".equals(status)){
			this.status = Status.CLOSED;
		}else{
			Log.e(TAG, "Unknown status string: "+status);
		}	
	}
	public void setStatus(Status status) {
		this.status = status;
	}
	public int getBikeStands() {
		return bikeStands;
	}
	public void setBikeStands(int bikeStands) {
		this.bikeStands = bikeStands;
	}
	public int getAvailableBikeStands() {
		return availableBikeStands;
	}
	public void setAvailableBikeStands(int availableBikeStands) {
		this.availableBikeStands = availableBikeStands;
	}
	public int getAvailableBikes() {
		return availableBikes;
	}
	public void setAvailableBikes(int availableBikes) {
		this.availableBikes = availableBikes;
	}
	public Date getLastUpDate() {
		return lastUpDate;
	}
	public void setLastUpDate(Date lastUpDate) {
		this.lastUpDate = lastUpDate;
	}
	
	public String getFormattedName() {
		String formattedName = "";
		if(name != null){
			formattedName = name.substring(name.indexOf('-')+2);
		}
		return formattedName;
	}

	public Marker getMarker() {
		return marker;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}
	
	public boolean isDifferent(Station station){
		return (station == null) || (station.getNumber() != number)
								 || (station.getAvailableBikes() != availableBikes)
								 || (station.getAvailableBikeStands() != availableBikeStands);
	}
	
}
