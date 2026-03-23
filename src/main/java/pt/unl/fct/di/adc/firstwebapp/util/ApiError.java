package pt.unl.fct.di.adc.firstwebapp.util;

public class ApiError {
    public String Error;
    public int Code;
    public String ErrorMessage;

    public ApiError(String error, int code, String errorMessage) {
        this.Error = error;
        this.Code = code;
        this.ErrorMessage = errorMessage;
    }
}