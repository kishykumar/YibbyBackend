package com.baasbox.exception;

/**
 * Created by eto on 25/09/14.
 */
public class IllegalRequestException extends Exception {
    public IllegalRequestException() {
    }

    public IllegalRequestException(String message) {
        super(message);
    }

    public IllegalRequestException(String message, Throwable cause) {
        super(message, cause);
    }

    public IllegalRequestException(Throwable cause) {
        super(cause);
    }
}
