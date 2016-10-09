package version_01.structure.logic;

import version_01.core.session.IoSession;

/**
 * Created by mati on 02/10/16.
 */
public interface HandlerDispatcher<T>{

    void dispatch(IoSession sessionFrom, T message);

}
