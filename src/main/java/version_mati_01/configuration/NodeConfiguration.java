package version_mati_01.configuration;

/**
 * Created by mati on 09/09/16.
 */
public interface NodeConfiguration {

    public int getPort();

    public boolean isReuseAddress();

    public int getBacklog();
}
