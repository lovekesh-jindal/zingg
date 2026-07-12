package zingg.common.client;

/**
 * Base class for all Zingg Exceptions
 * 
 * @author sgoyal
 *
 */
// DESIGN ISSUE : extends Exception instead of throwable
// REASON : as a direct throwable subclass it was a sibling of exception so catch(exception e) couldn't catch it (it behaved like an Error)
// but if an Error ( like out of memory , stack overflow ) came it will still flow and  ZinggClientException not able to catch that 
// now as a exception subclass it is now caught by an ordinary exception , while Error still propagate
public class ZinggClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public ZinggClientException(String m) {
		super(m);
	}

	public ZinggClientException(String m, Throwable cause) {
		super(m, cause);
	}

	public ZinggClientException(Throwable cause) {
		super(cause);
	}

}
