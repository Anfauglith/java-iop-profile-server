package version_01.structure.filters.protocol;

import com.google.protobuf.InvalidProtocolBufferException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.filter.base.ProtocolDecoderFilter;
import version_01.structure.protos.TestProto3;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mati on 20/09/16.
 */
public class ProtobufDecoderFilter extends ProtocolDecoderFilter<TestProto3.Message> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(ProtobufDecoderFilter.class);

    @Override
    public TestProto3.Message decode(ByteBuffer byteBuffer) {
        TestProto3.Message message = null;
        try {
            LOG.info("Decoding byteBuffer..");
            ByteBuffer byteBufferToRead = ByteBuffer.allocate(byteBuffer.remaining());
            byteBufferToRead.put(byteBuffer.array(),0,byteBuffer.limit());
            LOG.info("ByteBuffer to decode: "+ Arrays.toString(byteBufferToRead.array()));
            message = TestProto3.Message.parseFrom(byteBufferToRead.array());
        } catch (InvalidProtocolBufferException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        return message;
    }
}
