package version_01.core.filter.protocol;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public abstract class ProtocolEncoderFilter<T> {

    public abstract ByteBuffer encode(T message);


}
