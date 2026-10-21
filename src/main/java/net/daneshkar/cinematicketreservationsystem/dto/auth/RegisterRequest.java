package net.daneshkar.cinematicketreservationsystem.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "نام کاربری الزامی است")
        @Size(min = 3, max = 20, message = "نام کاربری باید بین ۳ تا ۲۰ کاراکتر باشد")
        String username,

        @NotBlank(message = "ایمیل الزامی است")
        @Email(message = "فرمت ایمیل معتبر نیست")
        String email,

        @NotBlank(message = "رمز عبور الزامی است")
        @Size(min = 6, message = "رمز عبور باید حداقل ۶ کاراکتر باشد")
        String password
) {
}
