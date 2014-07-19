package trouve.mon.velib.contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.util.JsonReader;

/*
 [
 	{
 		"name":"Rouen",
 		"cities":["Rouen"],
 		"commercial_name":"cy'clic",
 		"country_code":"FR"
 	}
...
]

 */


public abstract class ContractParser {
	
	//----------------- Static Fields ------------------
	
	//private static final String TAG = ContratParser.class.getName();

	public static String ATTRIBUTE_SERVICE_NAME 		= "commercial_name";
	public static String ATTRIBUTE_NAME 				= "name";
	public static String ATTRIBUTE_COUNTRY				= "country_code";

	
	//----------------- Static Methods ------------------
	
	public static List<Contract> parse(InputStream inputStream) throws IOException {
		List<Contract> contracts = null;
		JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
	    try {
	    	contracts = readContractArray(reader);
	    }
	    finally {
	    	reader.close();
	    }
	    Collections.sort(contracts);
	    return contracts;
	}
	
	private static List<Contract> readContractArray(JsonReader reader) throws IOException {
		List<Contract> contracts = new ArrayList<Contract>();
		reader.beginArray();
	    while (reader.hasNext()) {
	    	contracts.add(readContract(reader));
	    }
	    reader.endArray();
	    return contracts;
	}


	private static Contract readContract(JsonReader reader) throws IOException {
		Contract contract = new Contract();

	    reader.beginObject();
	    while (reader.hasNext()) {
	    	String attribute = reader.nextName();
	    	if(ATTRIBUTE_COUNTRY.equals(attribute)){
	    		contract.setCountry(reader.nextString());
	    	}
	    	else if(ATTRIBUTE_NAME.equals(attribute)){
	    		contract.setName(reader.nextString());
	    	}
	    	else if(ATTRIBUTE_SERVICE_NAME.equals(attribute)){
	    		contract.setServiceName(reader.nextString());
	    	}	
			else {
				reader.skipValue();
		    }
	     }
	     reader.endObject();
	     return contract;
	}

	
}
