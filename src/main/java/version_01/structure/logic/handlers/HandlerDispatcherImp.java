package version_01.structure.logic.handlers;

import com.google.protobuf.ByteString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import version_01.core.session.IoSession;
import version_01.core.write.SessionCloseException;
import version_01.home_net.data.db.DatabaseFactory;
import version_01.home_net.data.db.IdentitiesDao;
import version_01.home_net.data.db.exceptions.CantSaveIdentityException;
import version_01.home_net.data.models.IdentityData;
import version_01.home_net.data.models.NodeProfile;
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

    private NodeProfile nodeProfile;

    private IdentitiesDao identitiesDao;

    private CryptoImp crypto;

    public HandlerDispatcherImp(DatabaseFactory databaseFactory,CryptoImp crypto) {
        identitiesDao = new IdentitiesDao(databaseFactory);
        this.crypto = crypto;
    }

    @Override
    public void dispatch(IoSession sessionFrom, TestProto3.Message message) {
        try{
            switch (message.getMessageTypeCase()) {
                case REQUEST:
                    TestProto3.Request request = message.getRequest();
                    switch (request.getConversationTypeCase()) {
                        case CONVERSATIONREQUEST:
                            TestProto3.ConversationRequest conversationRequest = request.getConversationRequest();

                            switch (conversationRequest.getRequestTypeCase()) {
                                case HOMENODEREQUEST:
                                    try {
                                        // check
                                        checkConversationStatus(sessionFrom);
                                        // check if the person is what he is saying.
                                        checkSignature(conversationRequest.getSignature().toString(), sessionFrom);

                                        TestProto3.HomeNodePlanContract contract = conversationRequest.getHomeNodeRequest().getContract();
                                        String identityPk = contract.getIdentityPublicKey().toStringUtf8();
                                        String identityType = contract.getPlan().getIdentityType();
                                        // register identity
                                        registerIdentity(identityPk, identityType);
                                        // test propose
                                        LOG.info("Identity saved in database: " + searchIdentity(identityPk));
                                        // response with the same contract for now..
                                        LOG.info("Sending response to: " + conversationRequest.getRequestTypeCase());
                                        sessionFrom.write(MessageFactory.buildHomeNodeResponseRequest(contract,"SIGNATURE_HERE"));
                                    } catch (CantSaveIdentityException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                case CHECKIN:
                                    //check
                                    checkConversationStatus(sessionFrom);
                                    // check if the person is what he is saying.
                                    checkSignature(conversationRequest.getSignature().toString(), sessionFrom);

                                    sessionFrom.write(MessageFactory.buildCheckInResponse("SIGNATURE_HERE"));


                                    break;
                                case START:
                                    TestProto3.StartConversationRequest startConversationRequest = conversationRequest.getStart();

                                    break;
                                default:
                                    LOG.error("error in conversation request type.." + conversationRequest);
                                    break;
                            }
                            break;
                        case SINGLEREQUEST:
                            TestProto3.SingleRequest singleRequest = request.getSingleRequest();
                            // Message version protocol
                            if (checkVersion(singleRequest.getVersion().toString())) {

                                switch (singleRequest.getRequestTypeCase()) {
                                    case LISTHOMENODEPLANS:

                                        break;
                                    case LISTROLES:

                                        break;
                                    case PING:
                                        sessionFrom.write(MessageFactory.buildPingResponseMessage(TestProto3.PingResponse.newBuilder().setPayload(ByteString.copyFromUtf8("pong")).build(), "1"));
                                        break;
                                    case REQUESTTYPE_NOT_SET:

                                        throw new RuntimeException("REQUESTTYPE_NOT_SET");
                                }

                            } else {
                                // version not supported.
                                sessionFrom.write(MessageFactory.buildVersionNotSupportResponse());
                            }


                            break;

                        case CONVERSATIONTYPE_NOT_SET:
                            LOG.info("Message " + message.getId() + " received with not request type in session: " + sessionFrom.getId() + ", discarting...");
                            break;
                    }


                    break;
                case RESPONSE:

                    break;
                case MESSAGETYPE_NOT_SET:
                    LOG.info("Message " + message.getId() + " received with not message type in session: " + sessionFrom.getId() + ", discarting...");
                    break;
            }
        }catch (SessionCloseException e){
            // nothing to do..
        } catch (ConversationIsNotStartedException e) {
            e.printStackTrace();
        }
    }

    private void checkConversationStatus(IoSession session) throws ConversationIsNotStartedException  {
        if (!session.isConversationStarted()) throw new ConversationIsNotStartedException("Start conversation message is not received");
    }

    /**
     * Check if the message if from an identity of that session
     *
     * @param signature
     * @param session
     */
    private void checkSignature(String signature,IoSession session) {

    }

    /**
     * Check if the request version is supported
     * @param version
     */
    private boolean checkVersion(String version) {
        return true;
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

    private void checkInIdentity(String challenge){
        identitiesDao.checkIn(challenge);
    }

    private void startConversation(){

    }


}
