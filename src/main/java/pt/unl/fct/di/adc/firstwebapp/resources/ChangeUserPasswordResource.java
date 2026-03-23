package pt.unl.fct.di.adc.firstwebapp.resources;

import com.google.cloud.datastore.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.codec.digest.DigestUtils;
import pt.unl.fct.di.adc.firstwebapp.util.ApiError;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.Responses.GenericResponse;

import java.util.logging.Logger;

@Path("/ChangeUserPassword")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class ChangeUserPasswordResource {
    private static final Logger LOG = Logger.getLogger(ChangeUserRoleResource.class.getName());
    private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
    private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");

    public ChangeUserPasswordResource() {
    }

    public static class ChangePasswordRequest {
        public Input input;
        public AuthToken token;
    }

    public static class Input {
        public String userId;
        public String oldPassword;
        public String newPassword;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ChangePasswordRequest data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

        Input input = data.input;
        AuthToken token = data.token;


        if (token == null || token.getTokenId() == null || token.getTokenId().isBlank()) {
            ApiError error = new ApiError("INVALID_TOKEN", 9903,
                    "The operation is called with an invalid token (wrong format for example)");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        }

        Key sessionKey = datastore.newKeyFactory()
                .setKind("UserSession")
                .newKey(token.getTokenId());

        Transaction txn = datastore.newTransaction();

        try {
            Entity session = txn.get(sessionKey);

            if (session == null) {
                ApiError error = new ApiError("INVALID_TOKEN", 9903,
                        "The operation is called with an invalid token (wrong format for example)");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            long now = System.currentTimeMillis();
            if (session.getTimestamp("expiresAt").toDate().getTime() < now) {
                ApiError error = new ApiError("TOKEN_EXPIRED", 9904,
                        "The operation is called with a token that is expired");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            String targetUserId = input.userId;
            Key userKey = userKeyFactory.newKey(targetUserId);
            Entity user = txn.get(userKey);

            String actualUser = session.getString("username");

            if(!actualUser.equals(targetUserId)){
                ApiError error = new ApiError("UNAUTHORIZED", 9905,
                        "The operation is not allowed for the user role");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            String userPass = user.getString("user_pwd");

            if (!userPass.equals(DigestUtils.sha512Hex(input.oldPassword))) {
                ApiError error = new ApiError("INVALID_CREDENTIALS", 9900,
                        "The username-password pair is not valid");
                return Response.status(Response.Status.FORBIDDEN).entity(error).build();
            }

            String newPassword = input.newPassword;

            Entity updatedUser = Entity.newBuilder(user)
                    .set("user_pwd", DigestUtils.sha512Hex(newPassword))
                    .build();

            txn.put(updatedUser);
            txn.commit();

            GenericResponse response = new GenericResponse("sucess", "Password changed successfully");
            return Response.ok(response).build();
        } catch (Exception e) {
            txn.rollback();
            LOG.severe(e.getMessage());
            ApiError error = new ApiError("FORBIDDEN", 9907,
                    "The operation generated a forbidden error by other reason");
            return Response.status(Response.Status.FORBIDDEN).entity(error).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
            }
        }

    }
}
