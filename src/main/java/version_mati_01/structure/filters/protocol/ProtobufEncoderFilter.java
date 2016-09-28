package version_mati_01.structure.filters.protocol;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.filter.base.ProtocolEncoderFilter;
import version_mati_01.structure.protos.TestProto3;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by mati on 19/09/16.
 */
public class ProtobufEncoderFilter extends ProtocolEncoderFilter<TestProto3.Message> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(ProtobufEncoderFilter.class);

    @Override
    public ByteBuffer encode(TestProto3.Message message) {
        LOG.info("Encoding message..");
        byte[] byteMessageToSend = message.toByteArray();
        LOG.info(" bytes: "+ Arrays.toString(byteMessageToSend));
        return ByteBuffer.wrap(byteMessageToSend);
    }


    public static void main(String[] args){


//        LOG.info(String.valueOf(pingRequest.isInitialized()));
//        LOG.info(String.valueOf(pingRequest.getSerializedSize()));
//        LOG.info(String.valueOf(pingRequest.toString()));


        TestProto3.PingRequest pingRequest1 = TestProto3.PingRequest.newBuilder().setPayload(ByteString.copyFromUtf8("h")).build();

        TestProto3.SingleRequest singleRequest = TestProto3.SingleRequest.newBuilder().setPing(pingRequest1).build();

        TestProto3.Request request = TestProto3.Request.newBuilder().setSingleRequest(singleRequest).build();

        TestProto3.Message message = TestProto3.Message.newBuilder().setRequest(request).build();

        LOG.info("ping: "+pingRequest1.toString());
        LOG.info("singleRequest: "+singleRequest.toString());
        LOG.info("request:  "+request.toString());
        LOG.info("message: "+message.toString());

        ByteString version = ByteString.copyFromUtf8("1");




//        LOG.info(messageToSend.getInitializationErrorString());
    }
}
