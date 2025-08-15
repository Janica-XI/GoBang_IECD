// ========================================
// GAME UI - Interface and Visualization
// ========================================

console.log('Game UI loaded - 2025 Responsive version');

// ========================================
// Configuration and State
// ========================================

const UI_COLORS = {
    BOARD_BG: '#1e1e2e',
    BOARD_LINES: '#44475a',
    BOARD_STARS: '#6272a4',
    BOARD_HOVER: 'rgba(255, 255, 255, 0.1)',
    
    PLAYER_BLACK: '#00f5ff',
    PLAYER_WHITE: '#ff6b6b',
    
    // Last move highlighting (lighter shades)
    LAST_MOVE_BLACK: '#4DFFFF',
    LAST_MOVE_WHITE: '#FF9999',
    
    GLOW_BLUE: 'rgba(0, 245, 255, 0.3)',
    GLOW_RED: 'rgba(255, 107, 107, 0.3)',
    SHADOW: 'rgba(0, 0, 0, 0.5)'
};

let currentHoverRow = -1;
let currentHoverCol = -1;
let activeParticleEffects = [];
let animationFrame = null;
let logicalCanvasSize = { width: 600, height: 600 };
let canvasResizeTimeout = null;

// ========================================
// Initialization
// ========================================

document.addEventListener('DOMContentLoaded', function() {
    console.log('Initializing responsive game UI');
    
    if (!initializeGameLogic()) {
        console.error('Failed to initialize game logic');
        return;
    }
    
    setupResponsiveCanvas();
    setupEventHandlers();
    renderGameBoard();
    startAnimationLoop();
    updateGameStatus();
    
    console.log('Game UI initialized successfully');
});

/**
 * Initializes the game logic system.
 * * @returns {boolean} True if initialization succeeded, false otherwise
 */
function initializeGameLogic() {
    return window.gameLogic && window.gameLogic.initializeGame && window.gameLogic.initializeGame();
}

// ========================================
// Canvas Management
// ========================================

/**
 * Sets up the responsive canvas system with automatic resizing.
 */
function setupResponsiveCanvas() {
    resizeCanvasToOptimalSize();
    
    window.addEventListener('resize', function() {
        clearTimeout(canvasResizeTimeout);
        canvasResizeTimeout = setTimeout(resizeCanvasToOptimalSize, 100);
    });
}

/**
 * Resizes the canvas to optimal size based on container and screen dimensions.
 */
function resizeCanvasToOptimalSize() {
    const canvas = document.getElementById('gameBoard');
    const container = canvas.parentElement;
    const containerRect = container.getBoundingClientRect();
    
    const maxSize = Math.min(containerRect.width - 40, window.innerHeight * 0.6, 700);
    const optimalSize = Math.max(300, Math.min(maxSize, 600));
    
    canvas.style.width = optimalSize + 'px';
    canvas.style.height = optimalSize + 'px';
    
    const devicePixelRatio = window.devicePixelRatio || 1;
    canvas.width = logicalCanvasSize.width * devicePixelRatio;
    canvas.height = logicalCanvasSize.height * devicePixelRatio;
    
    const ctx = canvas.getContext('2d');
    ctx.scale(devicePixelRatio, devicePixelRatio);
    
    canvas.displayWidth = optimalSize;
    canvas.displayHeight = optimalSize;
    
    renderGameBoard();
    console.log(`Canvas resized to ${optimalSize}x${optimalSize}, DPR: ${devicePixelRatio}`);
}

/**
 * Converts screen coordinates to canvas coordinates.
 * * @param {MouseEvent|Touch} event - Mouse or touch event
 * @returns {Object} Canvas coordinates {x, y}
 */
function getCanvasCoordinates(event) {
    const canvas = document.getElementById('gameBoard');
    const rect = canvas.getBoundingClientRect();
    
    const scaleX = logicalCanvasSize.width / rect.width;
    const scaleY = logicalCanvasSize.height / rect.height;
    
    return {
        x: (event.clientX - rect.left) * scaleX,
        y: (event.clientY - rect.top) * scaleY
    };
}

/**
 * Converts canvas coordinates to board grid position.
 * * @param {number} canvasX - Canvas X coordinate
 * @param {number} canvasY - Canvas Y coordinate
 * @returns {Object} Board position {row, col}
 */
