/**
 * Login form validation with client-side username and password validation.
 * Provides real-time feedback and ensures usernames are normalized to lowercase.
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form[action="login"]');
    const usernameInput = document.getElementById('username');
    const passwordInput = document.getElementById('password');
    const usernameError = document.getElementById('usernameError');
    const passwordError = document.getElementById('passwordError');

    function validateUsername(username) {
        const trimmedUsername = username.trim();
        if (trimmedUsername.length < 3) {
            return 'O nome de utilizador deve ter pelo menos 3 caracteres.';
        } else if (trimmedUsername.length > 8) {
            return 'O nome de utilizador deve ter no máximo 8 caracteres.';
        }
        return null;
    }

    function validatePassword(password) {
        if (password.length < 8) {
            return 'A palavra-passe deve ter pelo menos 8 caracteres.';
        } else if (password.length > 16) {
            return 'A palavra-passe deve ter no máximo 16 caracteres.';
        }
        return null;
    }

    function showError(input, errorDiv, message) {
        input.classList.add('is-invalid');
        input.classList.remove('is-valid');
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
    }

    function showSuccess(input, errorDiv) {
        input.classList.remove('is-invalid');
        input.classList.add('is-valid');
        errorDiv.style.display = 'none';
    }

    function clearValidationOnFocus(input, errorDiv) {
        input.addEventListener('focus', function() {
            if (this.classList.contains('is-invalid')) {
                this.classList.remove('is-invalid');
                errorDiv.style.display = 'none';
            }
        });
    }

    usernameInput.addEventListener('blur', function() {
        this.value = this.value.toLowerCase();
    });

    usernameInput.addEventListener('input', function() {
        const username = this.value;
        const error = validateUsername(username);
        
        if (error) {
            showError(this, usernameError, error);
        } else {
            showSuccess(this, usernameError);
        }
    });

    passwordInput.addEventListener('input', function() {
        const password = this.value;
        const error = validatePassword(password);
        
        if (error) {
            showError(this, passwordError, error);
        } else {
            showSuccess(this, passwordError);
        }
    });

    form.addEventListener('submit', function(e) {
        let isValid = true;
        
        usernameInput.value = usernameInput.value.toLowerCase();
        
        const usernameValidationError = validateUsername(usernameInput.value);
        if (usernameValidationError) {
            showError(usernameInput, usernameError, usernameValidationError);
            isValid = false;
        } else {
            showSuccess(usernameInput, usernameError);
        }
        
        const passwordValidationError = validatePassword(passwordInput.value);
        if (passwordValidationError) {
            showError(passwordInput, passwordError, passwordValidationError);
            isValid = false;
        } else {
            showSuccess(passwordInput, passwordError);
        }
        
        if (!isValid) {
            e.preventDefault();
            
            if (usernameValidationError) {
                usernameInput.focus();
            } else if (passwordValidationError) {
                passwordInput.focus();
            }
            
            console.log('Login form validation failed - correcting errors before submission');
        }
    });

    clearValidationOnFocus(usernameInput, usernameError);
    clearValidationOnFocus(passwordInput, passwordError);
});

/**
 * Utility object for login validation that can be used by other scripts.
 * Provides consistent validation logic across the application.
 */
const LoginValidator = {
    isValidUsername: function(username) {
        const trimmed = username.trim().toLowerCase();
        return trimmed.length >= 3 && trimmed.length <= 8;
    },
    
    isValidPassword: function(password) {
        return password.length >= 8 && password.length <= 16;
    },
    
    normalizeUsername: function(username) {
        return username.trim().toLowerCase();
    },
    
    validateForm: function(username, password) {
        const normalizedUsername = this.normalizeUsername(username);
        return {
            username: {
                isValid: this.isValidUsername(username),
                normalizedValue: normalizedUsername,
                error: normalizedUsername.length < 3 ? 'O nome de utilizador deve ter pelo menos 3 caracteres.' :
                       normalizedUsername.length > 8 ? 'O nome de utilizador deve ter no máximo 8 caracteres.' : null
            },
            password: {
                isValid: this.isValidPassword(password),
                error: password.length < 8 ? 'A palavra-passe deve ter pelo menos 8 caracteres.' :
                       password.length > 16 ? 'A palavra-passe deve ter no máximo 16 caracteres.' : null
            }
        };
    }
};