/**
 * Main JavaScript file for IECD TP Part 2 - GoBang Web Application
 * 
 * Provides global functionality including page loading animations and navigation utilities.
 * 
 * @authors Diogo, Joana, Jaison
 */

window.addEventListener('load', function() {
    setTimeout(hideLoader, 1000);
});

/**
 * Shows the page loader with automatic timeout fallback.
 * Displays loading animation for navigation transitions.
 */
function showLoader() {
    const loader = document.getElementById('pageLoader');
    if (loader) {
        loader.classList.remove('hidden');
        setTimeout(hideLoader, 3000);
    }
}

/**
 * Hides the page loader animation.
 * Removes the loading overlay from the page.
 */
function hideLoader() {
    const loader = document.getElementById('pageLoader');
    if (loader) {
        loader.classList.add('hidden');
    }
}

/**
 * Navigates to the specified URL with loading animation.
 * Shows loader before navigation for smooth user experience.
 * 
 * @param {string} url - The destination URL to navigate to
 */
function navigateToPage(url) {
    if (!url) {
        console.log('Navigation failed: no URL provided');
        return;
    }
    
    showLoader();
    
    setTimeout(() => {
        window.location.href = url;
    }, 500);
}