function getBoardPosition(canvasX, canvasY) {
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    
    return {
        row: Math.round((canvasY - PADDING) / CELL_SIZE),
        col: Math.round((canvasX - PADDING) / CELL_SIZE)
    };
}

/**
 * Gets the center coordinates of a specific board cell.
 * * @param {number} row - Board row
 * @param {number} col - Board column
 * @returns {Object} Center coordinates {x, y}
 */
function getCellCenter(row, col) {
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    
    return {
        x: PADDING + col * CELL_SIZE,
        y: PADDING + row * CELL_SIZE
    };
}

// ========================================
// Rendering System
// ========================================

/**
 * Main rendering function that orchestrates the drawing of the entire game board.
 */
function renderGameBoard() {
    const gameState = window.gameLogic.getGameState();
    const canvas = document.getElementById('gameBoard');
    const ctx = canvas.getContext('2d');
    
    clearCanvasWithBackground(ctx);
    drawGridLines(ctx);
    drawStarPoints(ctx);
    drawHoverPreview(ctx);
    drawAllPieces(ctx, gameState.board);
    drawParticleEffects(ctx);
}

/**
 * Clears the canvas and draws the background gradient.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 */
function clearCanvasWithBackground(ctx) {
    ctx.fillStyle = UI_COLORS.BOARD_BG;
    ctx.fillRect(0, 0, logicalCanvasSize.width, logicalCanvasSize.height);
    
    const gradient = ctx.createRadialGradient(
        logicalCanvasSize.width / 2, logicalCanvasSize.height / 2, 0,
        logicalCanvasSize.width / 2, logicalCanvasSize.height / 2, logicalCanvasSize.width / 2
    );
    gradient.addColorStop(0, 'rgba(255, 255, 255, 0.02)');
    gradient.addColorStop(1, 'rgba(0, 0, 0, 0.1)');
    
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, logicalCanvasSize.width, logicalCanvasSize.height);
}

/**
 * Draws the grid lines on the game board.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 */
function drawGridLines(ctx) {
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    
    ctx.strokeStyle = UI_COLORS.BOARD_LINES;
    ctx.lineWidth = 1.5;
    ctx.lineCap = 'round';
    
    for (let i = 0; i <= 14; i++) {
        const x = PADDING + i * CELL_SIZE;
        const y = PADDING + i * CELL_SIZE;
        
        // Vertical lines
        ctx.beginPath();
        ctx.moveTo(x, PADDING);
        ctx.lineTo(x, PADDING + BOARD_WIDTH);
        ctx.stroke();
        
        // Horizontal lines
        ctx.beginPath();
        ctx.moveTo(PADDING, y);
        ctx.lineTo(PADDING + BOARD_WIDTH, y);
        ctx.stroke();
    }
}

/**
 * Draws the star points (Hoshi) on the game board.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 */
function drawStarPoints(ctx) {
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    const STAR_RADIUS = CELL_SIZE / 8;
    const STAR_POSITIONS = [[3, 3], [11, 3], [7, 7], [3, 11], [11, 11]];
    
    ctx.fillStyle = UI_COLORS.BOARD_STARS;
    ctx.shadowColor = UI_COLORS.BOARD_STARS;
    ctx.shadowBlur = 8;
    
    STAR_POSITIONS.forEach(function(pos) {
        const x = PADDING + pos[0] * CELL_SIZE;
        const y = PADDING + pos[1] * CELL_SIZE;
        
        ctx.beginPath();
        ctx.arc(x, y, STAR_RADIUS, 0, 2 * Math.PI);
        ctx.fill();
    });
    
    ctx.shadowBlur = 0;
}

/**
 * Draws a preview piece at the current hover position if the move is valid.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 */
function drawHoverPreview(ctx) {
    if (currentHoverRow >= 0 && currentHoverCol >= 0 && 
        window.gameLogic.isValidMove(currentHoverRow, currentHoverCol)) {
        drawPreviewPiece(ctx, currentHoverRow, currentHoverCol);
    }
}

/**
 * Iterates through the game board state and draws all placed pieces.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {Array<Array<number>>} board - The 2D array representing the game board.
 */
