package uk.co.fivium.digitalnotificationlibrary.core.notification;

record Response<T>(T successResponseObject, ErrorResponse error) {
  boolean isSuccessfulResponse() {
    return successResponseObject != null;
  }

  static <T> Response<T> successfulResponse(T responseObject) {
    return new Response<>(responseObject, null);
  }

  static <T> Response<T> failedResponse(int httpStatusCode, String message) {
    return new Response<>(null, new ErrorResponse(httpStatusCode, message));
  }

  record ErrorResponse(int httpStatus, String message) {
  }
}
