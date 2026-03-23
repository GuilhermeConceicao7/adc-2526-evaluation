package pt.unl.fct.di.adc.firstwebapp.util;

import java.util.UUID;

public class AuthToken {

	public static final long EXPIRATION_TIME = 1000*60*60*2; // 2h
	public static final long TESTING_TIME = 1000*20; // 20 sec

	public String username;
	public String tokenID;
	public String role;
	public long creationData;
	public long expirationData;
	
	public AuthToken() { }
	
	public AuthToken(String username, String role) {
		this.username = username;
		this.role = role;
		this.tokenID = UUID.randomUUID().toString();
		this.creationData = System.currentTimeMillis();
		this.expirationData = this.creationData + EXPIRATION_TIME;
	}


	public String getTokenId() {
		return tokenID;
	}

	public String getUserId() {
		return username;
	}

	public String getRole(){
		return role;
	}

	public long getIssuedAt() {
		return creationData;
	}

	public long getExpiresAt() {
		return expirationData;
	}
}