function drawAllPieces(ctx, board) {
    for (let row = 0; row < 15; row++) {
        for (let col = 0; col < 15; col++) {
            if (board[row][col] !== 0) {
                drawGamePiece(ctx, row, col, board[row][col]);
            }
        }
    }
}

/**
 * Draws a single game piece with glow, body, and highlight effects.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} row - The board row of the piece.
 * @param {number} col - The board column of the piece.
 * @param {number} pieceState - The state of the piece (1 for Black, 2 for White).
 */
function drawGamePiece(ctx, row, col, pieceState) {
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    const PIECE_RADIUS = CELL_SIZE / 2 - 4;
    
    const x = PADDING + col * CELL_SIZE;
    const y = PADDING + row * CELL_SIZE;
    
    const isBlackPiece = pieceState === 1;
    const isLastMove = checkIfLastMove(row, col, pieceState);
    
    // Choose color based on whether this is the last move
    let pieceColor;
    if (isLastMove) {
        pieceColor = isBlackPiece ? UI_COLORS.LAST_MOVE_BLACK : UI_COLORS.LAST_MOVE_WHITE;
    } else {
        pieceColor = isBlackPiece ? UI_COLORS.PLAYER_BLACK : UI_COLORS.PLAYER_WHITE;
    }
    
    drawPieceGlow(ctx, x, y, PIECE_RADIUS, pieceColor);
    drawPieceBody(ctx, x, y, PIECE_RADIUS, pieceColor);
    drawPieceHighlight(ctx, x, y, PIECE_RADIUS);
    
    if (isLastMove) {
        drawLastMoveIndicator(ctx, x, y, PIECE_RADIUS);
    }
    
    ctx.shadowBlur = 0;
    ctx.shadowColor = 'transparent';
}

/**
 * Checks if a piece at a given position was the last move made by that color.
 * @param {number} row - The board row of the piece.
 * @param {number} col - The board column of the piece.
 * @param {number} pieceState - The state of the piece (1 for Black, 2 for White).
 * @returns {boolean} True if it is the last move, false otherwise.
 */
function checkIfLastMove(row, col, pieceState) {
    if (!window.gameLogic.getLastMovePositions) return false;
    
    const lastMoves = window.gameLogic.getLastMovePositions();
    const isBlackPiece = pieceState === 1;
    
    if (isBlackPiece && lastMoves.black) {
        return lastMoves.black.row === row && lastMoves.black.col === col;
    } else if (!isBlackPiece && lastMoves.white) {
        return lastMoves.white.row === row && lastMoves.white.col === col;
    }
    
    return false;
}

/**
 * Draws the outer glow effect for a game piece.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} x - The center x-coordinate of the piece.
 * @param {number} y - The center y-coordinate of the piece.
 * @param {number} radius - The radius of the piece.
 * @param {string} color - The base color of the piece.
 */
function drawPieceGlow(ctx, x, y, radius, color) {
    ctx.shadowColor = color;
    ctx.shadowBlur = 15;
    
    const gradient = ctx.createRadialGradient(x, y, 0, x, y, radius + 10);
    gradient.addColorStop(0, color);
    gradient.addColorStop(0.7, color + '80');
    gradient.addColorStop(1, 'transparent');
    
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(x, y, radius + 8, 0, 2 * Math.PI);
    ctx.fill();
}

/**
 * Draws the main body of a game piece with a radial gradient.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} x - The center x-coordinate of the piece.
 * @param {number} y - The center y-coordinate of the piece.
 * @param {number} radius - The radius of the piece.
 * @param {string} color - The base color of the piece.
 */
function drawPieceBody(ctx, x, y, radius, color) {
    const gradient = ctx.createRadialGradient(
        x - radius * 0.3, y - radius * 0.3, 0,
        x, y, radius
    );
    gradient.addColorStop(0, color + 'FF');
    gradient.addColorStop(0.6, color + 'DD');
    gradient.addColorStop(1, color + '99');
    
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    ctx.fill();
}

/**
 * Draws a subtle highlight on the top-left of a game piece to give it a 3D effect.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} x - The center x-coordinate of the piece.
 * @param {number} y - The center y-coordinate of the piece.
 * @param {number} radius - The radius of the piece.
 */
