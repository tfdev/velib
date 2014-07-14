package trouve.mon.velib;

public class Contrat implements Comparable<Contrat>{
	
	enum Country{
		BE,
		ES,
		FR
	}
	
	public static Contrat[] contrats = new Contrat[]{
		
		new Contrat("Lyon", 		Country.FR, "Vélo'V"),
		new Contrat("Marseille", 	Country.FR, "Le vélo"),
		new Contrat("Paris", 		Country.FR, "Velib"),
		new Contrat("Toulouse", 	Country.FR, "Vélô")
	};
	
	
	final static public String CONTRACT_PREFERENCE_KEY = "contract";
	
	
	private String name;
	private Country country;
	private String serviceName;
		
	public Contrat() {
		super();
	}
	
	public Contrat(String name, Country country, String serviceName) {
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
	public int compareTo(Contrat another) {
		return getName().compareTo(another.getName());
	}
	
}
