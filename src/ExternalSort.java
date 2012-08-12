import java.util.*;
import java.io.*;
import java.nio.charset.Charset;

/**
 * Goal: offer a generic external-memory sorting program in Java.
 * 
 * It must be : 
 *  - hackable (easy to adapt)
 *  - scalable to large files
 *  - sensibly efficient.
 *
 * This software is in the public domain.
 *
 * Usage: 
 *  java ExternalSort somefile.txt out.txt
 * 
 * You can change the default maximal number of temporary files with the -t flag:
 *  java ExternalSort somefile.txt out.txt -t 3
 *
 * For very large files, you might want to use an appropriate flag to allocate
 * more memory to the Java VM: 
 *  java -Xms2G ExternalSort somefile.txt out.txt
 *
 * By (in alphabetical order) 
 *  Philippe Beaudoin,  Jon Elsas,  Christan Grant, Daniel Haran, Daniel Lemire, 
 *  April 2010
 * 	originally posted at 
 *  http://www.daniel-lemire.com/blog/archives/2010/04/01/external-memory-sorting-in-java/
 *	
 *	****************************************************************************
 *	All credit for initial code goes to above authors, this code has been refactored
 *	and slightly adjusted by Roberto Trifiletti, both for clarity in code and
 *  better hackability.
 *	
 */
public class ExternalSort implements Runnable {
	/**
	 * We multiply by two because later on someone insisted on counting the memory
	 * usage as 2 bytes per character. By this model, loading a file with 1 character
	 * will use 2 bytes.
	 */
	private static final int bytesPerChar = 2;
	private static final int DEFAULTMAXTEMPFILES = 1024;

	private int maxtmpfiles;
	private boolean verbose;
	private Charset charset;
	private File inputFile, outputFile;
	private Comparator<String> comparator;
	
	public ExternalSort(boolean verbose, int maxtmpfiles, Charset charset,
			File inputFile, File outputFile, Comparator<String> comparator){
		this.verbose = verbose;
		if (maxtmpfiles == 0) {
			this.maxtmpfiles = DEFAULTMAXTEMPFILES;
		}
		else {
			this.maxtmpfiles = maxtmpfiles;
		}
		
		this.charset = charset;
		this.inputFile = inputFile;
		this.outputFile = outputFile;
		this.comparator = comparator;
	}
	
	public void run() {
		List<File> l;
		try {
			l = sortInBatch(inputFile,
					comparator, maxtmpfiles, charset, null);
			if(verbose) {
				System.out.println("created "+ l.size() + " tmp files");
			}

			mergeSortedFiles(l, outputFile, comparator, charset);

		} catch (IOException e) {
			System.out.println("An IO error occurred while sorting the file");
		}
	}
	
	/**
	 * This will simply load the file by blocks of x rows, then
	 * sort them in-memory, and write the result to 
	 * temporary files that have to be merged later. You can
	 * specify a bound on the number of temporary files that
	 * will be created.
	 * 
	 * @param file some flat  file
	 * @param comparator string comparator 
	 * @param maxtmpfiles maximal number of temporary files
	 * @param charset character set to use  (can use Charset.defaultCharset()) 
	 * @param tmpdirectory location of the temporary files (set to null for default location)
	 * @return a list of temporary flat files
	 */
	public List<File> sortInBatch(File inputfile, Comparator<String> comparator,
			int maxtmpfiles, Charset charset, File tmpdirectory) 
					throws IOException {
		List<File> files = new ArrayList<File>();
		BufferedReader fbr = new BufferedReader(new InputStreamReader(
				new FileInputStream(inputfile), charset));

		// in bytes
		long blocksize = estimateBestSizeOfBlocks(inputfile, maxtmpfiles); 

		try{
			List<String> tmplist =  new ArrayList<String>();
			String line = "";
			try {
				long currentblocksize = 0;// in bytes

				// as long as enough memory
				while((currentblocksize < blocksize) 
						&&((line = fbr.readLine()) != null)) { 
					tmplist.add(line);
					currentblocksize += line.length() * bytesPerChar;
				}
				files.add(sortAndSave(tmplist, comparator, charset, 
						tmpdirectory));
				tmplist.clear();
			} catch(EOFException oef) {
				if(tmplist.size() > 0) {
					files.add(sortAndSave(tmplist, comparator, charset, 
							tmpdirectory));
					tmplist.clear();
				}
			}
		} finally {
			fbr.close();
		}

		return files;
	}
	
	// we divide the file into small blocks. If the blocks
	// are too small, we shall create too many temporary files. 
	// If they are too big, we shall be using too much memory. 
	public long estimateBestSizeOfBlocks(File inputfile, int maxtmpfiles) {
		long filesize = inputfile.length() * bytesPerChar;

		// we don't want to open up much more than maxtmpfiles temporary files, better run
		// out of memory first.
		long blocksize = filesize / maxtmpfiles + 
				(filesize % maxtmpfiles == 0 ? 0 : 1) ;

		// on the other hand, we don't want to create many temporary files
		// for naught. If blocksize is smaller than half the free memory, grow it.
		long freemem = Runtime.getRuntime().freeMemory();

		if( blocksize < freemem/2) {
			blocksize = freemem/2;
		} 
		return blocksize;
	}
	
	/**
	 * Sort a list and save it to a temporary file 
	 *
	 * @return the file containing the sorted data
	 * @param tmplist data to be sorted
	 * @param comparator string comparator
	 * @param charset charset to use for output (can use Charset.defaultCharset())
	 * @param tmpdirectory location of the temporary files (set to null for default location)
	 */ 
	public File sortAndSave(List<String> tmplist, Comparator<String> comparator,
			Charset charset, File tmpdirectory) throws IOException {

		Collections.sort(tmplist, comparator);  
		File newtmpfile = File.createTempFile("sortInBatch", "flatfile", tmpdirectory);
		newtmpfile.deleteOnExit();

		BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(newtmpfile), charset));
		try {
			for(String s : tmplist) {
				fbw.write(s);
				fbw.newLine();
			}
		} finally {
			fbw.close();
		}

		return newtmpfile;
	}
	
	/**
	 * This merges a bunch of temporary flat files 
	 * @param files
	 * @param output file
	 * @param Charset character set to use to load the strings
	 * @return The number of lines sorted. (P. Beaudoin)
	 */
	public int mergeSortedFiles(List<File> files, File outputfile,
			final Comparator<String> comparator, Charset charset)
					throws IOException {

		PriorityQueue<BinaryFileBuffer> priorityQueue =
				new PriorityQueue<BinaryFileBuffer>(11,
						new BinaryFileBufferComparator(comparator));

		for (File f : files) {
			BinaryFileBuffer bfb = new BinaryFileBuffer(f, charset);
			priorityQueue.add(bfb);
		}

		BufferedWriter fbw = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputfile), charset));

		int rowcounter = 0;
		try {
			while(priorityQueue.size() > 0) {
				BinaryFileBuffer bfb = priorityQueue.poll();
				String s = bfb.pop();

				fbw.write(s);
				fbw.newLine();
				++rowcounter;

				if(bfb.isEmpty()) {
					bfb.fbr.close();
					bfb.originalfile.delete();// we don't need you anymore
				} else {
					priorityQueue.add(bfb); // add it back
				}
			}
		} finally { 
			fbw.close();
			for(BinaryFileBuffer bfb : priorityQueue ) bfb.close();
		}

		return rowcounter;
	}
}