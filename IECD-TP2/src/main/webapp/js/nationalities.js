/**
 * Nationality selection utilities for form inputs.
 * Provides comprehensive list of nationalities with flag support and datalist population.
 */

const nationalities = [
    { nome: "Afegã", codigo: "AF", emoji: "🇦🇫" },
    { nome: "Sul-africana", codigo: "ZA", emoji: "🇿🇦" },
    { nome: "Alemã", codigo: "DE", emoji: "🇩🇪" },
    { nome: "Angolana", codigo: "AO", emoji: "🇦🇴" },
    { nome: "Saudita", codigo: "SA", emoji: "🇸🇦" },
    { nome: "Argelina", codigo: "DZ", emoji: "🇩🇿" },
    { nome: "Argentina", codigo: "AR", emoji: "🇦🇷" },
    { nome: "Australiana", codigo: "AU", emoji: "🇦🇺" },
    { nome: "Austríaca", codigo: "AT", emoji: "🇦🇹" },
    { nome: "Belga", codigo: "BE", emoji: "🇧🇪" },
    { nome: "Boliviana", codigo: "BO", emoji: "🇧🇴" },
    { nome: "Brasileira", codigo: "BR", emoji: "🇧🇷" },
    { nome: "Búlgara", codigo: "BG", emoji: "🇧🇬" },
    { nome: "Cabo-verdiana", codigo: "CV", emoji: "🇨🇻" },
    { nome: "Camaronesa", codigo: "CM", emoji: "🇨🇲" },
    { nome: "Canadiana", codigo: "CA", emoji: "🇨🇦" },
    { nome: "Chilena", codigo: "CL", emoji: "🇨🇱" },
    { nome: "Chinesa", codigo: "CN", emoji: "🇨🇳" },
    { nome: "Cipriota", codigo: "CY", emoji: "🇨🇾" },
    { nome: "Colombiana", codigo: "CO", emoji: "🇨🇴" },
    { nome: "Sul-coreana", codigo: "KR", emoji: "🇰🇷" },
    { nome: "Costarriquenha", codigo: "CR", emoji: "🇨🇷" },
    { nome: "Croata", codigo: "HR", emoji: "🇭🇷" },
    { nome: "Cubana", codigo: "CU", emoji: "🇨🇺" },
    { nome: "Dinamarquesa", codigo: "DK", emoji: "🇩🇰" },
    { nome: "Egípcia", codigo: "EG", emoji: "🇪🇬" },
    { nome: "Emiratense", codigo: "AE", emoji: "🇦🇪" },
    { nome: "Equatoriana", codigo: "EC", emoji: "🇪🇨" },
    { nome: "Eslovaca", codigo: "SK", emoji: "🇸🇰" },
    { nome: "Eslovena", codigo: "SI", emoji: "🇸🇮" },
    { nome: "Espanhola", codigo: "ES", emoji: "🇪🇸" },
    { nome: "Estadunidense", codigo: "US", emoji: "🇺🇸" },
    { nome: "Etíope", codigo: "ET", emoji: "🇪🇹" },
    { nome: "Filipina", codigo: "PH", emoji: "🇵🇭" },
    { nome: "Finlandesa", codigo: "FI", emoji: "🇫🇮" },
    { nome: "Francesa", codigo: "FR", emoji: "🇫🇷" },
    { nome: "Ganesa", codigo: "GH", emoji: "🇬🇭" },
    { nome: "Grega", codigo: "GR", emoji: "🇬🇷" },
    { nome: "Holandesa", codigo: "NL", emoji: "🇳🇱" },
    { nome: "Húngara", codigo: "HU", emoji: "🇭🇺" },
    { nome: "Indiana", codigo: "IN", emoji: "🇮🇳" },
    { nome: "Indonésia", codigo: "ID", emoji: "🇮🇩" },
    { nome: "Britânica", codigo: "GB", emoji: "🇬🇧" },
    { nome: "Iraniana", codigo: "IR", emoji: "🇮🇷" },
    { nome: "Iraquiana", codigo: "IQ", emoji: "🇮🇶" },
    { nome: "Irlandesa", codigo: "IE", emoji: "🇮🇪" },
    { nome: "Israelita", codigo: "IL", emoji: "🇮🇱" },
    { nome: "Italiana", codigo: "IT", emoji: "🇮🇹" },
    { nome: "Jamaicana", codigo: "JM", emoji: "🇯🇲" },
    { nome: "Japonesa", codigo: "JP", emoji: "🇯🇵" },
    { nome: "Libanesa", codigo: "LB", emoji: "🇱🇧" },
    { nome: "Marroquina", codigo: "MA", emoji: "🇲🇦" },
    { nome: "Mexicana", codigo: "MX", emoji: "🇲🇽" },
    { nome: "Moçambicana", codigo: "MZ", emoji: "🇲🇿" },
    { nome: "Nigeriana", codigo: "NG", emoji: "🇳🇬" },
    { nome: "Norueguesa", codigo: "NO", emoji: "🇳🇴" },
    { nome: "Neozelandesa", codigo: "NZ", emoji: "🇳🇿" },
    { nome: "Panamenha", codigo: "PA", emoji: "🇵🇦" },
    { nome: "Paquistanesa", codigo: "PK", emoji: "🇵🇰" },
    { nome: "Paraguaia", codigo: "PY", emoji: "🇵🇾" },
    { nome: "Peruana", codigo: "PE", emoji: "🇵🇪" },
    { nome: "Polaca", codigo: "PL", emoji: "🇵🇱" },
    { nome: "Porto-riquenha", codigo: "PR", emoji: "🇵🇷" },
    { nome: "Portuguesa", codigo: "PT", emoji: "🇵🇹" },
    { nome: "Queniana", codigo: "KE", emoji: "🇰🇪" },
    { nome: "Checa", codigo: "CZ", emoji: "🇨🇿" },
    { nome: "Dominicana", codigo: "DO", emoji: "🇩🇴" },
    { nome: "Romena", codigo: "RO", emoji: "🇷🇴" },
    { nome: "Russa", codigo: "RU", emoji: "🇷🇺" },
    { nome: "Salvadorenha", codigo: "SV", emoji: "🇸🇻" },
    { nome: "Senegalesa", codigo: "SN", emoji: "🇸🇳" },
    { nome: "Síria", codigo: "SY", emoji: "🇸🇾" },
    { nome: "Sueca", codigo: "SE", emoji: "🇸🇪" },
    { nome: "Suíça", codigo: "CH", emoji: "🇨🇭" },
    { nome: "Tailandesa", codigo: "TH", emoji: "🇹🇭" },
    { nome: "Turca", codigo: "TR", emoji: "🇹🇷" },
    { nome: "Ucraniana", codigo: "UA", emoji: "🇺🇦" },
    { nome: "Uruguaia", codigo: "UY", emoji: "🇺🇾" },
    { nome: "Venezuelana", codigo: "VE", emoji: "🇻🇪" },
    { nome: "Vietnamita", codigo: "VN", emoji: "🇻🇳" }
];

