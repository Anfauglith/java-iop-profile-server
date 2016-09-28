package version_01.core.session;

/**
 * The session state. A session can be in three different state :
 * <ul>
 *   <li>OPENING : The session has not been fully created</li>
 *   <li>OPENED : The session is opened</li>
 *   <li>CLOSING :  The session is closing</li>
 * </ul>
 * 
 * @author <a href="http://mina.apache.org">Apache MINA Project</a>
 */
public enum SessionState {
    OPENING, OPENED, CLOSING
}