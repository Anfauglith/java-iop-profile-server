package stress_clients;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.sql.Time;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mati on 07/09/16.
 */
public class NioClientTest {


    public static AtomicInteger conexionEstablecida = new AtomicInteger(0);
    public static int j = 1;

    public static void main(String[] args){

        ExecutorService executorService = Executors.newFixedThreadPool(10000);

        for (int i=0;i<10000;i++){
//            if ( i ==  1500*j) try {
//                j++;
//                TimeUnit.SECONDS.sleep(1);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            executorService.submit(() -> runClient());

            if (i==1999){
                System.out.println("############ cantidad de conexiones establecidas = "+conexionEstablecida);
            }
        }

    }

    public static void runClient(){
        try {

            SocketChannel socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);

            Selector selector = Selector.open();

            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            socketChannel.connect(new InetSocketAddress(9888));

            AtomicBoolean atomicBooleanWriteable = new AtomicBoolean(false);

            while (true){

                try {

                    int interestKeys = selector.select();
                    if (interestKeys>0){
                        Iterator<SelectionKey> it = selector.selectedKeys().iterator();
                        while (it.hasNext()){
                            try {
                                SelectionKey selectionKey = it.next();
                                it.remove();
                                if (selectionKey.isConnectable()){
                                    System.out.println("Conexion establecida!, cantidad conectados: "+conexionEstablecida.incrementAndGet());
                                    ((SocketChannel)selectionKey.channel()).finishConnect();
                                    selectionKey.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
                                }
                                if (selectionKey.isReadable()){
                                    System.out.println("Estoy por leer algo!");

                                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
                                    SocketChannel socketChannel1 = ((SocketChannel)selectionKey.channel());
                                    socketChannel1.read(byteBuffer);
                                    System.out.println("byteBuffer a leer: "+byteBuffer);
                                    byteBuffer.flip();
                                    Charset charset = Charset.forName("ISO-8859-1");
                                    String decodeMessage = charset.decode(byteBuffer).toString();
                                    System.out.println("Llegó: "+ decodeMessage);//bytesToStringUTFNIO(byteBuffer.array()));
//                                    System.exit(1);
                                }
                                if (selectionKey.isWritable()){
                                    if (!atomicBooleanWriteable.get() && selectionKey.channel().isOpen()) {
//                                        atomicBooleanWriteable.compareAndSet(false, true);
                                        System.out.println("Estoy por escribir un socketChannel!");
                                        Charset charset = Charset.forName("ISO-8859-1");
                                        String saludo = "hola que tal";
                                        CharBuffer charBuffer = CharBuffer.allocate(saludo.length()).wrap(saludo);

                                        ByteBuffer byteBuffer = charset.encode(charBuffer);
                                        charBuffer.flip();

                                        System.out.println("charBuffer: "+ charBuffer.toString());
                                        System.out.println("byteBuffer: "+ byteBuffer);
//                                        byteBuffer.put(stringToBytesUTFNIO(saludo));
                                        try {
                                            int writeCount = ((SocketChannel)selectionKey.channel()).write(byteBuffer);
                                            System.out.println("Write count: "+writeCount);
                                        }catch (IOException e){
                                            System.out.println("Se removió la selectionKey con exito!");
                                            System.out.println("Cerrando conexión");
                                            selectionKey.cancel();
                                        }
                                        // selection read
                                        selectionKey.interestOps(SelectionKey.OP_READ);
                                    }
                                }

                            }catch (Exception e){
                                e.printStackTrace();
                            }

                        }
                    }


                    Thread.sleep(1000);

                }catch (Exception e){
                    e.printStackTrace();
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    public static String bytesToStringUTFNIO(byte[] bytes) {
        CharBuffer cBuffer = ByteBuffer.wrap(bytes).asCharBuffer();
        return cBuffer.toString();
    }

    public static byte[] stringToBytesUTFNIO(String str) {
        char[] buffer = str.toCharArray();
        byte[] b = new byte[buffer.length << 1];
        CharBuffer cBuffer = ByteBuffer.wrap(b).asCharBuffer();
        for(int i = 0; i < buffer.length; i++)
            cBuffer.put(buffer[i]);
        return b;
    }



}
