package version_mati_01.structure;

import version_mati_01.core.filter.IoFilter;
import version_mati_01.core.filter.IoFilterChain;
import version_mati_01.core.filter.IoFilterChainBuilder;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultFilterChainBuilder implements IoFilterChainBuilder {

    private IoFilterChain ioFilterChain;

    public DefaultFilterChainBuilder() {

    }

    private IoFilter protocolDecoderFilter(){
        return null;
    }

    @Override
    public void buildFilterChain(IoFilterChain filterChain) {

    }
}
