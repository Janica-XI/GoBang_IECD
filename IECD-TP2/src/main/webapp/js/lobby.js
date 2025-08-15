// ========================================
// LOBBY.JS - Consolidated Polling System
// ========================================

/**
 * Main lobby functionality for the GoBang web application.
 * Handles player lists, notifications, challenges, and game flow with real-time polling.
 * Follows KISS principle for maintainable and reliable operation.
 */

console.log('Lobby JavaScript loaded');

// ========================================
// Global State Management
// ========================================

let currentWaitingModal = null;
let currentChallengeModal = null;
let isSearchingPlayers = false;

// ========================================
// Application Initialization
// ========================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM loaded - initializing lobby systems');

    initializeProfileModal();
    startPlayerListPolling();
    startNotificationPolling();
});

// ========================================
// Profile Modal Management
// ========================================

/**
 * Initializes the profile modal with dynamic content loading.
 */
function initializeProfileModal() {
    const modalElement = document.getElementById('editProfileModal');
    if (!modalElement) return;

    const bootstrapModal = new bootstrap.Modal(modalElement);
    const openModalButton = document.getElementById('openProfileModalBtn');
    const modalBodyContent = document.getElementById('editProfileModal');

    if (openModalButton && modalBodyContent) {
        openModalButton.addEventListener('click', function() {
            showProfileLoadingState(modalBodyContent);
            bootstrapModal.show();
            loadProfileContent(modalBodyContent);
        });
    }
}

/**
 * Shows loading state in profile modal.
 * 
 * @param {HTMLElement} modalBody - The modal body element
 */
function showProfileLoadingState(modalBody) {
    modalBody.innerHTML = '<div class="d-flex justify-content-center"><div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div></div><p class="text-center mt-2">A carregar conteúdo...</p>';
}

/**
 * Loads profile content into the modal.
 * 
 * @param {HTMLElement} modalBody - The modal body element
 */
function loadProfileContent(modalBody) {
    fetch('profile.jsp')
        .then(function(response) {
            return response.text();
        })
        .then(function(htmlContent) {
            modalBody.innerHTML = htmlContent;
            console.log('Profile content loaded successfully');
        })
        .catch(function(error) {
            console.error('Error loading profile content:', error);
            modalBody.innerHTML = '<div class="alert alert-danger">Erro ao carregar conteúdo</div>';
        });
}

// ========================================
// Ready State Management
// ========================================

/**
 * Toggles the user's ready state for matchmaking.
 */
function toggleReady() {
    const readyButton = document.getElementById('readyBtn');
    if (!readyButton) return;

    const currentlyReady = readyButton.classList.contains('btn-success');
    const newReadyState = !currentlyReady;

    updateReadyButtonDisplay(newReadyState);
    sendReadyStateToServer(newReadyState, currentlyReady);
}

/**
 * Updates the ready button display.
 * 
 * @param {boolean} isReady - Whether the user is ready
 */
function updateReadyButtonDisplay(isReady) {
    const readyButton = document.getElementById('readyBtn');
    if (!readyButton) return;

    if (isReady) {
        readyButton.className = 'btn btn-success me-2';
        readyButton.innerHTML = '<i class="fas fa-check"></i> Pronto!';
    } else {
        readyButton.className = 'btn btn-outline-success me-2';
        readyButton.innerHTML = '<i class="fas fa-play"></i> Ficar Pronto';
    }
}

/**
 * Sends ready state change to server.
 * 
 * @param {boolean} newReadyState - The new ready state
 * @param {boolean} previousState - The previous ready state for rollback
 */
function sendReadyStateToServer(newReadyState, previousState) {
    fetch('lobby', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=ready&ready=' + newReadyState
    })
        .then(function(response) {
            if (!response.ok) {
                updateReadyButtonDisplay(previousState);
                showAlert('Erro ao alterar estado', 'error');
            }
        })
        .catch(function(error) {
            console.error('Error updating ready state:', error);
            updateReadyButtonDisplay(previousState);
            showAlert('Erro de conexão', 'error');
        });
}

// ========================================
// Player List Management and Polling
// ========================================

/**
 * Starts the player list polling system.
 */
