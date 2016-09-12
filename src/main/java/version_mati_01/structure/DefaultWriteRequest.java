package version_mati_01.structure;

import version_mati_01.core.write.WriteFuture;
import version_mati_01.core.write.WriteRequest;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultWriteRequest implements WriteRequest {

    private final Object message;

    private Object bufferMessageFiltered;

    public DefaultWriteRequest(Object message) {
        this.message = message;
    }

    @Override
    public WriteFuture getFuture() {
        return null;
    }

    @Override
    public Object getMessage() {
        return message;
    }

    @Override
    public Object getMessageFiltered() {
        return bufferMessageFiltered;
    }

    @Override
    public SocketAddress getDestination() {
        return null;
    }

    @Override
    public boolean isEncoded() {
        return false;
    }

    @Override
    public void setFilteredMessage(ByteBuffer byteBuffer) {
        bufferMessageFiltered = byteBuffer;
    }

}
