package com.he187383.barber.dto;

import jakarta.validation.constraints.*;

public class ServiceDtos {

    public record ServiceCreateReq(
            @NotBlank String name,
            @NotNull @Min(1) Integer durationMin,
            @NotNull @Min(0) Integer price,
            Boolean isActive
    ) {}
    public record ServiceUpdateReq(
            String name,
            Integer durationMin,
            Integer price,
            Boolean isActive
    ) {}

}
