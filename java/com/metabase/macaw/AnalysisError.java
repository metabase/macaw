package com.metabase.macaw;

public class AnalysisError extends RuntimeException {
    public final AnalysisErrorType errorType;
    public final Throwable cause;

    public AnalysisError(AnalysisErrorType errorType) {
        this.errorType = errorType;
        this.cause = null;
    }

    public AnalysisError(AnalysisErrorType errorType, Throwable cause) {
        this.errorType = errorType;
        this.cause = cause;
    }
}
