/**
 * Registration form validation with comprehensive client-side validation.
 * Supports both registration and profile update forms with flexible field validation.
 */

document.addEventListener('DOMContentLoaded', function() {
    const form = document.querySelector('form[action="register"]');
    const usernameInput = document.getElementById('username');
    const newPasswordInput = document.getElementById('password');
    const confirmPasswordInput = document.getElementById('passwordConfirmation');
    const dateOfBirthInput = document.getElementById('dateOfBirth');
    
    const usernameError = document.getElementById('usernameError');
    const passwordError = document.getElementById('passwordError');
    const confirmPasswordError = document.getElementById('passwordConfirmationError');

    function validateUsername(username) {
        const trimmedUsername = username.trim().toLowerCase();
        if (trimmedUsername.length < 3) {
            return 'O nome de utilizador deve ter pelo menos 3 caracteres.';
        } else if (trimmedUsername.length > 8) {
            return 'O nome de utilizador deve ter no máximo 8 caracteres.';
        }
        return null;
    }

    function validatePassword(password) {
        if (password === '') {
            return null;
        }
        
        if (password.length < 8) {
            return 'A palavra-passe deve ter pelo menos 8 caracteres.';
        } else if (password.length > 16) {
            return 'A palavra-passe deve ter no máximo 16 caracteres.';
        }
        return null;
    }

    function validatePasswordConfirmation(password, confirmPassword) {
        if (password === '' && confirmPassword === '') {
            return null;
        }
        
        if (password !== confirmPassword) {
            return 'As palavras-passe não coincidem.';
        }
        
        return null;
    }

    function validateDateOfBirth(dateString) {
        if (!dateString) {
            return null;
        }
        
        const birthDate = new Date(dateString);
        const today = new Date();
        const minDate = new Date();
        minDate.setFullYear(today.getFullYear() - 120);
        
        const maxDate = new Date();
        maxDate.setFullYear(today.getFullYear() - 6);
        
        if (birthDate > maxDate) {
            return 'Deve ter pelo menos 6 anos de idade.';
        }
        
        if (birthDate < minDate) {
            return 'Data de nascimento inválida.';
        }
        
        return null;
    }

    function showError(input, errorDiv, message) {
        if (input && errorDiv) {
            input.classList.add('is-invalid');
            input.classList.remove('is-valid');
            errorDiv.textContent = message;
            errorDiv.style.display = 'block';
        }
    }

    function showSuccess(input, errorDiv) {
        if (input && errorDiv) {
            input.classList.remove('is-invalid');
            input.classList.add('is-valid');
            errorDiv.style.display = 'none';
        }
    }

    function createErrorDiv(input, id) {
        let errorDiv = document.getElementById(id);
        if (!errorDiv) {
            errorDiv = document.createElement('div');
            errorDiv.id = id;
            errorDiv.className = 'invalid-feedback';
            errorDiv.style.display = 'none';
            input.parentNode.appendChild(errorDiv);
        }
        return errorDiv;
    }

    function clearValidationOnFocus(input) {
        if (input) {
            input.addEventListener('focus', function() {
                if (this.classList.contains('is-invalid')) {
                    this.classList.remove('is-invalid');
                    const errorDiv = this.parentNode.querySelector('.invalid-feedback');
                    if (errorDiv) {
                        errorDiv.style.display = 'none';
                    }
                }
            });
        }
    }

    if (usernameInput) {
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
    }

    if (newPasswordInput) {
        newPasswordInput.addEventListener('input', function() {
            const password = this.value;
            const error = validatePassword(password);
            
            if (error) {
                showError(this, passwordError, error);
            } else {
                showSuccess(this, passwordError);
            }
            
            if (confirmPasswordInput && confirmPasswordInput.value) {
                const confirmError = validatePasswordConfirmation(password, confirmPasswordInput.value);
                if (confirmError) {
                    showError(confirmPasswordInput, confirmPasswordError, confirmError);
                } else {
                    showSuccess(confirmPasswordInput, confirmPasswordError);
                }
            }
        });
    }

    if (confirmPasswordInput) {
        confirmPasswordInput.addEventListener('input', function() {
            const password = newPasswordInput ? newPasswordInput.value : '';
            const confirmPassword = this.value;
            const error = validatePasswordConfirmation(password, confirmPassword);
            
            if (error) {
                showError(this, confirmPasswordError, error);
            } else {
                showSuccess(this, confirmPasswordError);
            }
        });
    }

    if (dateOfBirthInput) {
        const dobErrorDiv = createErrorDiv(dateOfBirthInput, 'dobError');
        
        dateOfBirthInput.addEventListener('input', function() {
            const dateString = this.value;
            const error = validateDateOfBirth(dateString);
            
            if (error) {
                showError(this, dobErrorDiv, error);
            } else {
                showSuccess(this, dobErrorDiv);
            }
        });
    }

    if (form) {
        form.addEventListener('submit', function(e) {
            let isValid = true;
            let firstInvalidField = null;
            
            if (usernameInput) {
                usernameInput.value = usernameInput.value.toLowerCase();
                
                const usernameValidationError = validateUsername(usernameInput.value);
                if (usernameValidationError) {
                    showError(usernameInput, usernameError, usernameValidationError);
                    isValid = false;
                    if (!firstInvalidField) firstInvalidField = usernameInput;
                }
            }
            
            if (newPasswordInput) {
                const passwordValidationError = validatePassword(newPasswordInput.value);
                if (passwordValidationError) {
                    showError(newPasswordInput, passwordError, passwordValidationError);
                    isValid = false;
                    if (!firstInvalidField) firstInvalidField = newPasswordInput;
                }
            }
            
            if (confirmPasswordInput) {
                const confirmValidationError = validatePasswordConfirmation(
                    newPasswordInput ? newPasswordInput.value : '',
                    confirmPasswordInput.value
                );
                if (confirmValidationError) {
                    showError(confirmPasswordInput, confirmPasswordError, confirmValidationError);
                    isValid = false;
                    if (!firstInvalidField) firstInvalidField = confirmPasswordInput;
                }
            }
            
            if (dateOfBirthInput) {
                const dobValidationError = validateDateOfBirth(dateOfBirthInput.value);
                const dobErrorDiv = document.getElementById('dobError');
                if (dobValidationError) {
                    showError(dateOfBirthInput, dobErrorDiv, dobValidationError);
                    isValid = false;
                    if (!firstInvalidField) firstInvalidField = dateOfBirthInput;
                }
            }

            if (!isValid) {
                e.preventDefault();
                
                if (firstInvalidField) {
                    firstInvalidField.focus();
                    firstInvalidField.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }
                
                console.log('Registration form validation failed - correcting errors before submission');
            }
        });
    }

    [usernameInput, newPasswordInput, confirmPasswordInput, dateOfBirthInput]
        .forEach(input => clearValidationOnFocus(input));
});