function startPlayerListPolling() {
    console.log('Starting player list polling system');

    updatePlayerListFromServer();

    setInterval(function() {
        if (!isSearchingPlayers) {
            updatePlayerListFromServer();
        }
    }, 3000);
}

/**
 * Fetches and updates the player list from the server.
 */
function updatePlayerListFromServer() {
    fetch('/IECD-TP2/api/lobby?action=playerlist')
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.players) {
                updatePlayerListDisplay(data.players);
                console.log(`Player list updated: ${data.players.length} players online`);
            }
        })
        .catch(function(error) {
            console.log('Error in player list polling:', error);
        });
}

/**
 * Updates the player list display in the UI.
 * 
 * @param {Array} players - Array of player objects from server
 */
function updatePlayerListDisplay(players) {
    const playersContainer = document.getElementById('playersContainer');
    if (!playersContainer) return;

    console.log('Players received from server:', players.length);

    playersContainer.innerHTML = '';

	if (players.length === 0) {
	    showNoPlayersMessage(playersContainer);
	    updateOnlinePlayerCount(0);
	    return;
	}

	players.forEach(function(player) {
	    const playerCard = createPlayerCard(player);
	    playersContainer.insertAdjacentHTML('beforeend', playerCard);
	});

	updateOnlinePlayerCount(players.length);
}

/**
 * Shows message when no other players are online.
 * 
 * @param {HTMLElement} container - The players container element
 */
function showNoPlayersMessage(container) {
    container.innerHTML = '<div class="col-12 text-center"><p class="text-muted">Nenhum jogador online no momento.</p><button onclick="refreshPlayerList()" class="btn btn-outline-secondary btn-sm"><i class="fas fa-refresh"></i> Atualizar</button></div>';
}

/**
 * Creates HTML for a player card.
 * 
 * @param {Object} player - Player object with username, victories, defeats, photoBase64
 * @returns {string} HTML string for the player card
 */
function createPlayerCard(player) {
    const playerImageSource = getPlayerImageSource(player.photoBase64);

    return '<div class="col-md-6 col-lg-4 mb-3">' +
        '<div class="d-flex align-items-center p-2 border rounded hover-shadow player-card" ' +
        'onclick="challengePlayer(\'' + player.username + '\')" style="cursor: pointer;">' +
        '<img src="' + playerImageSource + '" alt="Player" class="rounded-circle me-3" style="width: 50px; height: 50px; object-fit: cover;">' +
        '<div class="flex-grow-1">' +
        '<h6 class="mb-1">' + player.username + '</h6>' +
        '<small class="text-success"><i class="fas fa-circle"></i> Online</small><br>' +
        '<small class="text-muted">V: ' + (player.victories || 0) + ', D: ' + (player.defeats || 0) + '</small>' +
        '</div>' +
        '</div>' +
        '</div>';
}

/**
 * Gets the appropriate image source for a player.
 * 
 * @param {string} photoBase64 - Base64 encoded photo data
 * @returns {string} Image source URL or data URI
 */
function getPlayerImageSource(photoBase64) {
    if (photoBase64 && photoBase64.length > 0) {
        return 'data:image/jpeg;base64,' + photoBase64;
    }
    return 'images/default.jpg';
}

/**
 * Updates the online player count badge.
 * 
 * @param {number} count - Number of online players
 */
function updateOnlinePlayerCount(count) {
	const playerCountBadge = document.getElementById('playerCountBadge');
    if (playerCountBadge) {
        playerCountBadge.textContent = count + ' Online';
    }
}

/**
 * Gets the current user's username from the page context.
 * 
 * @returns {string|null} The current username or null if not found
 */
function getCurrentUsername() {
    if (window.getCurrentUsername) {
        return window.getCurrentUsername;
    }

    const welcomeTextElement = document.querySelector('h4.text-primary');
    if (welcomeTextElement && welcomeTextElement.textContent.includes('Bem-vindo ')) {
        return welcomeTextElement.textContent.replace('Bem-vindo ', '').trim();
    }

    return null;
}

/**
 * Refreshes the player list manually.
 */
function refreshPlayerList() {
    if (!isSearchingPlayers) {
        updatePlayerListFromServer();
    }
}