function drawPieceHighlight(ctx, x, y, radius) {
    const gradient = ctx.createRadialGradient(
        x - radius * 0.4, y - radius * 0.4, 0,
        x - radius * 0.4, y - radius * 0.4, radius * 0.6
    );
    gradient.addColorStop(0, 'rgba(255, 255, 255, 0.6)');
    gradient.addColorStop(1, 'rgba(255, 255, 255, 0)');
    
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    ctx.fill();
}

/**
 * Draws a pulsating ring around the last-played piece.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} x - The center x-coordinate of the piece.
 * @param {number} y - The center y-coordinate of the piece.
 * @param {number} radius - The radius of the piece.
 */
function drawLastMoveIndicator(ctx, x, y, radius) {
    const time = Date.now() * 0.003;
    const pulse = Math.sin(time) * 0.3 + 0.7;
    
    ctx.save();
    ctx.globalAlpha = pulse * 0.6;
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)';
    ctx.lineWidth = 3;
    
    ctx.beginPath();
    ctx.arc(x, y, radius + 6, 0, 2 * Math.PI);
    ctx.stroke();
    
    ctx.restore();
}

/**
 * Draws a translucent preview piece at a specified board location.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 * @param {number} row - The board row for the preview.
 * @param {number} col - The board column for the preview.
 */
function drawPreviewPiece(ctx, row, col) {
    const gameState = window.gameLogic.getGameState();
    const PADDING = 30;
    const BOARD_WIDTH = logicalCanvasSize.width - 2 * PADDING;
    const CELL_SIZE = BOARD_WIDTH / 14;
    const PIECE_RADIUS = CELL_SIZE / 2 - 4;
    
    const x = PADDING + col * CELL_SIZE;
    const y = PADDING + row * CELL_SIZE;
    
    const isUserBlack = gameState.gameConfig.myColor === 'Black';
    const color = isUserBlack ? UI_COLORS.PLAYER_BLACK : UI_COLORS.PLAYER_WHITE;
    
    ctx.save();
    ctx.globalAlpha = 0.4;
    
    const time = Date.now() * 0.005;
    const pulse = Math.sin(time) * 0.1 + 0.9;
    const radius = PIECE_RADIUS * pulse;
    
    ctx.shadowColor = color;
    ctx.shadowBlur = 8;
    ctx.fillStyle = color + '80';
    ctx.beginPath();
    ctx.arc(x, y, radius, 0, 2 * Math.PI);
    ctx.fill();
    
    ctx.restore();
}

// ========================================
// Particle Effects
// ========================================

/**
 * Creates a burst of particles at a specified location.
 * @param {number} x - The starting x-coordinate for the particles.
 * @param {number} y - The starting y-coordinate for the particles.
 * @param {string} color - The color of the particles.
 */
function createParticleEffect(x, y, color) {
    for (let i = 0; i < 8; i++) {
        activeParticleEffects.push({
            x: x,
            y: y,
            vx: (Math.random() - 0.5) * 4,
            vy: (Math.random() - 0.5) * 4,
            life: 1.0,
            decay: 0.02,
            color: color,
            size: Math.random() * 3 + 2
        });
    }
}

/**
 * Updates the position and life of all active particles.
 */
function updateParticleEffects() {
    activeParticleEffects = activeParticleEffects.filter(particle => {
        particle.x += particle.vx;
        particle.y += particle.vy;
        particle.life -= particle.decay;
        particle.vx *= 0.98;
        particle.vy *= 0.98;
        return particle.life > 0;
    });
}

/**
 * Draws all active particles on the canvas.
 * @param {CanvasRenderingContext2D} ctx - The canvas rendering context.
 */
function drawParticleEffects(ctx) {
    activeParticleEffects.forEach(particle => {
        ctx.save();
        ctx.globalAlpha = particle.life;
        ctx.fillStyle = particle.color;
        ctx.shadowColor = particle.color;
        ctx.shadowBlur = 5;
        
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, particle.size, 0, 2 * Math.PI);
        ctx.fill();
        
        ctx.restore();
    });
}

/**
 * Starts the main animation loop for updating and rendering particle effects.
 */
function startAnimationLoop() {
    function animate() {
        updateParticleEffects();
        
        if (activeParticleEffects.length > 0) {
            renderGameBoard();
        }
        
        animationFrame = requestAnimationFrame(animate);
    }
    animate();
}

// ========================================
// Event Handling
// ========================================

