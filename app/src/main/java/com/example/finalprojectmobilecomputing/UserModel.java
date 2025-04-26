package com.example.finalprojectmobilecomputing;

public class UserModel {
    private String email;
    private String phone;

    public UserModel() {
        // Required empty constructor for Firebase
    }

    public UserModel(String email, String phone) {
        this.email = email;
        this.phone = phone;
    }

    public String getEmail() { return email; }
    public String getPhone() { return phone; }
}
