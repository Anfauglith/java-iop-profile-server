package version_01.structure.logic.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.session.IoSession;
import version_01.home_net.data.db.DatabaseFactory;
import version_01.home_net.data.db.IdentitiesDao;
import version_01.home_net.data.db.exceptions.CantSaveIdentityException;
import version_01.home_net.data.models.IdentityData;
import version_01.structure.logic.HandlerDispatcher;
import version_01.structure.logic.crypto.Crypto;
import version_01.structure.logic.crypto.CryptoImp;
import version_01.structure.messages.MessageFactory;
import version_01.structure.protos.TestProto3;

import java.io.UnsupportedEncodingException;

/**
 * Created by mati on 02/10/16.
 */
public class HandlerDispatcherImp implements HandlerDispatcher<TestProto3.Message> {

    /** A logger for this class */
    private final static Logger LOG = LoggerFactory.getLogger(HandlerDispatcherImp.class);

    private IdentitiesDao identitiesDao;

    private CryptoImp crypto;

    public HandlerDispatcherImp(DatabaseFactory databaseFactory,CryptoImp crypto) {
        identitiesDao = new IdentitiesDao(databaseFactory);
        this.crypto = crypto;
    }

    @Override
    public void dispatch(IoSession sessionFrom, TestProto3.Message message) {
        switch (message.getMessageTypeCase()){
            case REQUEST:
                TestProto3.Request request = message.getRequest();
                switch (request.getConversationTypeCase()){
                    case CONVERSATIONREQUEST:
                        TestProto3.ConversationRequest conversationRequest = request.getConversationRequest();
                        switch (conversationRequest.getRequestTypeCase()){
                            case HOMENODEREQUEST:
                                try {
                                    TestProto3.HomeNodePlanContract contract = conversationRequest.getHomeNodeRequest().getContract();
                                    String identityPk = contract.getIdentityPublicKey().toStringUtf8();
                                    String identityType = contract.getPlan().getIdentityType();
                                    // register identity
                                    registerIdentity(identityPk,identityType);
                                    // test propose
                                    LOG.info("Identity saved in database: "+searchIdentity(identityPk));
                                    // response with the same contract for now..
                                    LOG.info("Sending response to: "+conversationRequest.getRequestTypeCase());
                                    sessionFrom.write(MessageFactory.buildHomeNodeResponseRequest(contract));
                                } catch (CantSaveIdentityException e) {
                                    e.printStackTrace();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                break;
                            case CHECKIN:
                                TestProto3.CheckInRequest checkInRequest = conversationRequest.getCheckIn();
//                                checkInRequest.getChallenge()
                                break;
                            case START:
                                TestProto3.StartConversationRequest startConversationRequest = conversationRequest.getStart();

                                break;
                            default:
                                LOG.error("error in conversation request type.."+conversationRequest);
                                break;
                        }
                        break;
                    case SINGLEREQUEST:
                        throw new RuntimeException("SINGLEREQUEST not implemented.");

                    case CONVERSATIONTYPE_NOT_SET:
                        LOG.info("Message "+message.getId()+" received with not request type in session: "+sessionFrom.getId()+", discarting...");
                        break;
                }


                break;
            case RESPONSE:

                break;
            case MESSAGETYPE_NOT_SET:
                LOG.info("Message "+message.getId()+" received with not message type in session: "+sessionFrom.getId()+", discarting...");
                break;

        }
    }


    private void registerIdentity(String identityPk,String identityType) throws CantSaveIdentityException {
        try {
            identitiesDao.saveIdentity(
                    new IdentityData(
                            crypto.sha256(identityPk),
                            identityPk.getBytes("UTF-8"),
                            "1".getBytes(),
                            identityPk,
                            identityType,
                            null,
                            0,
                            null));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private IdentityData searchIdentity(String identityPk){
        return identitiesDao.getIdentity(crypto.sha256(identityPk));
    }

    private void checkInIdentity(String identityPk){
//        identitiesDao.checkIn()
    }

    private void startConversation(){

    }


}