/**
 * Sets up all primary event listeners for the UI.
 */
function setupEventHandlers() {
    setupCanvasEvents();
    setupWindowEvents();
}

/**
 * Sets up event listeners specifically for the game canvas.
 */
function setupCanvasEvents() {
    const canvas = document.getElementById('gameBoard');
    
    canvas.addEventListener('click', handleCanvasClick);
    canvas.addEventListener('mousemove', handleMouseMove);
    canvas.addEventListener('mouseleave', handleMouseLeave);
    
    // Touch support for mobile
    canvas.addEventListener('touchstart', handleTouchStart, { passive: false });
    canvas.addEventListener('touchmove', handleTouchMove, { passive: false });
    canvas.addEventListener('touchend', handleTouchEnd, { passive: false });
}

/**
 * Sets up global window event listeners, such as for closing the tab.
 */
function setupWindowEvents() {
    window.addEventListener('beforeunload', function(e) {
        const gameState = window.gameLogic.getGameState();
        if (gameState.gameActive && 
            (gameState.gameConfig.isBlackPlayer || gameState.gameConfig.isWhitePlayer)) {
            
            navigator.sendBeacon('game', 'action=forfeit&gameId=' + 
                encodeURIComponent(gameState.gameConfig.gameId));
            
            const message = 'Se fechares esta janela, irás desistir da partida!';
            e.returnValue = message;
            return message;
        }
    });
}

/**
 * Handles the mouse move event on the canvas to show the hover preview.
 * @param {MouseEvent} event - The mouse move event.
 */
function handleMouseMove(event) {
    const gameState = window.gameLogic.getGameState();
    if (!gameState.gameActive || !gameState.myTurn) return;
    
    const coords = getCanvasCoordinates(event);
    const pos = getBoardPosition(coords.x, coords.y);
    
    if (shouldUpdateHover(pos)) {
        currentHoverRow = pos.row;
        currentHoverCol = pos.col;
        renderGameBoard();
    }
}

/**
 * Determines if the hover position has changed to a new valid grid cell.
 * @param {{row: number, col: number}} pos - The current board position under the cursor.
 * @returns {boolean} True if the hover state should be updated.
 */
function shouldUpdateHover(pos) {
    return (pos.row !== currentHoverRow || pos.col !== currentHoverCol) && 
           pos.row >= 0 && pos.row < 15 && 
           pos.col >= 0 && pos.col < 15;
}

/**
 * Handles the mouse leave event to clear the hover preview.
 */
function handleMouseLeave() {
    if (currentHoverRow >= 0 || currentHoverCol >= 0) {
        currentHoverRow = -1;
        currentHoverCol = -1;
        renderGameBoard();
    }
}

/**
 * Handles the click event on the canvas to make a move.
 * @param {MouseEvent} event - The click event.
 */
function handleCanvasClick(event) {
    const gameState = window.gameLogic.getGameState();
    if (!gameState.gameActive || !gameState.myTurn) {
        console.log('Click ignored - not my turn or game not active');
        return;
    }
    
    const coords = getCanvasCoordinates(event);
    const pos = getBoardPosition(coords.x, coords.y);
    
    if (pos.row < 0 || pos.row >= 15 || pos.col < 0 || pos.col >= 15) {
        return;
    }
    
    // Create particle effect
    const center = getCellCenter(pos.row, pos.col);
    const isUserBlack = gameState.gameConfig.myColor === 'Black';
    const effectColor = isUserBlack ? UI_COLORS.PLAYER_BLACK : UI_COLORS.PLAYER_WHITE;
    
    createParticleEffect(center.x, center.y, effectColor);
    
    // Clear hover and make move
    currentHoverRow = -1;
    currentHoverCol = -1;
    window.gameLogic.makeMove(pos.row, pos.col);
}

// Touch event handlers
/**
 * Handles the touch start event, treating it like a mouse move for hover preview.
 * @param {TouchEvent} event - The touch start event.
 */
function handleTouchStart(event) {
    event.preventDefault();
    if (event.touches.length === 1) {
        const touch = event.touches[0];
        const mouseEvent = new MouseEvent('mousemove', {
            clientX: touch.clientX,
            clientY: touch.clientY
        });
        handleMouseMove(mouseEvent);
    }
}

