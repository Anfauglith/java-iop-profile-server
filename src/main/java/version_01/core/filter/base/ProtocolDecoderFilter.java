package version_01.core.filter.base;

import java.nio.ByteBuffer;

/**
 * Created by mati on 11/09/16.
 */
public abstract class ProtocolDecoderFilter<T> {

    public abstract T decode(ByteBuffer byteBuffer);


}