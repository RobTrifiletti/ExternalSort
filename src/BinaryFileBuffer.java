import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class BinaryFileBuffer  {
	public static final int BUFFERSIZE = 2048;
	public BufferedReader fbr;
	public File originalfile;
	private String cache;
	private boolean empty;

	public BinaryFileBuffer(File f, Charset charset) throws IOException {
		originalfile = f;
		fbr = new BufferedReader(new InputStreamReader(
				new FileInputStream(f), charset), BUFFERSIZE);
		reload();
	}

	public boolean isEmpty() {
		return empty;
	}

	private void reload() throws IOException {
		try {
			if((this.cache = fbr.readLine()) == null){
				empty = true;
				cache = null;
			}
			else{
				empty = false;
			}
		} catch(EOFException oef) {
			empty = true;
			cache = null;
		}
	}

	public void close() throws IOException {
		fbr.close();
	}


	public String peek() {
		if(isEmpty()) {
			return null;
		}

		else return cache.toString();
	}

	public String pop() throws IOException {
		String answer = peek();
		reload();
		return answer;
	}


}