// ========================================
// Player Search Functionality
// ========================================

/**
 * Filters players based on search input.
 */
function filterPlayers() {
    const searchInput = document.getElementById('playerSearchInput');
    const playersContainer = document.getElementById('playersContainer');
    const noResultsMessage = document.getElementById('noSearchResults');
    
    if (!searchInput || !playersContainer) return;

    const searchTerm = searchInput.value.toLowerCase().trim();
    isSearchingPlayers = searchTerm !== '';

    console.log(`Filtering players with search term: '${searchTerm}'`);

    if (searchTerm === '') {
        clearPlayerSearch(noResultsMessage);
        return;
    }

    performPlayerSearch(searchTerm, playersContainer, noResultsMessage);
}

/**
 * Clears player search and shows full list.
 * 
 * @param {HTMLElement} noResultsMessage - No results message element
 */
function clearPlayerSearch(noResultsMessage) {
    console.log('Clearing search - showing full player list');
    updatePlayerListFromServer();
    if (noResultsMessage) {
        noResultsMessage.style.display = 'none';
    }
}

/**
 * Performs player search with the given term.
 * 
 * @param {string} searchTerm - The search term
 * @param {HTMLElement} container - Players container element
 * @param {HTMLElement} noResultsMessage - No results message element
 */
function performPlayerSearch(searchTerm, container, noResultsMessage) {
    fetch('/IECD-TP2/api/lobby?action=playerlist')
        .then(function(response) {
            return response.json();
        })
        .then(function(data) {
            if (data.players) {
                const filteredPlayers = data.players.filter(function(player) {
                    return player.username.toLowerCase().includes(searchTerm);
                });

                displaySearchResults(filteredPlayers, container, noResultsMessage);
            }
        })
        .catch(function(error) {
            console.log('Error during player search:', error);
        });
}

/**
 * Displays search results in the container.
 * 
 * @param {Array} filteredPlayers - Filtered player list
 * @param {HTMLElement} container - Players container element
 * @param {HTMLElement} noResultsMessage - No results message element
 */
function displaySearchResults(filteredPlayers, container, noResultsMessage) {
    console.log(`Search results: ${filteredPlayers.length} players found`);

    container.innerHTML = '';

    if (filteredPlayers.length === 0) {
        if (noResultsMessage) {
            noResultsMessage.style.display = 'block';
        }
    } else {
        if (noResultsMessage) {
            noResultsMessage.style.display = 'none';
        }

        filteredPlayers.forEach(function(player) {
            const playerCard = createPlayerCard(player);
            container.insertAdjacentHTML('beforeend', playerCard);
        });
    }

    updateOnlinePlayerCount(filteredPlayers.length);
}

// ========================================
// Profile Statistics Updates
// ========================================

/**
 * Updates the user's own statistics display.
 * 
 * @param {number} victories - Number of victories
 * @param {number} defeats - Number of defeats
 * @param {string} totalTime - Formatted total play time
 */
function updateOwnStatistics(victories, defeats, totalTime) {
    const statisticsElements = document.querySelectorAll('small.text-light');
    statisticsElements.forEach(function(element) {
        if (element.textContent.includes('Vitórias:')) {
            element.textContent = 'Vitórias: ' + victories + ' | Derrotas: ' + defeats;
        } else if (element.textContent.includes('Tempo Total:')) {
            element.textContent = 'Tempo Total: ' + totalTime;
        }
    });
}

// ========================================
// Challenge System Flow
// ========================================

/**
 * Initiates a challenge to another player.
 * 
 * @param {string} username - Username of the player to challenge
 */
function challengePlayer(username) {
    showChallengeConfirmationModal(username);
}

/**
 * Shows confirmation modal for sending a challenge.
 * 
 * @param {string} username - Username of the player to challenge
 */