/**
 * Utility object for profile validation that can be used by other scripts.
 * Provides consistent validation logic for registration and profile updates.
 */
const ProfileValidator = {
    isValidUsername: function(username) {
        const trimmed = username.trim().toLowerCase();
        return trimmed.length >= 3 && trimmed.length <= 8;
    },
    
    isValidPassword: function(password) {
        return password === '' || (password.length >= 8 && password.length <= 16);
    },
    
    isValidAge: function(birthDate) {
        if (!birthDate) return true;
        const birth = new Date(birthDate);
        const today = new Date();
        const age = today.getFullYear() - birth.getFullYear();
        return age >= 6 && age <= 120;
    },

    validateRegistrationForm: function(username, password, confirmPassword, dateOfBirth) {
        const normalizedUsername = username.trim().toLowerCase();
        return {
            username: {
                isValid: this.isValidUsername(username),
                normalizedValue: normalizedUsername,
                error: normalizedUsername.length < 3 ? 'O nome de utilizador deve ter pelo menos 3 caracteres.' :
                       normalizedUsername.length > 8 ? 'O nome de utilizador deve ter no máximo 8 caracteres.' : null
            },
            password: {
                isValid: this.isValidPassword(password),
                error: password.length > 0 && password.length < 8 ? 'A palavra-passe deve ter pelo menos 8 caracteres.' :
                       password.length > 16 ? 'A palavra-passe deve ter no máximo 16 caracteres.' : null
            },
            passwordConfirmation: {
                isValid: password === confirmPassword,
                error: password !== confirmPassword ? 'As palavras-passe não coincidem.' : null
            },
            dateOfBirth: {
                isValid: this.isValidAge(dateOfBirth),
                error: !this.isValidAge(dateOfBirth) ? 'Data de nascimento inválida.' : null
            }
        };
    }
};