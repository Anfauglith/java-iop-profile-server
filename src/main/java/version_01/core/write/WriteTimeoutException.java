package version_01.core.write;

/**
 * Created by mati on 10/10/16.
 */
public class WriteTimeoutException extends Exception {

    public WriteTimeoutException() {
    }

    public WriteTimeoutException(String message) {
        super(message);
    }
}
