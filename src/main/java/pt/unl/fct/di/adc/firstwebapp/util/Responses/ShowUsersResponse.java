package pt.unl.fct.di.adc.firstwebapp.util.Responses;

import java.util.List;

public class ShowUsersResponse {

    public String status;
    public Data data;

    public ShowUsersResponse(List<UserInfo> users) {
        this.status = "success";
        this.data = new Data(users);
    }

    public static class Data {
        public List<UserInfo> users;

        public Data(List<UserInfo> users) {
            this.users = users;
        }
    }
}