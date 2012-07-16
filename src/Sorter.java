import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Sorter implements Runnable {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String filename = null;
		double mbPerChunk = 0.1; // Default to 10 MB pr. chuck approx
		if (args.length == 1){
			filename = args[0];
		}
		else if( args.length == 2){
			filename = args[0];
			mbPerChunk = Double.parseDouble(args[1]);
		}
		else {
			System.out.println("Must provide arguments");
			return;
		}

		Sorter sort = new Sorter(filename, mbPerChunk);
		sort.run();
	}

	private String filename;
	private double linesPerChunk;
	File outputDir;
	File outputFile;

	public Sorter(String filename, double mbPerChunk){
		//this.filename = filename;
		this.linesPerChunk = mbPerChunk * 40000; //Each line = 26 bytes (Meaning 40000 for 1 MB pr chunk)
		this.filename = "/Volumes/Roberto/Dropbox/Job/Studenterprogrammør/" +
				"ExternalSort/data/aes.txt";
		outputDir = new File("output");
		outputFile = new File("/Volumes/Roberto/Dropbox/Job/Studenterprogrammør/" +
				"ExternalSort/output/sorted.txt");
	}

	public void run() {
		outputDir.mkdir();
		splitFiles();
		mergeFiles();
		cleanup();
	}

	private void cleanup() {
		File[] chunks = outputDir.listFiles();
		for(File f: chunks){
			if (f.getName().equals("sorted.txt")){
				continue;
			}
			f.delete();
		}
		
	}

	private void mergeFiles() {
		try {
			if(outputFile.createNewFile()){

				File[] chunks = outputDir.listFiles();
				List<BufferedReader> brs = new ArrayList<BufferedReader>();
				for(File f: chunks){
					FileReader fr = null;
					try {
						fr = new FileReader(f);
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					}
					brs.add(new BufferedReader(fr));
				}

				try {
					List<String> list = new ArrayList<String>();
					for(BufferedReader br: brs){
						String line;
						int currentLines = 0;
						while((line = br.readLine()) != null){
							if(line.startsWith("2 1 ")){
								list.add(line + "\n");
								currentLines++;
							}
							if (currentLines >= linesPerChunk){
								WireComparator wc = new WireComparator();

								Collections.sort(list, wc);
								FileWriter fstream = new FileWriter(outputFile, true);
					            BufferedWriter fbw = new BufferedWriter(fstream);
								for (String s: list){
									fbw.write(s);
								}
								fbw.close();
								currentLines = 0;
								list.clear();
							}
							if (currentLines >= linesPerChunk /chunks.length){
								continue;
							}
						}
					}
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

private void splitFiles() {
	File file = new File(filename);

	try {
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		List<String> list = new ArrayList<String>();
		String line;
		int currentLines = 0;
		int fileCount = 0;
		while((line = br.readLine()) != null){
			if(line.startsWith("2 1 ")){
				list.add(line + "\n");
				currentLines++;
			}
			if (currentLines >= linesPerChunk){
				WireComparator wc = new WireComparator();

				Collections.sort(list, wc);
				File outFile = 
						new File("/Volumes/Roberto/Dropbox/Job/Studenterprogrammør/" +
								"ExternalSort/output/chunk" + fileCount + ".txt");
				if(outFile.createNewFile()){
					BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
					for (String s: list){
						bw.write(s);
					}
					bw.close();
				}
				fileCount++;
				currentLines = 0;
				list.clear();
			}
		}

		fr.close();
		br.close();

	} catch (FileNotFoundException e) {
		e.printStackTrace();
	} catch (IOException e) {
		e.printStackTrace();
	}
}
}
