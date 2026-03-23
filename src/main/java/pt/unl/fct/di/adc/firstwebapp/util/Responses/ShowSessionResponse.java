package pt.unl.fct.di.adc.firstwebapp.util.Responses;

import java.util.List;

public class ShowSessionResponse {

    public String status;
    public Data data;

    public ShowSessionResponse(List<SessionInfo> sessions){
        this.status = "Sucess";
        this.data = new Data(sessions);
    }

    public static class Data {
        public List<SessionInfo> users;

        public Data(List<SessionInfo> users) {
            this.users = users;
        }
    }
}
