package com.rrjaggery.commerce.dto;

import jakarta.validation.constraints.NotBlank;

public class AddressDto {

    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    @NotBlank(message = "Phone number is required")
    private String phone;

    @NotBlank(message = "Street address is required")
    private String streetAddress;

    @NotBlank(message = "Apartment or building name is required")
    private String apartmentName;

    @NotBlank(message = "City is required")
    private String city;

    @NotBlank(message = "State is required")
    private String state;

    @NotBlank(message = "Postal code / PIN is required")
    private String postalCode;

    @NotBlank(message = "Nearest landmark is required")
    private String nearestLandmark;

    public AddressDto() {}

    public AddressDto(String recipientName, String phone, String streetAddress, String city, String state, String postalCode) {
        this(recipientName, phone, "", streetAddress, city, state, postalCode, "");
    }

    public AddressDto(String recipientName, String phone, String apartmentName, String streetAddress,
                      String city, String state, String postalCode, String nearestLandmark) {
        this.recipientName = recipientName;
        this.phone = phone;
        this.apartmentName = apartmentName;
        this.streetAddress = streetAddress;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.nearestLandmark = nearestLandmark;
    }

    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getApartmentName() { return apartmentName; }
    public void setApartmentName(String apartmentName) { this.apartmentName = apartmentName; }

    public String getStreetAddress() { return streetAddress; }
    public void setStreetAddress(String streetAddress) { this.streetAddress = streetAddress; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getState() { return state; }
    public void setState(String state) { this.state = state; }

    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }

    public String getNearestLandmark() { return nearestLandmark; }
    public void setNearestLandmark(String nearestLandmark) { this.nearestLandmark = nearestLandmark; }
}