function showChallengeConfirmationModal(username) {
    const confirmationModalHtml = createChallengeConfirmationModal(username);

    document.body.insertAdjacentHTML('beforeend', confirmationModalHtml);
    const modal = new bootstrap.Modal(document.getElementById('confirmChallengeModal'));

    document.getElementById('confirmChallengeModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });

    modal.show();
}

/**
 * Creates HTML for challenge confirmation modal.
 * 
 * @param {string} username - Username of the player to challenge
 * @returns {string} HTML string for the modal
 */
function createChallengeConfirmationModal(username) {
    return '<div class="modal fade" id="confirmChallengeModal" tabindex="-1">' +
        '<div class="modal-dialog">' +
        '<div class="modal-content">' +
        '<div class="modal-header">' +
        '<h5 class="modal-title">Enviar Desafio</h5>' +
        '<button type="button" class="btn-close" data-bs-dismiss="modal"></button>' +
        '</div>' +
        '<div class="modal-body">' +
        '<p>Deseja desafiar <strong>' + username + '</strong> para uma partida?</p>' +
        '</div>' +
        '<div class="modal-footer">' +
        '<button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>' +
        '<button type="button" class="btn btn-primary" onclick="sendChallenge(\'' + username + '\')">Enviar Desafio</button>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>';
}

/**
 * Sends a challenge to the specified player.
 * 
 * @param {string} username - Username of the player to challenge
 */
function sendChallenge(username) {
    const confirmationModal = bootstrap.Modal.getInstance(document.getElementById('confirmChallengeModal'));
    confirmationModal.hide();

    fetch('lobby', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=challenge&opponent=' + encodeURIComponent(username)
    })
        .then(function(response) {
            if (response.ok) {
                showWaitingForChallengeResponse(username);
            } else {
                showAlert('Erro ao enviar desafio', 'error');
            }
        })
        .catch(function(error) {
            console.error('Error sending challenge:', error);
            showAlert('Erro de conexão ao enviar desafio', 'error');
        });
}

/**
 * Shows waiting modal while challenge response is pending.
 * 
 * @param {string} opponentUsername - Username of the challenged player
 */
function showWaitingForChallengeResponse(opponentUsername) {
    const waitingModalHtml = createWaitingForResponseModal(opponentUsername);

    document.body.insertAdjacentHTML('beforeend', waitingModalHtml);
    currentWaitingModal = new bootstrap.Modal(document.getElementById('waitingModal'));
    currentWaitingModal.show();
}

/**
 * Creates HTML for waiting for response modal.
 * 
 * @param {string} opponentUsername - Username of the challenged player
 * @returns {string} HTML string for the modal
 */
function createWaitingForResponseModal(opponentUsername) {
    return '<div class="modal fade" id="waitingModal" tabindex="-1" data-bs-backdrop="static">' +
        '<div class="modal-dialog">' +
        '<div class="modal-content">' +
        '<div class="modal-header">' +
        '<h5 class="modal-title"><i class="fas fa-clock text-warning"></i> Aguardando resposta...</h5>' +
        '</div>' +
        '<div class="modal-body text-center">' +
        '<div class="spinner-border text-primary mb-3" role="status"><span class="visually-hidden">Loading...</span></div>' +
        '<p>A aguardar resposta de <strong>' + opponentUsername + '</strong>...</p>' +
        '<small class="text-muted">O jogador tem até 30 segundos para responder</small>' +
        '</div>' +
        '<div class="modal-footer">' +
        '<button type="button" class="btn btn-outline-secondary" onclick="cancelWaitingChallenge()"><i class="fas fa-times"></i> Cancelar Desafio</button>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>';
}

/**
 * Cancels a waiting challenge and notifies the server.
 */
function cancelWaitingChallenge() {
    hideWaitingModal();

    fetch('lobby', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=cancelChallenge'
    })
        .catch(function(error) {
            console.error('Error canceling challenge:', error);
        });
}

/**
 * Hides and removes the waiting modal.
 */
function hideWaitingModal() {
    if (currentWaitingModal) {
        currentWaitingModal.hide();
        const modalElement = document.getElementById('waitingModal');
        if (modalElement) {
            modalElement.remove();
        }
        currentWaitingModal = null;
    }
}

// ========================================
// Notification Polling System
// ========================================

/**
 * Starts the notification polling system for real-time events.
 */
function startNotificationPolling() {
    console.log('Starting notification polling system');

    setInterval(checkForNotifications, 1000);
}

