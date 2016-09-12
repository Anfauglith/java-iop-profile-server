package version_mati_01.core.filter;

/**
 * Created by mati on 11/09/16.
 */
public interface IoFilter<T> {

    void filterNext(IoFilter next,int filterType,T data);



}
