package com.businessprocess.core.policy;

public enum FailureType {
    TECHNICAL_FAILURE,
    AUTH_FAILURE,
    BUSINESS_FAILURE,
    CONFLICT,
    UNKNOWN;

    public static FailureType fromHttpStatus(int statusCode) {
        if (statusCode >= 300 && statusCode < 400) {
            return TECHNICAL_FAILURE;
        }
        return switch (statusCode) {
            case 400, 402, 404, 406, 410, 412, 413, 416, 422, 428, 451 -> BUSINESS_FAILURE;
            case 401, 403, 407, 511 -> AUTH_FAILURE;
            case 409, 423 -> CONFLICT;
            case 405, 408, 411, 414, 415, 417, 421, 424, 425, 426, 429, 431 -> TECHNICAL_FAILURE;
            case 500, 501, 502, 503, 504, 505, 506, 507, 508, 510 -> TECHNICAL_FAILURE;
            default -> UNKNOWN;
        };
    }
}
