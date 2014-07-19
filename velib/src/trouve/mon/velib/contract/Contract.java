package trouve.mon.velib.contract;

public class Contract implements Comparable<Contract>{
	
		
	final static public String CONTRACT_PREFERENCE_KEY = "contract";
	final static public String SERVICE_PREFERENCE_KEY = "service";
	
	private String name;
	private String country;
	private String serviceName;
		
	public Contract() {
		super();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getServiceName() {
		return serviceName;
	}

	public void setServiceName(String serviceName) {
		this.serviceName = serviceName;
	}
	
	public String toString(){
		return getName();
	}

	@Override
	public int compareTo(Contract another) {
		return getName().compareTo(another.getName());
	}
	
}
