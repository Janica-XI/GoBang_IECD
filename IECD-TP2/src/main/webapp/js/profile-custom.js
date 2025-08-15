/**
 * Theme selector functionality for profile page.
 * Handles interactive theme selection with enhanced UX for clickable theme options.
 */

document.addEventListener('DOMContentLoaded', function() {
    console.log('Setting up theme selector functionality');

    const themeRadioButtons = document.querySelectorAll('input[name="theme"]');
    const themeOptionContainers = document.querySelectorAll('.theme-option');

    if (themeRadioButtons.length === 0) {
        console.log('No theme radio buttons found on this page');
        return;
    }

    console.log(`Found ${themeRadioButtons.length} theme radio buttons and ${themeOptionContainers.length} theme option containers`);

    setupThemeOptionClickHandlers();
    setupThemeChangeHandlers();

    /**
     * Sets up click handlers for theme option containers.
     * Allows users to click anywhere on the theme option area to select it.
     */
    function setupThemeOptionClickHandlers() {
        themeOptionContainers.forEach(optionContainer => {
            optionContainer.addEventListener('click', function() {
                const radioButton = optionContainer.querySelector('input[type="radio"]');
                if (radioButton) {
                    selectThemeOption(radioButton);
                }
            });
        });
    }

    /**
     * Sets up change event listeners for radio buttons.
     * Handles theme selection feedback and additional logic.
     */
    function setupThemeChangeHandlers() {
        themeRadioButtons.forEach(radioButton => {
            radioButton.addEventListener('change', function() {
                if (this.checked) {
                    handleThemeSelection(this.value);
                }
            });
        });
    }

    /**
     * Selects a specific theme option and triggers change events.
     * 
     * @param {HTMLInputElement} targetRadioButton - The radio button to select
     */
    function selectThemeOption(targetRadioButton) {
        clearAllThemeSelections();
        
        targetRadioButton.checked = true;
        targetRadioButton.dispatchEvent(new Event('change'));
        
        console.log(`Theme selected: ${targetRadioButton.value}`);
    }

    /**
     * Clears all theme selections by unchecking all radio buttons.
     */
    function clearAllThemeSelections() {
        themeRadioButtons.forEach(radioButton => {
            radioButton.checked = false;
        });
    }

    /**
     * Handles theme selection with logging and potential future enhancements.
     * 
     * @param {string} selectedTheme - The value of the selected theme
     */
    function handleThemeSelection(selectedTheme) {
        console.log(`Theme changed to: ${selectedTheme}`);
        
        // Future enhancement: Could add visual feedback, preview, or validation here
    }
});