package dev.pulceo.pms.exception;

public class MetricsQueryServiceException extends Exception {

    public MetricsQueryServiceException() {
    }

    public MetricsQueryServiceException(String message) {
        super(message);
    }

    public MetricsQueryServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricsQueryServiceException(Throwable cause) {
        super(cause);
    }

    public MetricsQueryServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
