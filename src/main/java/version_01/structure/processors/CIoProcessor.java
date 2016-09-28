package version_01.structure.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.polling.AbstractProcessor;
import version_01.core.service.IoService;
import version_01.core.write.WriteRequest;
import version_01.ssl.SslContextFactory;
import version_01.ssl.SslFilter;
import version_01.structure.processors.internal.IoReadProcessor;
import version_01.structure.processors.internal.IoWriteProcessor;
import version_01.structure.session.BaseSession;
import version_01.structure.session.IoSessionIterator;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by mati on 22/09/16.
 */
public class CIoProcessor extends AbstractProcessor<BaseSession> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(CIoProcessor.class);

    /** Selector */
    private Selector selector;



    public CIoProcessor(IoService ioService, int id) {
        super(ioService,id,new IoReadProcessor(),new IoWriteProcessor());
        init();
    }

    private void init(){

        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open a selector.",e);
        }
    }


    @Override
    public boolean isDisposing() {
        return false;
    }

    @Override
    public boolean isDisposed() {
        return false;
    }

    @Override
    public void dispose() throws Exception {
        selector.close();
    }

    @Override
    public void write(BaseSession session, WriteRequest writeRequest) {
        getSupportIoListener().fireSendMessage(this,session,writeRequest);
    }


    @Override
    protected Iterator<BaseSession> selectedSessions() {
        return new IoSessionIterator<>(selector.selectedKeys());
    }

    @Override
    protected void registerNewSelector() throws IOException  {
        synchronized (getSelector()) {
            Set<SelectionKey> keys = getSelector().keys();

            // Open a new selector
            Selector newSelector = Selector.open();


            // Loop on all the registered keys, and register them on the new selector
            for (SelectionKey key : keys) {
                SelectableChannel ch = key.channel();

                // Don't forget to attache the session, and back !
                BaseSession session = (BaseSession) key.attachment();
                SelectionKey newKey = ch.register(newSelector, key.interestOps(), session);
                session.setSelectionKey(newKey);
            }

            // Now we can close the old selector and switch it
            selector.close();
            selector = newSelector;
        }
    }

    @Override
    public Selector getSelector() {
        return selector;
    }

    @Override
    public void preInitSession(BaseSession session) throws Exception {
        // Bogus ssl context initialization
        SSLContext sslContext = SslContextFactory.buildContext();

        // SSL filter
        SslFilter sslFilter = new SslFilter(sslContext);
        sslFilter.onPreAdd(session);
    }

}
