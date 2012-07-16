import java.util.Comparator;


public class BinaryFileBufferComparator implements Comparator<BinaryFileBuffer> {

	private Comparator<String> comparator;

	public BinaryFileBufferComparator(Comparator<String> comparator){
		this.comparator = comparator;

	}
	public int compare(BinaryFileBuffer i, BinaryFileBuffer j) {
		return comparator.compare(i.peek(), j.peek());
	}
}
