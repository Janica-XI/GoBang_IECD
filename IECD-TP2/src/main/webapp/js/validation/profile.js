/**
 * Profile page validation and photo preview functionality.
 * Handles client-side validation for profile updates and photo uploads.
 */

document.addEventListener('DOMContentLoaded', function() {
    const editProfileForm = document.getElementById('editProfileForm');
    
    if (editProfileForm) {
        editProfileForm.addEventListener('submit', function(e) {
            let isValid = true;

            clearPreviousErrors();

            const newPassword = document.getElementById('newPassword').value;
            const confirmPassword = document.getElementById('confirmPassword').value;

            if (newPassword.length > 0) {
                if (newPassword.length < 8 || newPassword.length > 16) {
                    showPasswordError();
                    isValid = false;
                }

                if (newPassword !== confirmPassword) {
                    showConfirmPasswordError();
                    isValid = false;
                }
            }

            if (!isValid) {
                e.preventDefault();
                console.log('Profile form validation failed - correcting password errors');
            }
        });
    }
});

/**
 * Clears all previous validation errors from the form.
 */
function clearPreviousErrors() {
    const passwordError = document.getElementById('passwordError');
    const confirmPasswordError = document.getElementById('confirmPasswordError');
    const newPasswordInput = document.getElementById('newPassword');
    const confirmPasswordInput = document.getElementById('confirmPassword');

    if (passwordError) passwordError.style.display = 'none';
    if (confirmPasswordError) confirmPasswordError.style.display = 'none';
    if (newPasswordInput) newPasswordInput.classList.remove('is-invalid');
    if (confirmPasswordInput) confirmPasswordInput.classList.remove('is-invalid');
}

/**
 * Shows password validation error.
 */
function showPasswordError() {
    const passwordError = document.getElementById('passwordError');
    const newPasswordInput = document.getElementById('newPassword');
    
    if (passwordError) {
        passwordError.textContent = 'A palavra-passe deve ter entre 8 e 16 caracteres.';
        passwordError.style.display = 'block';
    }
    if (newPasswordInput) {
        newPasswordInput.classList.add('is-invalid');
    }
}

/**
 * Shows password confirmation validation error.
 */
function showConfirmPasswordError() {
    const confirmPasswordError = document.getElementById('confirmPasswordError');
    const confirmPasswordInput = document.getElementById('confirmPassword');
    
    if (confirmPasswordError) {
        confirmPasswordError.textContent = 'As palavras-passe não coincidem.';
        confirmPasswordError.style.display = 'block';
    }
    if (confirmPasswordInput) {
        confirmPasswordInput.classList.add('is-invalid');
    }
}

/**
 * Previews uploaded photo before form submission.
 * Updates the profile photo preview image with the selected file.
 * 
 * @param {HTMLInputElement} input - The file input element containing the selected photo
 */
function previewPhoto(input) {
    if (input.files && input.files[0]) {
        const selectedFile = input.files[0];
        
        if (!selectedFile.type.startsWith('image/')) {
            console.log('Selected file is not an image');
            return;
        }

        const reader = new FileReader();
        
        reader.onload = function(e) {
            const previewImage = document.getElementById('profilePhotoPreview');
            if (previewImage) {
                previewImage.src = e.target.result;
                console.log('Photo preview updated successfully');
            }
        };
        
        reader.onerror = function() {
            console.log('Error reading selected photo file');
        };

        reader.readAsDataURL(selectedFile);
    }
}