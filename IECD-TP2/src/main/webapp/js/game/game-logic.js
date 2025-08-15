// ========================================
// GAME LOGIC - Core Game Management
// ========================================

console.log('Game Logic loaded - 2025 version');

// ========================================
// Configuration and Constants
// ========================================

const BOARD_SIZE = 15;
const GRID_SQUARES = 14;
const CELL_STATE = {
    FREE: 0,
    BLACK: 1,
    WHITE: 2
};

// ========================================
// Game State Variables
// ========================================

let gameBoard = Array(BOARD_SIZE).fill().map(() => Array(BOARD_SIZE).fill(CELL_STATE.FREE));
let currentTurn = 'Black';
let myTurn = false;
let gameActive = true;
let gameStartTime = Date.now();
let gameConfig = null;

// Last move tracking for UI highlighting
let lastBlackMovePosition = null;
let lastWhiteMovePosition = null;

// Timer system
let blackTimeRemaining = 5 * 60 * 1000;
let whiteTimeRemaining = 5 * 60 * 1000;
let serverTimestamp = Date.now();
let turnStartedLocally = Date.now();

// Server communication
let lastGameStateHash = null;
let waitingForGameState = false;

// Intervals
let timerInterval = null;
let localTimerInterval = null;

// ========================================
// Initialization
// ========================================

/**
 * Initializes the game logic system.
 * * @returns {boolean} True if initialization succeeded, false otherwise
 */
function initializeGame() {
    console.log('Initializing game logic');
    
    gameConfig = window.gameConfig;
    if (!gameConfig) {
        console.error('Game config not found');
        return false;
    }
    
    console.log('Game config loaded:', gameConfig);
    
    setupGameTimers();
    setupInitialTurnState();
    startServerCommunication();
    startTimerSystems();
    
    console.log('Game logic initialized - myTurn:', myTurn, 'myColor:', gameConfig.myColor);
    return true;
}

/**
 * Sets up game timers based on server timestamp or current time.
 */
function setupGameTimers() {
    if (gameConfig.gameStartTimestamp) {
        gameStartTime = gameConfig.gameStartTimestamp;
        
        const timeElapsed = Date.now() - gameStartTime;
        blackTimeRemaining = Math.max(0, 5 * 60 * 1000 - timeElapsed);
        whiteTimeRemaining = 5 * 60 * 1000;
        
        console.log(`Game started ${timeElapsed}ms ago`);
        console.log(`Initial times - Black: ${formatTime(blackTimeRemaining)}, White: ${formatTime(whiteTimeRemaining)}`);
    } else {
        gameStartTime = Date.now();
        blackTimeRemaining = 5 * 60 * 1000;
        whiteTimeRemaining = 5 * 60 * 1000;
        console.log('Using local time as fallback');
    }
    
    turnStartedLocally = Date.now();
}

/**
 * Sets up the initial turn state based on game configuration.
 */
function setupInitialTurnState() {
    myTurn = gameConfig.isBlackPlayer;
    currentTurn = 'Black';
}

// ========================================
// Game State Management
// ========================================

/**
 * Updates the complete game state based on server response.
 * * @param {Object} gameState - Game state received from server
 */
function updateGameState(gameState) {
    console.log('Updating game state:', gameState);
    
    const stateHash = gameState.gameId + '_' + gameState.nextPlayerColor + '_' + gameState.boardRows.join('');
    if (stateHash === lastGameStateHash) {
        console.log('Same game state, ignoring update');
        return;
    }
    
    if (waitingForGameState) {
        console.log('New GameState received after move');
        waitingForGameState = false;
    }
    
    lastGameStateHash = stateHash;
    
    updateBoardState(gameState);
    updateTurnState(gameState);
    updateTimerState(gameState);
    updateUI();
}

/**
 * Updates the board state from server data and detects new moves.
 * * @param {Object} gameState - Server game state
 */