/**
 * Checks for new notifications from the server.
 */
function checkForNotifications() {
    fetch('/IECD-TP2/api/lobby?action=notifications')
        .then(function(response) {
            return response.json();
        })
        .then(function(notificationData) {
            handleIncomingNotification(notificationData);
        })
        .catch(function(error) {
            console.log('Error in notification polling:', error);
        });
}

/**
 * Handles incoming notifications from the server.
 * 
 * @param {Object} notificationData - Notification data from server
 */
function handleIncomingNotification(notificationData) {
    console.log('Notification received:', notificationData);

    switch (notificationData.type) {
        case 'challenge':
            hideWaitingModal();
            showIncomingChallengeNotification(notificationData.username, notificationData.photoBase64);
            break;
        case 'gameStart':
            hideWaitingModal();
            hideChallengeModal();
            showGameStartModal(notificationData.gameId, notificationData.blackPlayer, notificationData.whitePlayer);
            break;
        case 'challengeReply':
            hideWaitingModal();
            hideChallengeModal();
            handleChallengeReplyNotification(notificationData.status);
            break;
        case 'profileUpdate':
            updateOwnStatistics(notificationData.victories, notificationData.defeats, notificationData.totalTime);
            showAlert('Estatísticas atualizadas!', 'success');
            console.log('Profile automatically updated:', notificationData);
            break;
    }
}

/**
 * Handles challenge reply notifications.
 * 
 * @param {string} replyStatus - Status of the challenge reply
 */
function handleChallengeReplyNotification(replyStatus) {
    switch (replyStatus) {
        case 'Rejected':
            showAlert('Desafio rejeitado pelo jogador', 'info');
            break;
        case 'Canceled':
            showAlert('O outro jogador cancelou o desafio', 'info');
            break;
        case 'Accepted':
            showAlert('Desafio aceite! A iniciar jogo...', 'success');
            break;
    }
}

/**
 * Shows incoming challenge notification modal.
 * 
 * @param {string} challengerName - Name of the challenging player
 * @param {string} photoBase64 - Base64 photo of the challenger
 */
function showIncomingChallengeNotification(challengerName, photoBase64) {
    const challengerImageHtml = getChallengerImageHtml(photoBase64);
    const challengeModalHtml = createChallengeNotificationModal(challengerName, challengerImageHtml);

    document.body.insertAdjacentHTML('beforeend', challengeModalHtml);
    currentChallengeModal = new bootstrap.Modal(document.getElementById('challengeNotificationModal'));
    currentChallengeModal.show();
}

/**
 * Gets HTML for challenger image.
 * 
 * @param {string} photoBase64 - Base64 photo data
 * @returns {string} HTML string for the image
 */
function getChallengerImageHtml(photoBase64) {
    const imageSource = getPlayerImageSource(photoBase64);
    return '<img src="' + imageSource + '" alt="Challenger" class="rounded-circle mb-3" style="width: 80px; height: 80px; object-fit: cover;">';
}

/**
 * Creates HTML for challenge notification modal.
 * 
 * @param {string} challengerName - Name of the challenger
 * @param {string} challengerImageHtml - HTML for challenger image
 * @returns {string} HTML string for the modal
 */
function createChallengeNotificationModal(challengerName, challengerImageHtml) {
    return '<div class="modal fade" id="challengeNotificationModal" tabindex="-1" data-bs-backdrop="static">' +
        '<div class="modal-dialog">' +
        '<div class="modal-content border-warning">' +
        '<div class="modal-header bg-warning text-dark">' +
        '<h5 class="modal-title"><i class="fas fa-gamepad"></i> Convite para Jogar</h5>' +
        '</div>' +
        '<div class="modal-body text-center">' +
        challengerImageHtml +
        '<h6><strong>' + challengerName + '</strong> quer jogar contigo!</h6>' +
        '<p class="text-muted">Aceitas o desafio?</p>' +
        '</div>' +
        '<div class="modal-footer justify-content-center">' +
        '<button type="button" class="btn btn-success" onclick="respondToChallenge(\'accept\')"><i class="fas fa-check"></i> Aceitar</button>' +
        '<button type="button" class="btn btn-danger" onclick="respondToChallenge(\'reject\')"><i class="fas fa-times"></i> Rejeitar</button>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>';
}

