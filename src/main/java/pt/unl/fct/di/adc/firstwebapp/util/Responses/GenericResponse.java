package pt.unl.fct.di.adc.firstwebapp.util.Responses;

public class GenericResponse {

    public String status;
    public String message;

    public GenericResponse(String status, String message){
        this.status = status;
        this.message = message;
    }

    public static class Data {
        public String message;


        public Data(String message) {
            this.message = message;
        }
    }
}
