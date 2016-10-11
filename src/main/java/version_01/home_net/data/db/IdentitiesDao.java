package version_01.home_net.data.db;

import com.sleepycat.collections.TransactionRunner;
import version_01.home_net.data.db.exceptions.CantSaveIdentityException;
import version_01.home_net.data.models.IdentityData;
import version_01.home_net.data.models.IdentityKey;

import java.util.*;

/**
 * Created by mati on 25/09/16.
 */
public class IdentitiesDao {

    private DatabaseFactory databaseFactory;

    private IdentityView identityView;

    public IdentitiesDao(DatabaseFactory databaseFactory) {
        this.databaseFactory = databaseFactory;
        identityView = new IdentityView(databaseFactory);
    }

    public Map<IdentityKey,IdentityData> getIdentities(){
        return new HashMap<>(identityView.getIdentityMap());
    }

    public IdentityData getIdentity(byte[] key){
        return identityView.getIdentityMap().get(new IdentityKey(key));
    }

    public void saveIdentity(IdentityData identity) throws CantSaveIdentityException{
        TransactionRunner transactionRunner = new TransactionRunner(databaseFactory.getEnvironment());
        try {
            transactionRunner.run(()->identityView.getIdentityMap().put(new IdentityKey(identity.getIdentityId()),identity));
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     *
     * @param id is the hash of the identity's public key
     * @return
     */
    public boolean isIdentityOnline(byte[] id){
        return identityView.getIdentityMap().containsKey(new IdentityKey(id));
    }

    public void checkIn(String challenge) {

    }
}
