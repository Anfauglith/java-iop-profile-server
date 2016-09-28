package version_mati_01.structure.processors.interfaces;

import version_mati_01.core.polling.AbstractProcessor;
import version_mati_01.core.service.IoProcessor;
import version_mati_01.core.session.IoSession;

/**
 * Created by mati on 17/09/16.
 */
public interface ReadManager<S extends IoSession> {

    void read(S session);

    void setProcessor(AbstractProcessor processor);


}
