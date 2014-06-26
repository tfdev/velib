package trouve.mon.velib;

import java.sql.Date;

import com.google.android.gms.maps.model.LatLng;

public class Station {

	private int number;
	private String name;
	private String address;
	private LatLng position;
	private boolean banking;
	private boolean bonus;
	private String status; // TODO make an enum
	private int bikeStands;
	private int availableBikeStands;
	private int availableBikes;
	private Date lastUpDate;
	
	
	public Station() {
		super();
		// TODO Auto-generated constructor stub
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
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
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
	
}
