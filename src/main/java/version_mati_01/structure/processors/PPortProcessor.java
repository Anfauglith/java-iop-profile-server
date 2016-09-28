package version_mati_01.structure.processors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_mati_01.core.polling.AbstractProcessor;
import version_mati_01.core.service.IoService;
import version_mati_01.core.write.WriteRequest;
import version_mati_01.structure.processors.internal.IoReadProcessor;
import version_mati_01.structure.processors.internal.primary.PrimaryProcessorWriteManager;
import version_mati_01.structure.session.BaseSession;
import version_mati_01.structure.session.IoSessionIterator;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by mati on 23/09/16.
 */
public class PPortProcessor extends AbstractProcessor<BaseSession> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(PPortProcessor.class);

    /** Selector */
    private Selector selector;

    public PPortProcessor(IoService ioService, int id) {
        super(ioService, id,new IoReadProcessor(),new PrimaryProcessorWriteManager());
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
        synchronized (selector) {
            Set<SelectionKey> keys = selector.keys();

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
    protected void preInitSession(BaseSession session) throws Exception {
        // session not secure
        session.setSecure(false);
    }

    @Override
    public Selector getSelector() {
        return selector;
    }

}
