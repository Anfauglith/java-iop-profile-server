package version_mati_01.structure;

import version_mati_01.core.filter.FilterTypes;
import version_mati_01.core.filter.IoFilter;
import version_mati_01.core.filter.IoFilterChain;

import java.util.LinkedList;

/**
 * Created by mati on 11/09/16.
 */
public class DefaultIoFilterChain implements IoFilterChain{

    private LinkedList<IoFilter> messageReceivedList;

    public DefaultIoFilterChain(){
        messageReceivedList = new LinkedList<>();
    }

    @Override
    public void addFirst(IoFilter ioFilter, int filterTypes) {
        if (filterTypes == FilterTypes.MSG_RECEIVED)
            messageReceivedList.addFirst(ioFilter);
    }

    @Override
    public void addLast(IoFilter ioFilter, int filterTypes) {
        if (filterTypes == FilterTypes.MSG_RECEIVED)
            messageReceivedList.addLast(ioFilter);
    }

    @Override
    public int size(int filterType) {
        return 0;
    }

}
