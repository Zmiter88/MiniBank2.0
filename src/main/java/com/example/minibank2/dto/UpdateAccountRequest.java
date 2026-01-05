package com.example.minibank2.dto;

import com.example.minibank2.entity.AccountType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class UpdateAccountRequest {

    @NotBlank(message = "Owner cannot be blank")
    private String owner;

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
}