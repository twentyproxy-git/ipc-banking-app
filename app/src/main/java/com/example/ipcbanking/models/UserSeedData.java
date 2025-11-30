package com.example.ipcbanking.models;

public class UserSeedData {
    private String email;
    private String password;
    private String fullName;
    private String phoneNumber;
    private String address; // [MỚI] Thêm địa chỉ
    private String role;
    private String avatarUrl;

    public UserSeedData() {
    }

    // Cập nhật Constructor thêm address
    public UserSeedData(String email, String password, String fullName, String phoneNumber, String address, String role, String avatarUrl) {
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.role = role;
        this.avatarUrl = avatarUrl;
    }

    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public String getPhoneNumber() { return phoneNumber; }

    // Getter Address
    public String getAddress() { return address; }

    public String getRole() { return role; }
    public String getAvatarUrl() { return avatarUrl; }
}