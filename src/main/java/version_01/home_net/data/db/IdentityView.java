package version_01.home_net.data.db;

import com.sleepycat.bind.EntryBinding;
import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.SerialBinding;
import com.sleepycat.collections.StoredEntrySet;
import com.sleepycat.collections.StoredSortedMap;
import version_01.home_net.data.models.IdentityData;
import version_01.home_net.data.models.IdentityKey;

/**
 * Created by mati on 25/09/16.
 */
public class IdentityView {

    private StoredSortedMap<IdentityKey,IdentityData> identityMap;

    public IdentityView(DatabaseFactory databaseFactory) {

        ClassCatalog classCatalog = databaseFactory.getClassCatalog();

        EntryBinding<IdentityKey> identityKeyBinding = new SerialBinding<>(classCatalog, IdentityKey.class);
        EntryBinding<IdentityData> identityDataBinding = new SerialBinding<>(classCatalog,IdentityData.class);

        identityMap = new StoredSortedMap<>(databaseFactory.getIdentityDb(),identityKeyBinding,identityDataBinding,true);

    }

    public StoredSortedMap<IdentityKey, IdentityData> getIdentityMap() {
        return identityMap;
    }

    public final StoredEntrySet getIdentityEntrySet(){
        return (StoredEntrySet) identityMap.entrySet();
    }
}