/**
 * Responds to an incoming challenge.
 * 
 * @param {string} response - Response to the challenge ('accept' or 'reject')
 */
function respondToChallenge(response) {
    hideChallengeModal();

    fetch('/IECD-TP2/api/lobby', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=challenge-response&response=' + response
    })
        .then(function() {
            if (response === 'accept') {
                showAlert('Desafio aceite! A iniciar jogo...', 'success');
            } else {
                showAlert('Desafio rejeitado', 'info');
            }
        })
        .catch(function(error) {
            console.error('Error responding to challenge:', error);
            showAlert('Erro ao responder ao desafio', 'error');
        });
}

/**
 * Hides and removes the challenge modal.
 */
function hideChallengeModal() {
    if (currentChallengeModal) {
        currentChallengeModal.hide();
        const modalElement = document.getElementById('challengeNotificationModal');
        if (modalElement) {
            modalElement.remove();
        }
        currentChallengeModal = null;
    }
}

// ========================================
// Game Flow Management
// ========================================

/**
 * Shows the game start modal with player information and navigation options.
 * 
 * @param {string} gameId - Unique game identifier
 * @param {string} blackPlayerName - Username of the black player
 * @param {string} whitePlayerName - Username of the white player
 */
function showGameStartModal(gameId, blackPlayerName, whitePlayerName) {
    const currentUsername = getCurrentUsername();
    const userColor = currentUsername === blackPlayerName ? 'Azuis' : 'Vermelhas';
    const userPieceClass = currentUsername === blackPlayerName ? 'piece-black' : 'piece-white';

    const gameStartModalHtml = createGameStartModal(gameId, blackPlayerName, whitePlayerName, currentUsername, userColor, userPieceClass);

    document.body.insertAdjacentHTML('beforeend', gameStartModalHtml);

    const gameStartModal = new bootstrap.Modal(document.getElementById('gameStartModal'));
    gameStartModal.show();

    document.getElementById('gameStartModal').addEventListener('hidden.bs.modal', function() {
        this.remove();
    });
}

/**
 * Creates HTML for the game start modal.
 * 
 * @param {string} gameId - Game identifier
 * @param {string} blackPlayerName - Black player username
 * @param {string} whitePlayerName - White player username
 * @param {string} currentUsername - Current user's username
 * @param {string} userColor - User's piece color in Portuguese
 * @param {string} userPieceClass - CSS class for user's pieces
 * @returns {string} HTML string for the modal
 */
