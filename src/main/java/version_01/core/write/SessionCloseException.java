package version_01.core.write;

/**
 * Created by mati on 17/09/16.
 *
 * Exception to use when is trying to do something with a closed session
 */
public class SessionCloseException extends Exception {

    public SessionCloseException(String message) {
        super(message);
    }
}
