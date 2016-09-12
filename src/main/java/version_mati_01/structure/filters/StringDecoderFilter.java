package version_mati_01.structure.filters;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * Created by mati on 11/09/16.
 */
public class StringDecoderFilter extends ProtocolDecoderFilter<String> {


    @Override
    public String decode(ByteBuffer byteBuffer) {
        Charset charset = Charset.forName("ISO-8859-1");
        return charset.decode(byteBuffer).toString();
    }
}
