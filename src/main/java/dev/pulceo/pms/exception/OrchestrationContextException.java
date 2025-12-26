package dev.pulceo.pms.exception;

public class OrchestrationContextException extends Exception {

    public OrchestrationContextException() {

    }

    public OrchestrationContextException(String message) {
        super(message);
    }

    public OrchestrationContextException(String message, Throwable cause) {
        super(message, cause);
    }

    public OrchestrationContextException(Throwable cause) {
        super(cause);
    }

    public OrchestrationContextException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
