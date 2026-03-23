package pt.unl.fct.di.adc.firstwebapp.util.Responses;

public class UserRoleResponse {

    public String status;
    public Data data;

    public static class Data {
        public String userId;
        public String role;

        public Data(String userId, String role) {
            this.userId = userId;
            this.role = role;
        }
    }

    public UserRoleResponse(String userId, String role) {
        this.status = "success";
        this.data = new Data(userId, role);
    }
}