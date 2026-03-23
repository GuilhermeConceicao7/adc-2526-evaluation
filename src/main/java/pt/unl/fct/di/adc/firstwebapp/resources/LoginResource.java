package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import org.apache.commons.codec.digest.DigestUtils;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response.Status;

import jakarta.servlet.http.HttpServletRequest;

import pt.unl.fct.di.adc.firstwebapp.util.ApiError;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.KeyFactory;
import com.google.cloud.datastore.PathElement;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.StructuredQuery.OrderBy;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.StructuredQuery.CompositeFilter;

import com.google.gson.Gson;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;
import pt.unl.fct.di.adc.firstwebapp.util.Responses.LoginResponse;
import pt.unl.fct.di.adc.firstwebapp.util.UserSession;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {


	private static final String USER_PWD = "user_pwd";
    private static final String USER_ROLE = "role";
	/** 
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());
	private static final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	private static final KeyFactory userKeyFactory = datastore.newKeyFactory().setKind("User");


	private final Gson g = new Gson();
	
	public LoginResource() {} // Nothing to be done here
	

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(RegisterData data, @Context HttpServletRequest request, @Context HttpHeaders headers) {

		RegisterData.Input input = data.input;

		if(!input.validLogin()) {
			ApiError error = new ApiError("INVALID_INPUT", 9906,
					"The call is using input data not following the correct specification");
			return Response.status(Status.BAD_REQUEST).entity(error).build();
		}

		Key userKey = userKeyFactory.newKey(input.username);

		// Generate automatically a key
		Key logKey = datastore.allocateId(
				datastore.newKeyFactory()
						.addAncestors(PathElement.of("User", input.username))
						.setKind("UserLog").newKey());

		Transaction txn = datastore.newTransaction();
		try {
			Entity user = txn.get(userKey);

			if (user == null ){
				ApiError error = new ApiError("USER_NOT_FOUND", 9902,
						"The username referred in the operation doesn’t exist in registered accounts");
				return Response.status(Status.NOT_FOUND).entity(error).build();
			}

			String userPass = user.getString(USER_PWD);
			if (!userPass.equals(DigestUtils.sha512Hex(input.password))) {
				// Username does not exist
				ApiError error = new ApiError("INVALID_CREDENTIALS", 9900,
						"The username-password pair is not valid");
				return Response.status(Status.FORBIDDEN).entity(error).build();
			}

			AuthToken token = new AuthToken(input.username, user.getString(USER_ROLE));

			String cityLatLong = headers.getHeaderString("X-AppEngine-CityLatLong");
			Entity log = Entity.newBuilder(logKey)
					.set("user_login_ip", request.getRemoteAddr())
					.set("user_login_host", request.getRemoteHost())
					.set("user_login_latlon", cityLatLong != null
							? cityLatLong
							: "")
					.set("user_login_city", headers.getHeaderString("X-AppEngine-City"))
					.set("user_login_country", headers.getHeaderString("X-AppEngine-Country"))
					.set("user_login_time", Timestamp.now())
					.build();

			Key sessionKey = datastore.newKeyFactory()
					.setKind("UserSession")
					.newKey(token.getTokenId());

			Entity session = UserSession.create(sessionKey, token);

			txn.put(session);
			txn.put(log);
			txn.commit();

			LoginResponse response = new LoginResponse(token);
			return Response.ok(response).build();

		} catch (Exception e) {
			txn.rollback();
			LOG.severe(e.getMessage());
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}
	}



/**
	@POST
	@Path("/user/login-logs/v2")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLatestLogins(LoginData data) {
		
		Key userKey = userKeyFactory.newKey(data.username);
		
		Entity user = datastore.get(userKey);
		if( user != null && user.getString(USER_PWD).equals(DigestUtils.sha512Hex(data.password))) {
			
			// Get the date of yesterday
			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -1);
			Timestamp yesterday = Timestamp.of(cal.getTime());
			
			Query<Entity> query = Query.newEntityQueryBuilder()
					.setKind("UserLog")
					.setFilter(
							CompositeFilter.and(
									PropertyFilter.hasAncestor(
											datastore.newKeyFactory().setKind("User").newKey(data.username)),
									PropertyFilter.ge(USER_LOGIN_TIME, yesterday)
							)
					)
					.setOrderBy(OrderBy.desc(USER_LOGIN_TIME))
					.setLimit(3)
					.build();
			QueryResults<Entity> logs = datastore.run(query);
			
			List<Date> loginDates = new ArrayList<Date>();
			logs.forEachRemaining(userlog -> {
				loginDates.add(userlog.getTimestamp(USER_LOGIN_TIME).toDate());
			});
			
			return Response.ok(g.toJson(loginDates)).build();
		}
		return Response.status(Status.FORBIDDEN).
				entity("ok")
				.build();
	}

**/


}