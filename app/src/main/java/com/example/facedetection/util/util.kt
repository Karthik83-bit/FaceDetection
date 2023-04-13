package com.example.facedetection.util

import java.util.regex.Pattern

object util {
    fun validateAadharNumber(aadharNumber: String): Boolean {
        val aadharPattern = Pattern.compile("\\d{12}")
        var isValidAadhar = aadharPattern.matcher(aadharNumber).matches()
        if (isValidAadhar) {
            isValidAadhar = Verhoff.validateVerhoeff(aadharNumber)
        }
        return isValidAadhar
    }
}