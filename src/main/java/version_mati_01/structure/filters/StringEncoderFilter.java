package version_mati_01.structure.filters;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;

/**
 * Created by mati on 11/09/16.
 */
public class StringEncoderFilter extends ProtocolEncoderFilter<String> {


    @Override
    public ByteBuffer encode(String message) {
        Charset charset = Charset.forName("ISO-8859-1");
        CharBuffer charBuffer = CharBuffer.allocate(message.length()).wrap(message);
        return charset.encode(charBuffer);
    }
}
