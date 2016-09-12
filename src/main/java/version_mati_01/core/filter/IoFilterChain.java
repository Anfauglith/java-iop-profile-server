package version_mati_01.core.filter;

/**
 * Created by mati on 11/09/16.
 */
public interface IoFilterChain {

    void addFirst(IoFilter ioFilter,int filterTypes);

    void addLast(IoFilter ioFilter, int filterTypes);

    int size(int filterType);


}