/**
 * Populates a datalist element with nationality options.
 * Users can search by nationality name or country code.
 * 
 * @param {string} datalistId - The ID of the datalist element to populate
 */
function populateDatalist(datalistId) {
    const datalistElement = document.getElementById(datalistId);

    if (datalistElement) {
        datalistElement.innerHTML = '';

        nationalities.forEach(item => {
            const optionElement = document.createElement('option');
            optionElement.value = item.codigo;
            optionElement.label = `${item.nome} (${item.codigo})`;
            datalistElement.appendChild(optionElement);
        });
        
        console.log(`Populated datalist '${datalistId}' with ${nationalities.length} nationalities`);
    } else {
        console.error(`Datalist element with ID '${datalistId}' not found`);
    }
}

/**
 * Generates URL for country flag image from external service.
 * 
 * @param {string} countryCode - Two-letter country code (ISO 3166-1 alpha-2)
 * @param {number} size - Desired width of the flag image
 * @returns {string} URL for the flag image
 */
function getFlagUrl(countryCode, size = 40) {
    return `https://flagcdn.com/w${size}/${countryCode.toLowerCase()}.png`;
}

/**
 * Creates an image element for a country flag.
 * 
 * @param {string} countryCode - Two-letter country code
 * @param {number} size - Desired width of the flag image
 * @returns {HTMLImageElement} Configured img element with flag
 */