/**
 * Handles the touch move event to update the hover preview.
 * @param {TouchEvent} event - The touch move event.
 */
function handleTouchMove(event) {
    event.preventDefault();
    if (event.touches.length === 1) {
        const touch = event.touches[0];
        const mouseEvent = new MouseEvent('mousemove', {
            clientX: touch.clientX,
            clientY: touch.clientY
        });
        handleMouseMove(mouseEvent);
    }
}

/**
 * Handles the touch end event, treating it as a click to make a move.
 * @param {TouchEvent} event - The touch end event.
 */
function handleTouchEnd(event) {
    event.preventDefault();
    if (event.changedTouches.length === 1) {
        const touch = event.changedTouches[0];
        const mouseEvent = new MouseEvent('click', {
            clientX: touch.clientX,
            clientY: touch.clientY
        });
        handleCanvasClick(mouseEvent);
    }
}

// ========================================
// UI Status Updates
// ========================================

/**
 * Updates the main status text display based on the current game state.
 */
function updateGameStatus() {
    const gameState = window.gameLogic.getGameState();
    const statusElement = document.getElementById('statusText');
    
    if (!statusElement) return;
    
    if (!gameState.gameActive) {
        statusElement.innerHTML = '<i class="fas fa-trophy me-2"></i>Jogo terminado';
    } else if (gameState.gameConfig.myColor === 'Spectator') {
        statusElement.innerHTML = '<i class="fas fa-eye me-2"></i>Estás a assistir este jogo';
    } else if (gameState.myTurn) {
        statusElement.innerHTML = '<i class="fas fa-mouse-pointer me-2"></i>É a tua vez! Clica no tabuleiro para jogar.';
    } else {
        statusElement.innerHTML = '<i class="fas fa-hourglass-half me-2"></i>A aguardar jogada do oponente...';
    }
    
    updateTurnIndicator();
}

/**
 * Updates the turn indicator display and player card highlighting.
 */
function updateTurnIndicator() {
    const gameState = window.gameLogic.getGameState();
    const indicator = document.getElementById('turnIndicator');
    const blackCard = document.getElementById('blackPlayerCard');
    const whiteCard = document.getElementById('whitePlayerCard');
    
    if (!indicator) {
        console.error('turnIndicator element not found');
        return;
    }
    
    // Reset classes
    if (blackCard) blackCard.classList.remove('active-black');
    if (whiteCard) whiteCard.classList.remove('active-white');
    indicator.className = 'turn-indicator';
    
    if (!gameState.gameActive) {
        indicator.innerHTML = '🏁 Jogo Terminado';
        return;
    }
    
    if (gameState.gameConfig.myColor === 'Spectator') {
        indicator.innerHTML = '<i class="fas fa-eye me-1"></i> A assistir jogo';
    } else if (gameState.myTurn) {
        showMyTurn(gameState, indicator, blackCard, whiteCard);
    } else {
        showOpponentTurn(gameState, indicator, blackCard, whiteCard);
    }
}

/**
 * Sets the UI to indicate it is the current player's turn.
 * @param {object} gameState - The current game state.
 * @param {HTMLElement} indicator - The turn indicator element.
 * @param {HTMLElement} blackCard - The black player's card element.
 * @param {HTMLElement} whiteCard - The white player's card element.
 */
function showMyTurn(gameState, indicator, blackCard, whiteCard) {
    if (gameState.gameConfig.myColor === 'Black') {
        indicator.innerHTML = '<i class="fas fa-circle piece-black me-1"></i> É a tua vez (Azuis)';
        indicator.classList.add('black-turn');
        if (blackCard) blackCard.classList.add('active-black');
    } else {
        indicator.innerHTML = '<i class="fas fa-circle piece-white me-1"></i> É a tua vez (Vermelhas)';
        indicator.classList.add('white-turn');
        if (whiteCard) whiteCard.classList.add('active-white');
    }
}

/**
 * Sets the UI to indicate it is the opponent's turn.
 * @param {object} gameState - The current game state.
 * @param {HTMLElement} indicator - The turn indicator element.
 * @param {HTMLElement} blackCard - The black player's card element.
 * @param {HTMLElement} whiteCard - The white player's card element.
 */
