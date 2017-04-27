package com.anders.reride.aws;

/**
 * Generic async task for asynchronous operations
 * @param <T> Generic type
 */
class AsyncTaskResult<T> {
    private T result;
    private Exception error;

    T getResult() {
        return result;
    }

    Exception getError() {
        return error;
    }

    AsyncTaskResult(T result) {
        super();
        this.result = result;
    }

    AsyncTaskResult(Exception error) {
        super();
        this.error = error;
    }
}
