<%@ page import="web.servlets.GameServlet.GameData"%>
<%@ page import="core.Player"%>
<%
    pageContext.setAttribute("pageTitle", "Game");
    GameData gameData = (GameData) request.getAttribute("gameData");
    
    // Se não há dados, redirecionar
    if (gameData == null) {
        response.sendRedirect("lobby");
        return;
    }
%>
<%
Player loggedInPlayer = (Player) session.getAttribute("loggedInPlayer");
String currentTheme = (loggedInPlayer != null && loggedInPlayer.getTheme() != null) 
    ? loggedInPlayer.getTheme() : "default";
%>
<%@ include file="includes/header.jsp"%>
<link rel="stylesheet" href="css/themes/<%=currentTheme%>.css">

<div class="container-fluid py-4">
	<!-- Game Header -->
	<div class="game-header">
		<div class="row align-items-center">
			<!-- Black Player -->
			<div class="col-md-4">
				<div class="player-card" id="blackPlayerCard">
					<div class="d-flex align-items-center">
						<img src="<%= gameData.getBlackPlayerPhotoSrc() %>"
							alt="Black Player" class="player-avatar me-3"
							id="blackPlayerAvatar" onerror="this.src='images/default.jpg'">
						<div class="flex-grow-1">
							<div class="d-flex align-items-center mb-1">
								<i class="fas fa-circle piece-black me-2"></i> <strong
									class="fs-6"><%= gameData.getBlackPlayer() %></strong>
								<% if (gameData.isBlackPlayer()) { %>
								<span class="badge-you ms-2">TU</span>
								<% } %>
							</div>
							<small class="piece-description">Peças Azuis</small>
							<div class="mt-1">
                                    <span class="timer-display" id="blackPlayerTimer">05:00</span>
                                </div>
						</div>
					</div>
				</div>
			</div>

			<!-- Game Info Center -->
			<div class="col-md-4 text-center">
				<div class="mb-2">
					<h6 class="mb-1 fw-bold">
						Jogo:
						<%= gameData.getGameIdShort() %></h6>
					<div class="turn-indicator" id="turnIndicator">
						<i class="fas fa-circle piece-black me-1"></i> Vez das Azuis
					</div>
				</div>
				<div class="game-timer">
					<i class="fas fa-clock me-1"></i> <span id="gameTimer">00:00</span>
				</div>
			</div>

			<!-- White Player -->
			<div class="col-md-4">
				<div class="player-card" id="whitePlayerCard">
					<div class="d-flex align-items-center justify-content-end">
						<div class="text-end me-3">
							<div class="d-flex align-items-center justify-content-end mb-1">
								<% if (gameData.isWhitePlayer()) { %>
								<span class="badge-you me-2">TU</span>
								<% } %>
								<strong class="fs-6"><%= gameData.getWhitePlayer() %></strong> <i
									class="fas fa-circle piece-white ms-2"></i>
							</div>
							<small class="piece-description">Peças Vermelhas</small>
							<div class="mt-1">
                                    <span class="timer-display" id="whitePlayerTimer">05:00</span>
                                </div>
						</div>
						<img src="<%= gameData.getWhitePlayerPhotoSrc() %>"
							alt="White Player" class="player-avatar" id="whitePlayerAvatar"
							onerror="this.src='images/default.jpg'">
					</div>
				</div>
			</div>
		</div>
	</div>

	<!-- Game Board Section -->
	<div id="game-board" class="row justify-content-center">
		<div class="col-xl-8 col-lg-10">
			<div class="board-container">
				<!-- Your role info -->
				<div class="info-card text-center mb-4">
					<h6 class="mb-2">Tu jogas como:</h6>
					<% if (gameData.isBlackPlayer()) { %>
					<span class="badge-modern piece-black fw-bold fs-6"> <i
						class="fas fa-circle me-1"></i> Peças Azuis (começas primeiro)
					</span>
					<% } else if (gameData.isWhitePlayer()) { %>
					<span class="badge-modern piece-white fw-bold fs-6"> <i
						class="fas fa-circle me-1"></i> Peças Vermelhas
					</span>
					<% } else { %>
					<span class="badge-modern"> <i class="fas fa-eye me-1"></i>
						Espectador
					</span>
					<% } %>
				</div>

				<!-- Game Board Canvas -->
				<div class="d-flex justify-content-center mb-4">
					<canvas id="gameBoard" width="600" height="600"
						style="cursor: crosshair;">
                            O teu navegador não suporta Canvas HTML5
                        </canvas>
				</div>

				<!-- Game Status -->
				<div class="game-status mb-4" id="gameStatus">
					<i class="fas fa-info-circle me-2"></i> <span class="status-text"
						id="statusText">A aguardar estado do jogo...</span>
				</div>

				<!-- Action Buttons -->
				<div class="text-center">
					<% if (gameData.getCurrentUser().equals(gameData.getBlackPlayer()) || 
                               gameData.getCurrentUser().equals(gameData.getWhitePlayer())) { %>
					<button onclick="showForfeitModal()"
						class="btn btn-modern btn-danger-modern">
						<i class="fas fa-flag me-2"></i> Desistir
					</button>
					<% } %>
				</div>
			</div>
		</div>
	</div>
</div>

<!-- Modal de Desistência -->
<div class="modal fade modern-modal" id="forfeitModal" tabindex="-1"
	data-bs-backdrop="static">
	<div class="modal-dialog">
		<div class="modal-content">
			<div class="modal-header bg-danger text-white">
				<h5 class="modal-title">
					<i class="fas fa-exclamation-triangle me-2"></i> Confirmar
					Desistência
				</h5>
			</div>
			<div class="modal-body text-center">
				<h6 class="mb-3">Tens a certeza que queres desistir desta
					partida?</h6>
				<p class="text-muted">
					<i class="fas fa-info-circle me-1"></i> Esta ação não pode ser
					desfeita e contará como uma derrota.
				</p>
			</div>
			<div class="modal-footer justify-content-center">
				<button type="button" class="btn btn-modern" data-bs-dismiss="modal">
					<i class="fas fa-times me-2"></i>Cancelar
				</button>
				<button type="button" class="btn btn-modern btn-danger-modern"
					onclick="confirmForfeit()">
					<i class="fas fa-flag me-2"></i>Sim, Desistir
				</button>
			</div>
		</div>
	</div>
</div>

<!-- Game Config (será movido para JS externo) -->
<script>
        window.gameConfig = {
            gameId: '<%= gameData.getGameId() %>',
            currentUser: '<%= gameData.getCurrentUser() %>',
            blackPlayer: '<%= gameData.getBlackPlayer() %>',
            whitePlayer: '<%= gameData.getWhitePlayer() %>',
            myColor: '<%= gameData.getMyColor() %>',
            isBlackPlayer: <%= gameData.isBlackPlayer() %>,
            isWhitePlayer: <%= gameData.isWhitePlayer() %>,
            gameStartTimestamp: <%= gameData.getGameTimestamp() %>
        };
    </script>

<script src="js/game/game-logic.js"></script>
<script src="js/game/game-ui.js"></script>
<%@ include file="includes/footer.jsp"%>