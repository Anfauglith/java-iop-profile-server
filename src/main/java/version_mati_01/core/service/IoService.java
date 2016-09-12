package version_mati_01.core.service;

import version_mati_01.core.filter.IoFilterChainBuilder;
import version_mati_01.core.session.IoSession;
import version_mati_01.structure.ClientSession;

/**
 * Created by mati on 09/09/16.
 */
public interface IoService {

    IoProcessor getIoProcessor();


    IoFilterChainBuilder getFilterChainBuilder();
}
