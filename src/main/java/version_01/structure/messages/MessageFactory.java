package version_01.structure.messages;

import com.google.protobuf.ByteString;
import version_01.structure.protos.TestProto3;

/**
 * Created by mati on 20/09/16.
 */
public class MessageFactory {

    /**
     * Build ping request message
     *
     * @param pingRequest
     * @param version
     * @return
     */
    public static TestProto3.Message buildPingRequestMessage(TestProto3.PingRequest pingRequest,String version){
        TestProto3.SingleRequest.Builder singleRequestBuilder = TestProto3.SingleRequest.newBuilder().setPing(pingRequest).setVersion(ByteString.copyFromUtf8(version));
        return buildMessage(singleRequestBuilder);

    }

    /**
     * Build ping response message
     *
     * @param pingResponse
     * @param version
     * @return
     */
    public static TestProto3.Message buildPingResponseMessage(TestProto3.PingResponse pingResponse,String version){
        TestProto3.SingleResponse.Builder singleResponseBuilder = TestProto3.SingleResponse.newBuilder().setPing(pingResponse).setVersion(ByteString.copyFromUtf8(version));
        return buildMessage(singleResponseBuilder);
    }

    /**
     *  Build server list roles request message
     *
     * @param listRolesRequest
     * @param version
     * @return
     */

    public static TestProto3.Message buildServerListRolesRequestMessage(TestProto3.ListRolesRequest listRolesRequest,String version){
        TestProto3.SingleRequest.Builder singleRequest = TestProto3.SingleRequest.newBuilder().setListRoles(listRolesRequest).setVersion(ByteString.copyFromUtf8(version));
        return buildMessage(singleRequest);
    }

    /**
     *  Build server list roles response message
     *
     * @param listRolesRequest
     * @param version
     * @return
     */
    public static TestProto3.Message buildServerListRolesResponseMessage(TestProto3.ListRolesResponse listRolesRequest,String version){
        TestProto3.SingleResponse.Builder singleRequest = TestProto3.SingleResponse.newBuilder().setListRoles(listRolesRequest).setVersion(ByteString.copyFromUtf8(version));
        return buildMessage(singleRequest);
    }

    /**
     *  Build serverRole
     *
     * @param roleType
     * @param port
     * @param isSecure
     * @param isTcp
     * @return
     */
    public static TestProto3.ServerRole buildServerRole(TestProto3.ServerRoleType roleType, int port, boolean isSecure, boolean isTcp){
        return TestProto3.ServerRole.newBuilder().setIsTcp(isTcp).setIsTls(isSecure).setPort(port).setRole(roleType).build();
    }

    public static TestProto3.HomeNodePlan buildHomeNodePlan(String planId,String identityType,int billingPeriodseconds,long fee){
        return TestProto3.HomeNodePlan.newBuilder().setIdentityType(identityType).setBillingPeriodSeconds(billingPeriodseconds).setPlanId(ByteString.copyFromUtf8(planId)).setFee(fee).build();
    }

    public static TestProto3.HomeNodePlanContract buildHomeNodePlanContract(String identityPk, String nodePk, long startTime, TestProto3.HomeNodePlan plan){
        return TestProto3.HomeNodePlanContract.newBuilder().setIdentityPublicKey(ByteString.copyFromUtf8(identityPk)).setNodePublicKey(ByteString.copyFromUtf8(nodePk)).setStartTime(startTime).setPlan(plan).build();
    }

    public static TestProto3.Message buildHomeNodeRequestRequest(TestProto3.HomeNodePlanContract contract){
        TestProto3.HomeNodeRequestRequest homeNodeRequestRequest = TestProto3.HomeNodeRequestRequest.newBuilder().setContract(contract).build();
        TestProto3.ConversationRequest.Builder conversaBuilder = TestProto3.ConversationRequest.newBuilder().setHomeNodeRequest(homeNodeRequestRequest);
        return buildMessage(conversaBuilder);
    }

    public static TestProto3.Message buildHomeNodeResponseRequest(TestProto3.HomeNodePlanContract contract,String signature){
        TestProto3.HomeNodeRequestResponse homeNodeRequestRequest = TestProto3.HomeNodeRequestResponse.newBuilder().setContract(contract).build();
        TestProto3.ConversationResponse.Builder conversaBuilder = TestProto3.ConversationResponse.newBuilder().setHomeNodeRequest(homeNodeRequestRequest);
        conversaBuilder.setSignature(ByteString.copyFromUtf8(signature));
        return buildMessage(conversaBuilder);
    }

    public static TestProto3.Message buildCheckInResponse(String signature){
        TestProto3.CheckInResponse checkInResponse = TestProto3.CheckInResponse.newBuilder().build();
        TestProto3.ConversationResponse.Builder conversaBuilder = TestProto3.ConversationResponse.newBuilder().setCheckIn(checkInResponse);
        conversaBuilder.setSignature(ByteString.copyFromUtf8(signature));
        return buildMessage(conversaBuilder);
    }

    /**
     * Error messages
     */


    public static TestProto3.Message buildInvalidMessageHeaderResponse(){
        return buildMessage(TestProto3.Status.ERROR_PROTOCOL_VIOLATION);
    }

    public static TestProto3.Message buildVersionNotSupportResponse() {
        // todo: Creo que se devuelve el unnsopported cuando la versión no es valida, deberia chequear esto..
        return buildMessage(TestProto3.Status.ERROR_UNSUPPORTED);
    }


    /**
     *  Private builders
     */

    private static TestProto3.Message buildMessage(TestProto3.Status responseStatus){
        return buildMessage(TestProto3.Response.newBuilder().setStatus(responseStatus));
    }

    private static TestProto3.Message buildMessage(TestProto3.SingleRequest.Builder singleRequest){
        TestProto3.Request.Builder requestBuilder = TestProto3.Request.newBuilder().setSingleRequest(singleRequest);
        return buildMessage(requestBuilder);
    }

    private static TestProto3.Message buildMessage(TestProto3.ConversationRequest.Builder conversationRequest){
        TestProto3.Request.Builder requestBuilder = TestProto3.Request.newBuilder().setConversationRequest(conversationRequest);
        return buildMessage(requestBuilder);
    }

    private static TestProto3.Message buildMessage(TestProto3.Request.Builder request){
        TestProto3.Message.Builder messageBuilder = TestProto3.Message.newBuilder().setRequest(request);
        return messageBuilder.build();
    }

    private static TestProto3.Message buildMessage(TestProto3.SingleResponse.Builder singleResponse){
        TestProto3.Response.Builder responseBuilder = TestProto3.Response.newBuilder().setSingleResponse(singleResponse);
        return buildMessage(responseBuilder);
    }

    private static TestProto3.Message buildMessage(TestProto3.Response.Builder response){
        TestProto3.Message.Builder messageBuilder = TestProto3.Message.newBuilder().setResponse(response);
        return messageBuilder.build();
    }

    private static TestProto3.Message buildMessage(TestProto3.ConversationResponse.Builder conversationResponse) {
        TestProto3.Response.Builder requestBuilder = TestProto3.Response.newBuilder().setConversationResponse(conversationResponse);
        return buildMessage(requestBuilder);
    }


}