function updateBoardState(gameState) {
    if (!gameState.boardRows || gameState.boardRows.length !== BOARD_SIZE) {
        console.log('Invalid board data in game state');
        return;
    }
    
    const previousBoard = gameBoard.map(row => [...row]);
    
    for (let row = 0; row < BOARD_SIZE; row++) {
        const rowStr = gameState.boardRows[row];
        for (let col = 0; col < BOARD_SIZE; col++) {
            const char = rowStr.charAt(col);
            let newState = CELL_STATE.FREE;
            
            if (char === 'B') newState = CELL_STATE.BLACK;
            else if (char === 'W') newState = CELL_STATE.WHITE;
            
            if (previousBoard[row][col] === CELL_STATE.FREE && newState !== CELL_STATE.FREE) {
                createOpponentMoveParticles(row, col, newState);
                updateLastMoveTracking(row, col, newState);
                console.log(`New piece detected at [${row}, ${col}], state: ${newState}`);
            }
            
            gameBoard[row][col] = newState;
        }
    }
}

/**
 * Updates last move tracking for visual highlighting.
 * * @param {number} row - Board row
 * @param {number} col - Board column
 * @param {number} pieceState - Piece state (BLACK or WHITE)
 */
function updateLastMoveTracking(row, col, pieceState) {
    if (pieceState === CELL_STATE.BLACK) {
        lastBlackMovePosition = { row, col };
    } else if (pieceState === CELL_STATE.WHITE) {
        lastWhiteMovePosition = { row, col };
    }
}

/**
 * Creates particle effects for opponent moves.
 * * @param {number} row - Board row
 * @param {number} col - Board column
 * @param {number} pieceState - Piece state (BLACK or WHITE)
 */
function createOpponentMoveParticles(row, col, pieceState) {
    if (waitingForGameState) return;
    
    if (window.createParticleEffect) {
        const BOARD_PADDING = 30;
        const BOARD_CONTENT_WIDTH = 570;
        const CELL_DIMENSION = BOARD_CONTENT_WIDTH / 14;
        
        const centerX = BOARD_PADDING + col * CELL_DIMENSION;
        const centerY = BOARD_PADDING + row * CELL_DIMENSION;
        
        const isBlackPiece = pieceState === CELL_STATE.BLACK;
        const effectColor = isBlackPiece ? '#00f5ff' : '#ff6b6b';
        
        window.createParticleEffect(centerX, centerY, effectColor);
        console.log(`Created opponent move particles at [${row}, ${col}]`);
    }
}

/**
 * Updates the current turn based on server data.
 * @param {object} gameState - The game state object from the server.
 */
function updateTurnState(gameState) {
    if (!gameState.nextPlayerColor) return;
    
    const oldTurn = currentTurn;
    currentTurn = gameState.nextPlayerColor;
    
    myTurn = (currentTurn === 'Black' && gameConfig.myColor === 'Black') ||
             (currentTurn === 'White' && gameConfig.myColor === 'White');
    
    if (oldTurn !== currentTurn) {
        turnStartedLocally = Date.now();
        console.log(`Turn changed from ${oldTurn} to ${currentTurn}`);
    } else if (gameState.timestamp) {
        adjustTurnStartTime(gameState.timestamp);
    }
    
    console.log(`Turn update - OLD=${oldTurn}, NEW=${currentTurn}, myColor=${gameConfig.myColor}, myTurn=${myTurn}`);
}

/**
 * Adjusts the local turn start time based on the server's timestamp to maintain sync.
 * @param {string} serverTimestampString - The ISO 8601 timestamp string from the server.
 */
function adjustTurnStartTime(serverTimestampString) {
    const serverTime = new Date(serverTimestampString).getTime();
    const currentTime = Date.now();
    const timeSinceServerUpdate = currentTime - serverTime;
    
    turnStartedLocally = currentTime - timeSinceServerUpdate;
    console.log(`Adjusted turn start time by ${timeSinceServerUpdate}ms`);
}

/**
 * Updates the remaining time for both players from the server's game state.
 * @param {object} gameState - The game state object from the server.
 */
