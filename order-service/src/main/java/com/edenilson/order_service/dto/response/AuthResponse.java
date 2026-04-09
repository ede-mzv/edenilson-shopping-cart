package com.edenilson.order_service.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponse {

    private String token;
    private Long customerId;
    private String email;
    private String firstName;
    private String lastName;
}