function createGameStartModal(gameId, blackPlayerName, whitePlayerName, currentUsername, userColor, userPieceClass) {
    return `
        <div class="modal fade modern-modal" id="gameStartModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header bg-success text-white text-center">
                        <div class="w-100">
                            <h4 class="modal-title mb-0">
                                <i class="fas fa-gamepad me-2"></i>
                                Jogo Iniciado!
                            </h4>
                        </div>
                    </div>
                    <div class="modal-body text-center py-4">
                        <div class="card mb-4">
                            <div class="card-body">
                                <h5 class="card-title text-primary mb-3">
                                    <i class="fas fa-chess-board me-2"></i>
                                    Jogo: ${gameId.substring(0, 8)}...
                                </h5>
                                
                                <div class="row align-items-center mb-3">
                                    <div class="col-5 text-end">
                                        <div class="p-3 rounded ${currentUsername === blackPlayerName ? 'bg-info bg-opacity-25 border border-info' : 'bg-secondary bg-opacity-10'}">
                                            <h6 class="mb-1">
                                                <i class="fas fa-circle piece-black me-1"></i>
                                                ${blackPlayerName}
                                            </h6>
                                            <small class="text-muted">Peças Azuis</small>
                                            ${currentUsername === blackPlayerName ? '<br><span class="badge bg-info text-dark mt-1">TU</span>' : ''}
                                        </div>
                                    </div>
                                    
                                    <div class="col-2">
                                        <h3 class="text-muted mb-0">VS</h3>
                                    </div>
                                    
                                    <div class="col-5 text-start">
                                        <div class="p-3 rounded ${currentUsername === whitePlayerName ? 'bg-danger bg-opacity-25 border border-danger' : 'bg-secondary bg-opacity-10'}">
                                            <h6 class="mb-1">
                                                <i class="fas fa-circle piece-white me-1"></i>
                                                ${whitePlayerName}
                                            </h6>
                                            <small class="text-muted">Peças Vermelhas</small>
                                            ${currentUsername === whitePlayerName ? '<br><span class="badge bg-danger text-white mt-1">TU</span>' : ''}
                                        </div>
                                    </div>
                                </div>
                                
                                <div class="alert alert-info d-inline-block">
                                    <i class="fas fa-user me-2"></i>
                                    Tu jogas como <strong class="${userPieceClass}">${userColor}</strong>
                                    ${currentUsername === blackPlayerName ? '<br><small class="text-muted">(Começam primeiro!)</small>' : ''}
                                </div>
                            </div>
                        </div>
                        
                        <div class="mb-4">
                            <h6 class="text-muted mb-2">Como queres continuar?</h6>
                            <p class="text-muted small">
                                Podes jogar na mesma aba ou abrir numa nova aba para manter o lobby aberto.
                            </p>
                        </div>
                    </div>
                    
                    <div class="modal-footer justify-content-center">
                        <button type="button" class="btn btn-outline-secondary me-2" onclick="stayInLobby()">
                            <i class="fas fa-times me-2"></i>
                            Ficar no Lobby
                        </button>
                        <button type="button" class="btn btn-primary me-2" onclick="playInSameTab('${gameId}', '${blackPlayerName}', '${whitePlayerName}')">
                            <i class="fas fa-play me-2"></i>
                            Jogar Aqui
                        </button>
                        <button type="button" class="btn btn-success" onclick="playInNewTab('${gameId}', '${blackPlayerName}', '${whitePlayerName}')">
                            <i class="fas fa-external-link-alt me-2"></i>
                            Nova Aba
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
}

/**
 * Handles staying in the lobby without joining the game.
 */
function stayInLobby() {
    const gameStartModal = bootstrap.Modal.getInstance(document.getElementById('gameStartModal'));
    gameStartModal.hide();

    showAlert('Ficaste no lobby. O jogo pode ser acedido mais tarde.', 'info');
}

/**
 * Navigates to the game in the same tab.
 * 
 * @param {string} gameId - Game identifier
 * @param {string} blackPlayerName - Black player username
 * @param {string} whitePlayerName - White player username
 */
function playInSameTab(gameId, blackPlayerName, whitePlayerName) {
    const gameStartModal = bootstrap.Modal.getInstance(document.getElementById('gameStartModal'));
    gameStartModal.hide();

    const gameUrl = buildGameUrl(gameId, blackPlayerName, whitePlayerName);
    window.location.href = gameUrl;
}

/**
 * Opens the game in a new tab.
 * 
 * @param {string} gameId - Game identifier
 * @param {string} blackPlayerName - Black player username
 * @param {string} whitePlayerName - White player username
 */
function playInNewTab(gameId, blackPlayerName, whitePlayerName) {
    const gameStartModal = bootstrap.Modal.getInstance(document.getElementById('gameStartModal'));
    gameStartModal.hide();

    const gameUrl = buildGameUrl(gameId, blackPlayerName, whitePlayerName);
    const gameTab = window.open(gameUrl, '_blank');

    if (!gameTab || gameTab.closed || typeof gameTab.closed === 'undefined') {
        handlePopupBlocked(gameId, blackPlayerName, whitePlayerName);
    } else {
        showAlert('Jogo aberto numa nova aba!', 'success');
    }
}

/**
 * Builds the game URL with parameters.
 * 
 * @param {string} gameId - Game identifier
 * @param {string} blackPlayerName - Black player username
 * @param {string} whitePlayerName - White player username
 * @returns {string} Complete game URL
 */
function buildGameUrl(gameId, blackPlayerName, whitePlayerName) {
    return 'game?gameId=' + encodeURIComponent(gameId) +
           '&blackPlayer=' + encodeURIComponent(blackPlayerName) +
           '&whitePlayer=' + encodeURIComponent(whitePlayerName);
}

/**
 * Handles popup blocking by falling back to same tab navigation.
 * 
 * @param {string} gameId - Game identifier
 * @param {string} blackPlayerName - Black player username
 * @param {string} whitePlayerName - White player username
 */
function handlePopupBlocked(gameId, blackPlayerName, whitePlayerName) {
    showAlert('Popup bloqueado! A abrir na mesma aba...', 'warning');
    setTimeout(() => playInSameTab(gameId, blackPlayerName, whitePlayerName), 1500);
}

// ========================================
// Logout Management
// ========================================

/**
 * Performs user logout with server communication.
 */
function performLogout() {
    console.log('Initiating logout process');
    
    fetch('/IECD-TP2/api/lobby', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=logout'
    })
    .then(function(response) {
        return response.json();
    })
    .then(function(logoutData) {
        handleLogoutResponse(logoutData);
    })
    .catch(function(error) {
        handleLogoutError(error);
    });
}

/**
 * Handles logout response from server.
 * 
 * @param {Object} logoutData - Response data from logout request
 */
function handleLogoutResponse(logoutData) {
    if (logoutData.status === 'success') {
        showAlert('Logout bem-sucedido!', 'success');
        setTimeout(function() {
            window.location.href = 'login';
        }, 1000);
    } else {
        showAlert('Erro no logout: ' + (logoutData.message || logoutData.error), 'error');
    }
}

/**
 * Handles logout errors with fallback navigation.
 * 
 * @param {Error} error - Error that occurred during logout
 */
function handleLogoutError(error) {
    console.error('Error during logout:', error);
    showAlert('Erro de conexão durante logout', 'error');
    setTimeout(function() {
        window.location.href = 'login';
    }, 2000);
}

// ========================================
// Utility Functions
// ========================================

/**
 * Shows a temporary alert message to the user.
 * 
 * @param {string} message - Message to display
 * @param {string} type - Alert type ('info', 'success', 'warning', 'error')
 */
function showAlert(message, type) {
    if (!type) type = 'info';

    const alertClass = getAlertClass(type);
    const alertHtml = createAlertHtml(message, alertClass);

    document.body.insertAdjacentHTML('beforeend', alertHtml);

    setTimeout(function() {
        removeAllAlerts();
    }, 5000);
}

/**
 * Gets the appropriate Bootstrap alert class for the given type.
 * 
 * @param {string} type - Alert type
 * @returns {string} Bootstrap alert class
 */
function getAlertClass(type) {
    switch (type) {
        case 'error': return 'alert-danger';
        case 'success': return 'alert-success';
        case 'warning': return 'alert-warning';
        default: return 'alert-info';
    }
}

/**
 * Creates HTML for an alert message.
 * 
 * @param {string} message - Alert message
 * @param {string} alertClass - Bootstrap alert class
 * @returns {string} HTML string for the alert
 */
function createAlertHtml(message, alertClass) {
    return '<div class="alert ' + alertClass + ' alert-dismissible fade show position-fixed" style="top: 20px; right: 20px; z-index: 9999; min-width: 300px;" role="alert">' +
        message +
        '<button type="button" class="btn-close" data-bs-dismiss="alert"></button>' +
        '</div>';
}

/**
 * Removes all visible alert messages.
 */
function removeAllAlerts() {
    const visibleAlerts = document.querySelectorAll('.alert.show');
    visibleAlerts.forEach(function(alert) {
        alert.remove();
    });
}

// ========================================
// Global Function Exports
// ========================================

/**
 * Export functions to global scope for onclick handlers and external access.
 */
window.toggleReady = toggleReady;
window.challengePlayer = challengePlayer;
window.sendChallenge = sendChallenge;
window.cancelWaitingChallenge = cancelWaitingChallenge;
window.respondToChallenge = respondToChallenge;
window.refreshPlayerList = refreshPlayerList;
window.filterPlayers = filterPlayers;
window.stayInLobby = stayInLobby;
window.playInSameTab = playInSameTab;
window.playInNewTab = playInNewTab;
window.performLogout = performLogout;