function updateTimerState(gameState) {
    if (gameState.blackTimeRemaining !== undefined) {
        blackTimeRemaining = gameState.blackTimeRemaining;
    }
    if (gameState.whiteTimeRemaining !== undefined) {
        whiteTimeRemaining = gameState.whiteTimeRemaining;
    }
    
    if (gameState.timestamp) {
        adjustTimersForNetworkDelay(gameState);
        serverTimestamp = new Date(gameState.timestamp).getTime();
    }
}

/**
 * Adjusts player timers to account for the network delay between the server and client.
 * @param {object} gameState - The game state object from the server, containing the timestamp.
 */
function adjustTimersForNetworkDelay(gameState) {
    const serverTime = new Date(gameState.timestamp).getTime();
    const currentTime = Date.now();
    const timeSinceServerTimestamp = currentTime - serverTime;
    
    if (timeSinceServerTimestamp > 1000) {
        if (gameState.nextPlayerColor === 'Black') {
            blackTimeRemaining = Math.max(0, blackTimeRemaining - timeSinceServerTimestamp);
            console.log(`Adjusted Black time by ${timeSinceServerTimestamp}ms`);
        } else {
            whiteTimeRemaining = Math.max(0, whiteTimeRemaining - timeSinceServerTimestamp);
            console.log(`Adjusted White time by ${timeSinceServerTimestamp}ms`);
        }
    }
}

// ========================================
// Move Handling
// ========================================

/**
 * Attempts to make a move at the specified board position.
 * * @param {number} row - Board row (0-14)
 * @param {number} col - Board column (0-14)
 * @returns {boolean} True if move was attempted, false if invalid
 */
function makeMove(row, col) {
    if (!gameActive || !myTurn) {
        console.log('Move ignored - not my turn or game not active');
        return false;
    }
    
    if (row < 0 || row >= BOARD_SIZE || col < 0 || col >= BOARD_SIZE) {
        console.log('Move ignored - out of bounds');
        return false;
    }
    
    if (gameBoard[row][col] !== CELL_STATE.FREE) {
        if (window.showGameMessage) {
            window.showGameMessage('Essa posição já está ocupada!', 'warning');
        }
        return false;
    }
    
    console.log('Making move at row=' + row + ', col=' + col);
    
    const timeConsumed = Date.now() - turnStartedLocally;
    updatePlayerTime(timeConsumed);
    
    executeOptimisticMove(row, col);
    switchTurns();
    updateUI();
    updateTimerDisplays();
    
    sendMoveToServer(row, col);
    return true;
}

/**
 * Updates the current player's remaining time after consuming time for a move.
 * * @param {number} timeConsumed - Time consumed in milliseconds
 */
function updatePlayerTime(timeConsumed) {
    if (gameConfig.myColor === 'Black') {
        blackTimeRemaining = Math.max(0, blackTimeRemaining - timeConsumed);
    } else {
        whiteTimeRemaining = Math.max(0, whiteTimeRemaining - timeConsumed);
    }
}

/**
 * Executes an optimistic move update on the local board.
 * * @param {number} row - Board row
 * @param {number} col - Board column
 */
function executeOptimisticMove(row, col) {
    const myPieceType = gameConfig.myColor === 'Black' ? CELL_STATE.BLACK : CELL_STATE.WHITE;
    gameBoard[row][col] = myPieceType;
    
    if (myPieceType === CELL_STATE.BLACK) {
        lastBlackMovePosition = { row, col };
    } else {
        lastWhiteMovePosition = { row, col };
    }
}

/**
 * Switches turns optimistically before server confirmation.
 */
function switchTurns() {
    currentTurn = currentTurn === 'Black' ? 'White' : 'Black';
    myTurn = false;
    turnStartedLocally = Date.now();
}

/**
 * Sends the player's move to the server for validation and processing.
 * @param {number} row - The row of the move.
 * @param {number} col - The column of the move.
 */
