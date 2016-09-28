package stress_clients;

import com.google.protobuf.ByteString;
import version_01.ssl.SslContextFactory;
import version_01.structure.filters.protocol.ProtobufDecoderFilter;
import version_01.structure.filters.protocol.ProtobufEncoderFilter;
import version_01.structure.messages.MessageFactory;
import version_01.structure.protos.TestProto3;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Created by mati on 19/09/16.
 */
public class NioSslPingClientTest extends Thread{

    private SSLSocket sslSocket;

    private static int clientsOk;

    int clientId;

    int sentMessages = 0;

    public NioSslPingClientTest(int id, SSLContext sslContext, String host, int port) throws IOException {
        this.sslSocket = (SSLSocket) sslContext.getSocketFactory().createSocket(host,port);
        this.sslSocket.setReuseAddress(true);
        this.clientId = id;
    }

    public void run(){
        WritableByteChannel writableChannel = null;
        try {

            System.out.println("Running..");
            int count;

            ProtobufEncoderFilter protobufEncoderFilter = new ProtobufEncoderFilter();
            ProtobufDecoderFilter protobufDecoderFilter = new ProtobufDecoderFilter();

            //writableChannel
            writableChannel = Channels.newChannel(sslSocket.getOutputStream());
            int randomSleepTime = new Random().nextInt(10);
//            ByteBuffer inputBuffer = ByteBuffer.allocate(1024);
            byte[] buffer = new byte[8192];
            while (sentMessages<=100) {
                //protobuff ping request encoder

                TestProto3.Message message = MessageFactory.buildPingRequestMessage(TestProto3.PingRequest.newBuilder().setPayload(ByteString.copyFromUtf8("ping")).build(), "1");
                ByteBuffer byteBuffer = protobufEncoderFilter.encode(message);
                // send request
                System.out.println("ByteBuffer to sent: " + byteBuffer);
                writableChannel.write(byteBuffer);
                System.out.println("ByteBuffer enviados: " + byteBuffer);
                byteBuffer.compact();
                // read reply
//                inputBuffer.flip();
//                count = readableChannel.read(inputBuffer);
                // read reply
                count = sslSocket.getInputStream().read(buffer);
                System.out.println("Message response read count: "+count);
                TestProto3.Message messageResponse = protobufDecoderFilter.decode(ByteBuffer.wrap(buffer,0,count));
                System.out.println("Message response: " + messageResponse.getId());
                buffer = null;
                buffer = new byte[8192];

                sentMessages++;

                try {
                    TimeUnit.SECONDS.sleep(randomSleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }catch (IOException e){
            e.printStackTrace();
        } finally {
            try {
                clientsOk++;
                System.out.println("Clientes atendidos bien hasta el momento: "+ clientsOk);
                if (writableChannel != null) {
                    writableChannel.close();
                }
                sslSocket.close();
                System.out.println("client: socket closed, id: "+clientId);
            }catch (IOException e){
//                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args){

        try {

            int clientsCont = 1;

            SSLContext sslContext = SslContextFactory.buildContext();
            ExecutorService executorService = Executors.newFixedThreadPool(clientsCont);

            for (int i=0;i<clientsCont;i++){
                int finalI = i;
                executorService.submit(() -> {
                    try {
                        new NioSslPingClientTest(finalI +1,sslContext,"localhost",9888).start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
