package dev.pulceo.pms.exception;

public class MetricsServiceException extends Exception {
    public MetricsServiceException() {
    }

    public MetricsServiceException(String message) {
        super(message);
    }

    public MetricsServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public MetricsServiceException(Throwable cause) {
        super(cause);
    }

    public MetricsServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