function sendMoveToServer(row, col) {
    fetch('game', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=move&gameId=' + encodeURIComponent(gameConfig.gameId) +
              '&row=' + row + '&col=' + col
    })
    .then(function(response) {
        if (response.ok) {
            return response.text();
        } else {
            return response.text().then(function(errorText) {
                throw new Error(errorText);
            });
        }
    })
    .then(function(data) {
        console.log('Move response OK:', data);
        if (data.includes('Error:') && !data.includes('Accepted')) {
            throw new Error(data);
        }
        waitingForGameState = true;
    })
    .catch(function(error) {
        console.error('Move was rejected:', error);
        revertOptimisticMove(row, col);
        showMoveError(error);
    });
}

/**
 * Reverts an optimistic move if the server rejects it.
 * @param {number} row - The row of the move to revert.
 * @param {number} col - The column of the move to revert.
 */
function revertOptimisticMove(row, col) {
    gameBoard[row][col] = CELL_STATE.FREE;
    currentTurn = currentTurn === 'Black' ? 'White' : 'Black';
    myTurn = true;
    turnStartedLocally = Date.now();
    waitingForGameState = false;
    
    // Revert last move tracking
    if (gameConfig.myColor === 'Black') {
        lastBlackMovePosition = null;
    } else {
        lastWhiteMovePosition = null;
    }
    
    updateUI();
}

/**
 * Displays a move error message to the user.
 * @param {Error} error - The error object received from the server or fetch call.
 */
function showMoveError(error) {
    let errorMsg = 'Jogada inválida!';
    if (error.message.includes('NotYourTurn')) {
        errorMsg = 'Não é a tua vez de jogar!';
    } else if (error.message.includes('InvalidMove')) {
        errorMsg = 'Jogada inválida!';
    } else if (error.message.includes('No connection')) {
        errorMsg = 'Erro de ligação';
    }
    
    if (window.showGameMessage) {
        window.showGameMessage(errorMsg, 'error');
    }
}

// ========================================
// Game End Handling
// ========================================

/**
 * Handles the end of the game, displaying results and stopping timers.
 * @param {object} data - The game end notification data from the server.
 */
function handleGameEnd(data) {
    gameActive = false;
    myTurn = false;
    
    stopAllTimers();
    
    const message = data.message || '';
    const winnerMatch = message.match(/Winner: (\w+)/);
    const winner = winnerMatch ? winnerMatch[1] : 'Desconhecido';
    
    let resultMessage = '';
    let alertType = 'info';
    
    if (winner === gameConfig.currentUser) {
        resultMessage = '🎉 Parabéns! Ganhaste!';
        alertType = 'success';
    } else {
        resultMessage = '😔 Perdeste. Vencedor: ' + winner;
        alertType = 'warning';
    }
    
    updateUI();
    
    if (window.showGameEndModal) {
        window.showGameEndModal(resultMessage, alertType);
    }
}

/**
 * Handles the scenario where the opponent disconnects from the game.
 */
function handleOpponentDisconnected() {
    gameActive = false;
    myTurn = false;
    
    stopAllTimers();
    updateUI();
    
    if (window.showGameMessage) {
        window.showGameMessage('O teu oponente desligou-se. Ganhas por desistência!', 'info');
    }
}

// ========================================
// Timer Management
// ========================================

/**
 * Starts all timer-related processes.
 */
function startTimerSystems() {
    startGameTimer();
    startLocalTimerUpdates();
}

/**
 * Starts the main game timer interval.
 */
function startGameTimer() {
    timerInterval = setInterval(updateGameTimer, 1000);
}

/**
 * Updates the total elapsed game time display.
 */
function updateGameTimer() {
    if (!gameActive) {
        clearInterval(timerInterval);
        return;
    }
    
    const elapsed = Math.floor((Date.now() - gameStartTime) / 1000);
    const minutes = Math.floor(elapsed / 60);
    const seconds = elapsed % 60;
    
    const timerElement = document.getElementById('gameTimer');
    if (timerElement) {
        timerElement.textContent = String(minutes).padStart(2, '0') + ':' + String(seconds).padStart(2, '0');
    }
}

/**
 * Starts the local interval for more frequent player timer UI updates.
 */
function startLocalTimerUpdates() {
    localTimerInterval = setInterval(updateTimerDisplays, 100);
    console.log('Local timer updates started');
}

/**
 * Updates the player-specific timer displays based on whose turn it is.
 */
