package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import jakarta.ws.rs.Produces;
import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import com.google.gson.Gson;
import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.DatastoreOptions;

import pt.unl.fct.di.adc.firstwebapp.util.ApiError;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.Responses.RegisterResponse;

@Path("/createAccount")
public class RegisterResource {

	private static final Logger LOG = Logger.getLogger(RegisterResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

	private final Gson g = new Gson();


	public RegisterResource() {}	// Default constructor, nothing to do

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
    public Response registerUser(RegisterData data) {

        RegisterData.Input input = data.input;


        if(!input.validRegistration()) {
            ApiError error = new ApiError("INVALID_INPUT", 9906,
                    "The call is using input data not following the correct specification");
            return Response.status(Status.BAD_REQUEST).entity(error).build();
        }
        try {
            Transaction txn = datastore.newTransaction();
            Key userKey = datastore.newKeyFactory().setKind("User").newKey(input.username);
            Entity user = txn.get(userKey);

            if(user != null) {
                txn.rollback();
                ApiError error = new ApiError("USER_ALREADY_EXISTS", 9901,
                        "Error in creating an account because the username already exists");
                return Response.status(Status.CONFLICT).entity(error).build();            }
            else {
                user = Entity.newBuilder(userKey)
                        .set("user_name", input.username)
                        .set("user_pwd", DigestUtils.sha512Hex(input.password))
                        .set("user_email", input.email)
						.set("phone", input.phone)
						.set("address", input.address)
						.set("role", input.role)
						.set("creation_time", Timestamp.now())
                        .build();
                txn.put(user);
                txn.commit();
                LOG.info("User registered " + input.username);

                RegisterResponse response = new RegisterResponse("success", input.username, input.role);
                return Response.ok(response).build();
            }
        } catch (Exception e) {
            LOG.severe("Error registering user: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity("Error registering user.").build();
        }
        finally {
            // No need to rollback here, as we only have one transaction and it will be automatically rolled back if not committed.
        }
    }
}