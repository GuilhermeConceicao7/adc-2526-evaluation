package pt.unl.fct.di.adc.firstwebapp.util;

import com.google.gson.annotations.SerializedName;

public class RegisterData {


	@SerializedName("input")
	public Input input;

	public static class Input {
		public String username;
		public String password;
		public String confirmation;
		public String email;
		public String phone;
		public String address;
		public String role;

		public Input() {}

		public Input(String username, String password, String confirmation, String email, String phone,
					 String address, String role) {
			this.username = username;
			this.password = password;
			this.confirmation = confirmation;
			this.email = email;
			this.phone = phone;
			this.address = address;
			this.role = role;
		}

		private boolean nonEmptyOrBlankField(String field) {
			return field != null && !field.isBlank();
		}

		private boolean validRole() {
			return nonEmptyOrBlankField(role) &&  (role.equals("USER") ||
					role.equals("BOFFICER") ||
					role.equals("ADMIN"));
		}

		public boolean validRegistration() {
			return nonEmptyOrBlankField(username) &&
					nonEmptyOrBlankField(password) &&
					nonEmptyOrBlankField(email) &&
					nonEmptyOrBlankField(phone) &&
					nonEmptyOrBlankField(address) &&
					validRole() &&
					email.contains("@") &&
					password.equals(confirmation);
		}

		public boolean validLogin() {
			return nonEmptyOrBlankField(username) &&
					nonEmptyOrBlankField(password);
		}
	}
}