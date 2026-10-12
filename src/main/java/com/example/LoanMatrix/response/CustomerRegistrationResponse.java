package com.example.LoanMatrix.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegistrationResponse {

    private Long id;

    private String firstName;

    private String lastName;

    private String email;

    private String mobileNo;

    private String aadhaarNo;

    private String panCard;

    private String employmentType;

    private boolean emailVerified;
}
