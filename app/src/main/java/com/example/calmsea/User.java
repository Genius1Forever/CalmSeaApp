package com.example.calmsea;

public class User {
    private String email;
    private String name;
    private String birthDate;
    private String gender;
    private String phoneNumber;
    private String profileImageUrl;

    public User() {} // Пустой конструктор для Firebase

    public User(String email, String name, String birthDate, String gender, String phoneNumber, String profileImageUrl) {
        this.email = email;
        this.name = name;
        this.birthDate = birthDate;
        this.gender = gender;
        this.phoneNumber = phoneNumber;
        this.profileImageUrl = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }
}

