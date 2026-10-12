package com.example.LoanMatrix.exception;


import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.response.ErrorDetails;

import lombok.extern.slf4j.Slf4j;



import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler
{
    @ExceptionHandler(CustomerNotFoundException.class)

    public ResponseEntity<ApiResponse<Void>> handleCustomerNotFound(
            CustomerNotFoundException ex) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Customer not found")
                .data(null)
                .error(
                        ErrorDetails.builder()
                                .error("CUSTOMER_NOT_FOUND")
                                .details(ex.getMessage())
                                .build()
                )
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(CibilAlreadyGeneratedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCibilAlreadyGenerated(
            CibilAlreadyGeneratedException ex) {

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(false)
                        .message("CIBIL already generated")
                        .data(null)
                        .error(
                                ErrorDetails.builder()
                                        .error("CIBIL_ALREADY_GENERATED")
                                        .details(ex.getMessage())
                                        .build()
                        )
                        .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }


    @ExceptionHandler(EligibilityDecisionAlreadyMadeException.class)
    public ResponseEntity<ApiResponse<Void>> handleEligibilityDecisionAlreadyMade(
            EligibilityDecisionAlreadyMadeException ex) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ApiResponse<>(
                        false,
                        ex.getMessage(),
                        null,
                        null
                ));
    }


    @ExceptionHandler(LoanAccountNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleLoanAccountNotFound(
            LoanAccountNotFoundException ex
    ) {

        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Loan account not found",
                ex.getMessage(),
                "LOAN_ACCOUNT_NOT_FOUND"
        );
    }

    @ExceptionHandler(UnauthorizedLoanAccessException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorizedLoanAccess(
            UnauthorizedLoanAccessException ex
    ) {

        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Unauthorized loan account access",
                ex.getMessage(),
                "UNAUTHORIZED_LOAN_ACCESS"
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(
            MethodArgumentNotValidException ex
    ) {

        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Validation failed",
                message,
                "VALIDATION_ERROR"
        );
    }

    private ResponseEntity<ApiResponse<Void>> buildErrorResponse(
            HttpStatus status,
            String message,
            String details,
            String errorCode
    ) {

        ApiResponse<Void> response =
                ApiResponse.<Void>builder()
                        .success(false)
                        .message(message)
                        .data(null)
                        .error(
                                ErrorDetails.builder()
                                        .error(errorCode)
                                        .details(details)
                                        .build()
                        )
                        .build();

        return ResponseEntity
                .status(status)
                .body(response);
    }

    @ExceptionHandler(EmiAlreadyPaidException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException( RuntimeException ex) {
        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Request failed")
                .data(null)
                .error(ErrorDetails.builder()
                        .error(ex.getMessage())
                        .details("Please check your request and try again.")
                        .build()
                )
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(EmiNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEmiNotFound(EmiNotFoundException ex) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Request failed")
                .data(null)
                .error(ErrorDetails.builder()
                        .error(ex.getMessage())
                        .details("The EMI does not exist or does not belong to the specified loan account.")
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(PaymentAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<Void>> handlePaymentAlreadyVerified(PaymentAlreadyVerifiedException ex) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Payment verification failed")
                .data(null)
                .error(ErrorDetails.builder()
                        .error(ex.getMessage())
                        .details("This payment has already been verified.")
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }


    @ExceptionHandler(InvalidPaymentSignatureException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidPaymentSignature(InvalidPaymentSignatureException ex) {

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .success(false)
                .message("Payment verification failed")
                .data(null)
                .error(ErrorDetails.builder()
                        .error(ex.getMessage())
                        .details("The payment signature is invalid.")
                        .build())
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
       log.error("Resource not found: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .error("NOT_FOUND")
                .details(ex.getMessage())
                .build();

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidLoanOperationException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidLoanOperationException(InvalidLoanOperationException ex) {
        log.error("Invalid loan operation: {}", ex.getMessage());

        ErrorDetails errorDetails = ErrorDetails.builder()
                .error("BAD_REQUEST")
                .details(ex.getMessage())
                .build();

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(ex.getMessage())
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(Exception ex) {
        log.error("An unexpected error occurred: ", ex);

        ErrorDetails errorDetails = ErrorDetails.builder()
                .error("INTERNAL_SERVER_ERROR")
                .details(ex.getMessage())
                .build();

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("An unexpected error occurred. Please try again.")
                .data(null)
                .error(errorDetails)
                .build();

        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
