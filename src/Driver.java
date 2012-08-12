import java.io.File;
import java.nio.charset.Charset;
import java.util.Comparator;


public class Driver {
	
	public static void main(String[] args) {
		
		int maxtmpfiles = 0;
		boolean verbose = false;
		Charset charset = Charset.defaultCharset();
		String inputfileName = null;
		String outputfileName = null;
		for (int param = 0; param < args.length; ++param) {
			if (args[param].equals("-v") ||  args[param].equals("--verbose")) {
				verbose = true;
			}	
			else if ((args[param].equals("-t") || args[param].equals("--maxtmpfiles"))
					&& args.length > param + 1) {
				param++;
				maxtmpfiles = Integer.parseInt(args[param]);  
			}
			else if ((args[param].equals("-c") || args[param].equals("--charset"))
					&& args.length > param + 1) {
				param++;
				charset = Charset.forName(args[param]);
			}
			else {	
				if (inputfileName == null) {
					inputfileName = args[param];
				}

				else if (outputfileName == null) {
					outputfileName = args[param];
				}

				else System.out.println("Unparsed: " + args[param]); 
			}
		}

		if(outputfileName == null) {
			outputfileName = "data/out.txt";
		}
		
		File inputFile = new File(inputfileName);
		File outputFile = new File(outputfileName);

		if (!inputFile.exists()){
			System.out.println("Inputfile: " + inputFile.getName() + " not found");
			return;
		}

		Comparator<String> wireComparator = new WireComparator();
		
		ExternalSort sorter = new ExternalSort(verbose, maxtmpfiles, charset,
				inputFile, outputFile, wireComparator);
		sorter.run();
	}
}