function updateTimerDisplays() {
    if (!gameActive) return;
    
    const now = Date.now();
    const elapsedSinceTurnStarted = now - turnStartedLocally;
    
    let currentBlackTime = blackTimeRemaining;
    let currentWhiteTime = whiteTimeRemaining;
    
    if (currentTurn === 'Black') {
        currentBlackTime = Math.max(0, blackTimeRemaining - elapsedSinceTurnStarted);
    } else {
        currentWhiteTime = Math.max(0, whiteTimeRemaining - elapsedSinceTurnStarted);
    }
    
    updateTimerUI(currentBlackTime, currentWhiteTime);
    checkLowTimeWarning(currentBlackTime, currentWhiteTime);
}

/**
 * Renders the time remaining for each player in the UI.
 * @param {number} blackTime - The remaining time for the black player in milliseconds.
 * @param {number} whiteTime - The remaining time for the white player in milliseconds.
 */
function updateTimerUI(blackTime, whiteTime) {
    const blackTimer = document.getElementById('blackPlayerTimer');
    const whiteTimer = document.getElementById('whitePlayerTimer');
    
    if (blackTimer) {
        blackTimer.textContent = formatTime(blackTime);
        blackTimer.classList.remove('active-timer', 'warning-timer');
        if (currentTurn === 'Black') {
            blackTimer.classList.add('active-timer');
            if (blackTime <= 30 * 1000) {
                blackTimer.classList.add('warning-timer');
            }
        }
    }
    
    if (whiteTimer) {
        whiteTimer.textContent = formatTime(whiteTime);
        whiteTimer.classList.remove('active-timer', 'warning-timer');
        if (currentTurn === 'White') {
            whiteTimer.classList.add('active-timer');
            if (whiteTime <= 30 * 1000) {
                whiteTimer.classList.add('warning-timer');
            }
        }
    }
}

/**
 * Checks if the current player's time is low and displays a warning if necessary.
 * @param {number} blackTime - Black player's remaining time in milliseconds.
 * @param {number} whiteTime - White player's remaining time in milliseconds.
 */
function checkLowTimeWarning(blackTime, whiteTime) {
    const warningTime = 30 * 1000;
    
    if (myTurn) {
        const myTime = gameConfig.myColor === 'Black' ? blackTime : whiteTime;
        
        if (myTime <= warningTime && myTime > 0) {
            // Check if the second has just changed to show the message once per second
            if (Math.floor(myTime / 1000) !== Math.floor((myTime + 100) / 1000)) {
                if (window.showGameMessage) {
                    window.showGameMessage(`⚠️ Tens apenas ${Math.ceil(myTime / 1000)} segundos!`, 'warning');
                }
            }
        }
    }
}

/**
 * Formats milliseconds into a MM:SS string.
 * @param {number} milliseconds - The time in milliseconds.
 * @returns {string} The formatted time string.
 */
