package com.example.LoanMatrix.exception;


import com.example.LoanMatrix.response.ApiResponse;
import com.example.LoanMatrix.response.ErrorDetails;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<String> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleOtherExceptions(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Something went wrong. Please try again later.");
    }




    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<String> handleFileStorage(FileStorageException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
    }

    @ExceptionHandler(ScheduleAlreadyExistsException.class)
    public ResponseEntity<String> handleScheduleAlreadyExists(ScheduleAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidLoanStatusException.class)
    public ResponseEntity<String> handleInvalidLoanStatus(InvalidLoanStatusException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

}
