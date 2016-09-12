package version_mati_01.structure.filters;

import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Created by mati on 11/09/16.
 */
public abstract class ProtocolEncoderFilter<T> {

    public abstract ByteBuffer encode(T message);


}