function formatTime(milliseconds) {
    if (milliseconds <= 0) return '00:00';
    
    const totalSeconds = Math.ceil(milliseconds / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    
    return `${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
}

/**
 * Stops all active timers and intervals.
 */
function stopAllTimers() {
    if (timerInterval) {
        clearInterval(timerInterval);
        timerInterval = null;
    }
    if (localTimerInterval) {
        clearInterval(localTimerInterval);
        localTimerInterval = null;
    }
}

// ========================================
// Server Communication
// ========================================

/**
 * Initializes server communication by starting polling mechanisms.
 */
function startServerCommunication() {
    startGameStatePolling();
    startGameNotificationPolling();
}

/**
 * Starts polling the server for game state updates.
 */
function startGameStatePolling() {
    console.log('Starting GameState polling');
    
    setInterval(function() {
        fetch('/IECD-TP2/api/gamestate?gameId=' + encodeURIComponent(gameConfig.gameId))
            .then(function(response) {
                return response.json();
            })
            .then(function(data) {
                if (data.gameState) {
                    updateGameState(data.gameState);
                }
            })
            .catch(function(error) {
                console.log('Game state polling error:', error);
            });
    }, 1000);
}

/**
 * Starts polling the server for game notifications (e.g., game end, opponent disconnect).
 */
function startGameNotificationPolling() {
    console.log('Starting Game Notifications polling');
    
    setInterval(function() {
        fetch('/IECD-TP2/api/game-notifications?gameId=' + encodeURIComponent(gameConfig.gameId))
            .then(function(response) {
                return response.json();
            })
            .then(function(data) {
                console.log('Game notification received:', data);
                
                if (data.type === 'gameEnd') {
                    handleGameEnd(data);
                } else if (data.type === 'opponentDisconnected') {
                    handleOpponentDisconnected();
                } else if (data.type === 'moveError') {
                    handleMoveError(data.status);
                }
            })
            .catch(function(error) {
                console.log('Game notifications polling error:', error);
            });
    }, 1000);
}

/**
 * Handles move-related errors received from server notifications.
 * @param {string} status - The error status message from the server.
 */
function handleMoveError(status) {
    let errorMsg = 'Jogada inválida!';
    if (status.includes('NotYourTurn')) {
        errorMsg = 'Não é a tua vez de jogar!';
    } else if (status.includes('InvalidMove')) {
        errorMsg = 'Jogada inválida!';
    } else if (status.includes('GameNotFound')) {
        errorMsg = 'Jogo não encontrado!';
    }
    
    if (window.showGameMessage) {
        window.showGameMessage(errorMsg, 'error');
    }
}

// ========================================
// Forfeit Handling
// ========================================

/**
 * Sends a request to the server to forfeit the game.
 */
function forfeitGame() {
    fetch('game', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        },
        body: 'action=forfeit&gameId=' + encodeURIComponent(gameConfig.gameId)
    })
    .then(function(response) {
        if (response.ok) {
            if (window.showGameMessage) {
                window.showGameMessage('Desististe da partida', 'info');
            }
            setTimeout(function() {
                window.location.href = 'lobby';
            }, 2000);
        } else {
            if (window.showGameMessage) {
                window.showGameMessage('Erro ao desistir da partida', 'error');
            }
        }
    })
    .catch(function(error) {
        console.error('Error:', error);
        if (window.showGameMessage) {
            window.showGameMessage('Erro de ligação', 'error');
        }
    });
}

// ========================================
// UI Integration
// ========================================

/**
 * Calls functions in the main UI script to update the visual representation of the game.
 */
function updateUI() {
    if (window.updateGameStatus) {
        window.updateGameStatus();
    }
    if (window.renderGameBoard) {
        window.renderGameBoard();
    }
}

// ========================================
// Utility Functions
// ========================================

/**
 * Returns the current state of the game.
 * @returns {object} An object containing the current game state.
 */
function getGameState() {
    return {
        board: gameBoard,
        currentTurn: currentTurn,
        myTurn: myTurn,
        gameActive: gameActive,
        gameConfig: gameConfig
    };
}

/**
 * Checks if a move is valid at the given coordinates.
 * @param {number} row - The row to check.
 * @param {number} col - The column to check.
 * @returns {boolean} True if the move is valid, false otherwise.
 */
function isValidMove(row, col) {
    return row >= 0 && row < BOARD_SIZE && 
           col >= 0 && col < BOARD_SIZE && 
           gameBoard[row][col] === CELL_STATE.FREE &&
           myTurn && gameActive;
}

/**
 * Gets the positions of the last moves made by each player.
 * @returns {object} An object with `black` and `white` properties containing the last move positions.
 */
function getLastMovePositions() {
    return {
        black: lastBlackMovePosition,
        white: lastWhiteMovePosition
    };
}

// ========================================
// Global Exports
// ========================================

window.gameLogic = {
    initializeGame: initializeGame,
    makeMove: makeMove,
    forfeitGame: forfeitGame,
    getGameState: getGameState,
    isValidMove: isValidMove,
    getLastMovePositions: getLastMovePositions,
    handleGameEnd: handleGameEnd,
    handleOpponentDisconnected: handleOpponentDisconnected
};

console.log('Game Logic functions exported globally');
