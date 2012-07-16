import java.util.Comparator;


public class WireComparator implements Comparator<String> {

	private static final String ignoredPrefix = "2 1 ";
	
	public int compare(String arg0, String arg1) {
		if (!arg0.startsWith(ignoredPrefix) || 
				!arg1.startsWith(ignoredPrefix)){
			return 0;
		}
		else{
			String[] split0 = arg0.split(" ");
			String[] split1 = arg1.split(" ");
			int i0 = Integer.parseInt(split0[4]);
			int i1 = Integer.parseInt(split1[4]);

			if (i0 > i1){
				return 1;
			}
			if (i0 < i1){
				return -1;
			}
			else return 0;
		}
	}

}
