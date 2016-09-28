package version_mati_01.structure.processors.interfaces;

import version_mati_01.core.polling.AbstractProcessor;
import version_mati_01.core.session.IoSession;

/**
 * Created by mati on 17/09/16.
 */
public interface WriteManager<S extends IoSession> {

    boolean flushNow(S session,long currentTime);

    void setProcessor(AbstractProcessor processor);
}
