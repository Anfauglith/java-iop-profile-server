package version_01.core.filter.protocol;

/**
 * Created by mati on 10/10/16.
 */
public class InvalidProtocolViolation extends Exception{

    public InvalidProtocolViolation(String message, Throwable cause) {
        super(message, cause);
    }
}
