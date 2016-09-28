package version_mati_01.core.filter.base;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public abstract class ProtocolEncoderFilter<T> {

    public abstract ByteBuffer encode(T message);


}
