package version_01.core.session;

/**
 *
 * Represents the type of idleness of {@link IoSession}
 * <ul>
 *   <li>{@link #READER_IDLE} - No data is coming from the remote peer.</li>
 *   <li>{@link #WRITER_IDLE} - Session is not writing any data.</li>
 *   <li>{@link #BOTH_IDLE} - Both {@link #READER_IDLE} and {@link #WRITER_IDLE}.</li>
 * </ul>
 * <p>
 * Created by mati on 10/10/16.
 */
public enum  IdleStatus {

    READER_IDLE,
    WRITER_IDLE,
    BOTH_IDLE

}