function createFlagImage(countryCode, size = 20) {
    const img = document.createElement('img');
    img.src = getFlagUrl(countryCode, size);
    img.width = size;
    img.height = size * 0.75;
    img.alt = `Flag of ${countryCode}`;
    img.style.marginRight = '8px';
    
    img.onerror = function() {
        console.log(`Failed to load flag image for country code: ${countryCode}`);
        this.style.display = 'none';
    };
    
    return img;
}

/**
 * Displays selected nationality with flag in a target element.
 * 
 * @param {string} selectedCode - The selected country code
 * @param {string} targetElementId - ID of element to display the nationality in
 */
function displaySelectedNationality(selectedCode, targetElementId) {
    const nationality = nationalities.find(n => n.codigo === selectedCode);
    const targetElement = document.getElementById(targetElementId);
    
    if (nationality && targetElement) {
        targetElement.innerHTML = '';
        
        const flagImg = createFlagImage(nationality.codigo);
        targetElement.appendChild(flagImg);
        
        const textSpan = document.createElement('span');
        textSpan.textContent = nationality.nome;
        targetElement.appendChild(textSpan);
        
        console.log(`Displayed nationality: ${nationality.nome} (${nationality.codigo})`);
    } else if (!nationality) {
        console.log(`Nationality not found for code: ${selectedCode}`);
    } else if (!targetElement) {
        console.error(`Target element '${targetElementId}' not found`);
    }
}

/**
 * Handles nationality selection from input element.
 * 
 * @param {HTMLInputElement} inputElement - The input element containing selected value
 */
function handleNationalitySelection(inputElement) {
    const selectedCode = inputElement.value;
    displaySelectedNationality(selectedCode, 'selectedNationality');
}

/**
 * Checks for and handles pre-filled nationality values.
 * Attempts multiple times to account for dynamic content loading.
 * 
 * @param {HTMLInputElement} nationalityInput - The nationality input element
 * @returns {boolean} True if pre-filled value was found and processed
 */
function checkPrefilledNationalityValue(nationalityInput) {
    if (nationalityInput.value) {
        console.log(`Found pre-filled nationality value: ${nationalityInput.value}`);
        displaySelectedNationality(nationalityInput.value, 'selectedNationality');
        return true;
    }
    return false;
}

/**
 * Sets up nationality input handling with retry logic for pre-filled values.
 * 
 * @param {HTMLInputElement} nationalityInput - The nationality input element
 */
function setupNationalityHandling(nationalityInput) {
    const handleNationalityDisplay = (code) => {
        if (code) {
            displaySelectedNationality(code, 'selectedNationality');
        }
    };
    
    nationalityInput.addEventListener('change', function() {
        handleNationalityDisplay(this.value);
    });
    
    if (!checkPrefilledNationalityValue(nationalityInput)) {
        setTimeout(() => {
            if (!checkPrefilledNationalityValue(nationalityInput)) {
                setTimeout(() => checkPrefilledNationalityValue(nationalityInput), 200);
            }
        }, 50);
    }
}

document.addEventListener('DOMContentLoaded', () => {
    populateDatalist('nationalities');
    
    const nationalityInput = document.getElementById('nationality');
    if (nationalityInput) {
        setupNationalityHandling(nationalityInput);
    } else {
        console.log('Nationality input element not found on this page');
    }
});