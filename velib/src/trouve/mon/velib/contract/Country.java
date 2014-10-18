package trouve.mon.velib.contract;

import trouve.mon.velib.R;

public enum Country {
	BE,
	ES,
	FR,
	IE,
	JP,
	LT,
	LU,
	NO,
	RU,
	SE,
	SI;
	
	static public int getDrawableResourceId(String countryCode){
		int result = -1;
		try{
			Country country = Country.valueOf(countryCode);
			switch(country){
			case BE: result = R.drawable.be;
				break;
			case ES: result = R.drawable.es;
				break;
			case FR: result = R.drawable.fr;
				break;
			case IE: result = R.drawable.ie;
				break;
			case JP: result = R.drawable.jp;
				break;
			case LT: result = R.drawable.lt;
				break;
			case LU: result = R.drawable.lu;
				break;
			case NO: result = R.drawable.no;
				break;
			case RU: result = R.drawable.ru;
				break;
			case SE: result = R.drawable.se;
				break;
			case SI: result = R.drawable.si;
				break;
			};
		}catch(Exception e){
			
		}
		return result;
	}
}
