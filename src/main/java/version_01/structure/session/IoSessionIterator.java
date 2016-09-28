package version_01.structure.session;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;
import java.util.Set;

/**
 * An encapsulating iterator around the {@link Selector#selectedKeys()} or
 * the {@link Selector#keys()} iterator;
 */
public class IoSessionIterator<ClientSession> implements Iterator<ClientSession> {
    private final Iterator<SelectionKey> iterator;

    /**
     * Create this iterator as a wrapper on top of the selectionKey Set.
     *
     * @param keys
     *            The set of selected sessions
     */
    public IoSessionIterator(Set<SelectionKey> keys) {
        iterator = keys.iterator();
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasNext() {
        return iterator.hasNext();
    }

    /**
     * {@inheritDoc}
     */
    public ClientSession next() {
        SelectionKey key = iterator.next();
        ClientSession nioSession = (ClientSession) key.attachment();
        return nioSession;
    }

    /**
     * {@inheritDoc}
     */
    public void remove() {
        iterator.remove();
    }
}