package trouve.mon.velib.contract;

public class Contract implements Comparable<Contract>{
	
	enum Country{
		BE,
		ES,
		FR
	}
	
	public static Contract[] contracts = new Contract[]{
		
		new Contract("Lyon", 		Country.FR, "Vélo'V"),
		new Contract("Marseille", 	Country.FR, "Le vélo"),
		new Contract("Paris", 		Country.FR, "Velib"),
		new Contract("Toulouse", 	Country.FR, "Vélô")
	};
	
	
	final static public String CONTRACT_PREFERENCE_KEY = "contract";
	
	
	private String name;
	private Country country;
	private String serviceName;
		
	public Contract() {
		super();
	}
	
	public Contract(String name, Country country, String serviceName) {
		super();
		this.setName(name);
		this.setCountry(country);
		this.setServiceName(serviceName);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Country getCountry() {
		return country;
	}

	public void setCountry(Country country) {
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