function showOpponentTurn(gameState, indicator, blackCard, whiteCard) {
    if (gameState.gameConfig.myColor === 'Black') {
        indicator.innerHTML = '<i class="fas fa-circle piece-white me-1"></i> Vez do oponente (Vermelhas)';
        indicator.classList.add('white-turn');
        if (whiteCard) whiteCard.classList.add('active-white');
    } else {
        indicator.innerHTML = '<i class="fas fa-circle piece-black me-1"></i> Vez do oponente (Azuis)';
        indicator.classList.add('black-turn');
        if (blackCard) blackCard.classList.add('active-black');
    }
}

// ========================================
// Modal and Message Functions
// ========================================

/**
 * Displays a temporary, non-blocking message to the user (a "toast" notification).
 * @param {string} message - The message to display.
 * @param {string} type - The type of message ('info', 'success', 'warning', 'error').
 */
function showGameMessage(message, type) {
    if (!type) type = 'info';
    
    const alertConfig = {
        'error': { class: 'alert-danger', icon: 'fas fa-exclamation-triangle' },
        'success': { class: 'alert-success', icon: 'fas fa-check-circle' },
        'warning': { class: 'alert-warning', icon: 'fas fa-exclamation-circle' },
        'info': { class: 'alert-info', icon: 'fas fa-info-circle' }
    };
    
    const config = alertConfig[type] || alertConfig['info'];
    
    const alertHtml = `
        <div class="alert ${config.class} alert-dismissible fade show position-fixed" 
             style="top: 20px; right: 20px; z-index: 9999; min-width: 350px; 
                    backdrop-filter: blur(10px); border: 1px solid rgba(255,255,255,0.2);
                    border-radius: 12px; box-shadow: 0 8px 32px rgba(0,0,0,0.3);" role="alert">
            <i class="${config.icon} me-2"></i>${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert"></button>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', alertHtml);
    
    setTimeout(function() {
        const alert = document.querySelector('.alert.show');
        if (alert) alert.remove();
    }, 4000);
}

/**
 * Displays a modal dialog at the end of the game.
 * @param {string} message - The result message to display.
 * @param {string} type - The type of result ('success' for win, 'warning' for loss).
 */
function showGameEndModal(message, type) {
    const bgClass = type === 'success' ? 'bg-success' : 'bg-warning';
    const icon = type === 'success' ? 'fas fa-trophy' : 'fas fa-medal';
    
    const modalHtml = `
        <div class="modal fade modern-modal" id="gameEndModal" tabindex="-1" data-bs-backdrop="static">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header ${bgClass} text-white">
                        <h5 class="modal-title"><i class="${icon} me-2"></i>Jogo Terminado</h5>
                    </div>
                    <div class="modal-body text-center">
                        <h4 class="mb-3">${message}</h4>
                        <p class="text-muted">O que queres fazer agora?</p>
                    </div>
                    <div class="modal-footer justify-content-center">
                        <button type="button" class="btn btn-modern" onclick="window.location.href='lobby'">
                            <i class="fas fa-home me-2"></i>Voltar ao Lobby
                        </button>
                        <button type="button" class="btn btn-modern btn-danger-modern" onclick="window.close()">
                            <i class="fas fa-times me-2"></i>Fechar Aba
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    const modal = new bootstrap.Modal(document.getElementById('gameEndModal'));
    modal.show();
}

/**
 * Shows the confirmation modal for forfeiting the game.
 */
function showForfeitModal() {
    const modal = new bootstrap.Modal(document.getElementById('forfeitModal'));
    modal.show();
}

/**
 * Confirms the forfeit action, hides the modal, and calls the game logic to forfeit.
 */
function confirmForfeit() {
    const modal = bootstrap.Modal.getInstance(document.getElementById('forfeitModal'));
    modal.hide();
    window.gameLogic.forfeitGame();
}

// ========================================
// Global Exports
// ========================================

window.updateTurnIndicator = updateTurnIndicator;
window.updateGameStatus = updateGameStatus;
window.showGameMessage = showGameMessage;
window.showGameEndModal = showGameEndModal;
window.showForfeitModal = showForfeitModal;
window.confirmForfeit = confirmForfeit;
window.renderGameBoard = renderGameBoard;
window.resizeCanvasToOptimalSize = resizeCanvasToOptimalSize;
window.createParticleEffect = createParticleEffect;

console.log('Game UI functions exported globally');