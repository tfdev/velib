package trouve.mon.velib.station;

import com.google.android.gms.maps.model.Marker;

public class MarkerWrapper {

	private final Marker marker;
	
	private final int stands;
	private final int bikes;
	private final int number;
	private final Status status;
	
	public MarkerWrapper(Marker marker, Station station) {
		this.marker = marker;
		this.stands = station.getAvailableBikeStands();
		this.bikes = station.getAvailableBikes();
		this.number = station.getNumber();
		this.status = station.getStatus();
	}

	public Marker getMarker() {
		return marker;
	}

	public int getStands() {
		return stands;
	}

	public int getBikes() {
		return bikes;
	}

	public int getNumber() {
		return number;
	}

	public Status getStatus() {
		return status;
	}

	
}
