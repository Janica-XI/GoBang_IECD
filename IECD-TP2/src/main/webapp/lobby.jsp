<%@ page import="java.util.List"%>
<%@ page import="core.Player"%>
<%@ page import="core.client.ClientUtils"%>
<%
pageContext.setAttribute("pageTitle", "Lobby");
%>
<%@ include file="includes/header.jsp"%>

<%
String successMessage = (String) session.getAttribute("successMessage");
if (successMessage != null) {
	session.removeAttribute("successMessage"); // Remove after showing
%>
<div class="alert alert-success alert-dismissible fade show mt-3"
	role="alert">
	<i class="fas fa-check-circle me-2"></i>
	<%=successMessage%>
	<button type="button" class="btn-close" data-bs-dismiss="alert"
		aria-label="Close"></button>
</div>
<%
}
%>

<main>
	<div class="container-fluid py-4">
		<div class="row justify-content-center">
			<%-- Adicionado justify-content-center para centralizar as colunas --%>
			<div class="col-lg-4 col-md-6 mb-4">
				<div class="card shadow-lg h-100 fade-in">
					<%-- shadow-lg para uma sombra mais forte, fade-in para animação --%>
					<div class="card-header text-center modern-card-header">
						<%-- Nova classe para estilizar o header --%>
						<h5 class="mb-0 text-white">Meu Perfil</h5>
					</div>
					<div class="card-body text-center">
						<div class="mb-3">
							<%
							Player loggedInPlayer = (Player) session.getAttribute("loggedInPlayer");
							String userPhotoBase64 = null;
							if (loggedInPlayer != null) {
								userPhotoBase64 = loggedInPlayer.getPhotoBase64();
							}

							if (userPhotoBase64 != null && !userPhotoBase64.isEmpty()) {
							%>
							<img src="data:image/jpeg;base64,<%=userPhotoBase64%>"
								alt="Player Photo"
								class="rounded-circle border border-3 border-player-black"
								<%-- Usando a cor player-black para a borda --%>
								style="width: 120px; height: 120px; object-fit: cover; box-shadow: var(--glow-blue);">
							<%-- Adicionando brilho --%>
							<%
							} else {
							%>
							<img src="images/default.jpg" alt="Player Photo"
								class="rounded-circle border border-3 border-player-black"
								<%-- Usando a cor player-black para a borda --%>
								style="width: 120px; height: 120px; object-fit: cover; box-shadow: var(--glow-blue);">
							<%-- Adicionando brilho --%>
							<%
							}
							%>
						</div>

						<h4 class="text-white mb-2"><%=loggedInPlayer != null ? "Bem-vindo " + loggedInPlayer.getUsername() : "Bem-vindo Visitante"%></h4>
						<%-- Usando text-white para consistência --%>

						<%
						if (loggedInPlayer != null) {
						%>
						<div class="mb-3">
							<small class="text-light d-block"> <%-- Usando text-light para visibilidade no fundo escuro --%>
								Vitórias: <%=loggedInPlayer.getVictories()%> | Derrotas: <%=loggedInPlayer.getDefeats()%>
							</small>
							<%
							if (loggedInPlayer.getTotalTime() != null) {
								long hours = loggedInPlayer.getTotalTime().toHours();
								long minutes = loggedInPlayer.getTotalTime().toMinutesPart();
								long seconds = loggedInPlayer.getTotalTime().toSecondsPart();
							%>
							<small class="text-light d-block"> <%-- Usando text-light --%>
								Tempo Total: <%=String.format("%02d:%02d:%02d", hours, minutes, seconds)%>
							</small>
							<%
							}
							%>
						</div>
						<%
						}
						%>

						<a href="profile" class="btn btn-primary btn-modern btn-sm mb-2">
							<%-- Usando btn-primary e btn-modern --%> <i
							class="fas fa-edit me-1"></i> Editar Perfil
						</a> <br>
						<button onclick="performLogout()"
							class="btn btn-danger btn-modern btn-sm">
							<i class="fas fa-sign-out-alt me-1"></i> Logout
						</button>
					</div>
				</div>
			</div>

			<div class="col-lg-8 col-md-6 mb-4">
				<%-- Adicionado mb-4 --%>
				<div class="card shadow-lg h-100 fade-in">
					<%-- shadow-lg e fade-in --%>
					<div
						class="card-header d-flex justify-content-between align-items-center modern-card-header-success">
						<%-- Nova classe para header verde --%>
						<h5 class="mb-0 text-white">Jogadores Online</h5>
						<%
						@SuppressWarnings("unchecked")
						List<Player> players = (List<Player>) request.getAttribute("players");
						int playerCount = 0;
						String currentUsername = (String) session.getAttribute("username");

						if (players != null) {
							for (Player p : players) {
								if (!p.getUsername().equals(currentUsername)) {
							playerCount++;
								}
							}
						}
						%>
						<span class="badge badge-modern badge-you fs-6" id="playerCountBadge">0</span>
						<%-- Usando as classes do tema --%>
					</div>
					<div class="card-body">
						<div
							class="mb-3 text-center border-bottom pb-3 border-secondary-subtle">
							<%-- Borda mais subtil --%>
							<%
							Boolean isReady = (Boolean) session.getAttribute("isReady");
							boolean ready = (isReady != null && isReady);
							%>
							<button onclick="toggleReady()"
								class="btn <%=ready ? "btn-success" : "btn-secondary"%> btn-modern me-2"
								<%-- Usando btn-secondary para "Ficar Pronto" --%>
									id="readyBtn">
								<i class="fas fa-<%=ready ? "check" : "play"%> me-1"></i>
								<%=ready ? "Pronto!" : "Ficar Pronto"%>
							</button>

							<a href="leaderboard" class="btn btn-secondary btn-modern"> 
								<%-- Usando btn-secondary e btn-modern --%>
								<i class="fa-solid fa-ranking-star"></i> Leaderboard
							</a>
						</div>
						<!-- Search Box -->
						<div class="mb-3">
							<div class="input-group">
								<span class="input-group-text bg-dark border-secondary">
									<i class="fas fa-search text-light"></i>
								</span>
								<input type="text" 
									   class="form-control bg-dark border-secondary text-white" 
									   id="playerSearchInput"
									   placeholder="Buscar jogadores..."
									   onkeyup="filterPlayers()" autocomplete="on">
							</div>
						</div>
						<div class="row" id="playersContainer">
							<%
							if (players != null && !players.isEmpty()) {
								boolean hasOtherPlayers = false;
								for (Player player : players) {
									if (!player.getUsername().equals(currentUsername)) {
								hasOtherPlayers = true;
								String playerPhotoBase64 = player.getPhotoBase64();
							%>
							<div class="col-md-6 col-lg-4 mb-3 fade-in">
								<%-- Adicionado fade-in --%>
								<div
									class="player-card d-flex align-items-center p-2 border rounded"
									<%-- Removido hover-shadow para usar player-card --%>
									 onclick="challengePlayer('<%=player.getUsername()%>')"
									style="cursor: pointer;">
									<%
									if (playerPhotoBase64 != null && !playerPhotoBase64.isEmpty()) {
									%>
									<img src="data:image/jpeg;base64,<%=playerPhotoBase64%>"
										alt="Player" class="rounded-circle me-3 player-avatar"
										<%-- Adicionado player-avatar --%>
										style="width: 50px; height: 50px; object-fit: cover;">
									<%
									} else {
									%>
									<img src="images/default.jpg" alt="Player"
										class="rounded-circle me-3 player-avatar"
										<%-- Adicionado player-avatar --%>
										style="width: 50px; height: 50px; object-fit: cover;">
									<%
									}
									%>
									<div class="flex-grow-1">
										<h6 class="mb-1 text-white"><%=player.getUsername()%></h6>
										<%-- text-white --%>
										<small class="text-player-black"> <%-- Usando a classe de cor de destaque --%>
											<i class="fas fa-circle me-1"></i> Online
										</small> <br> <small class="text-light"> <%-- text-light --%>
											V: <%=player.getVictories()%>, D: <%=player.getDefeats()%>
										</small>
									</div>
								</div>
							</div>
							<%
							}
							}

							if (!hasOtherPlayers) {
							%>
							<div class="col-12 text-center py-4">
								<%-- Aumentado padding para melhor visualização --%>
								<p class="text-muted">Nenhum jogador online no momento.</p>
								<button onclick="refreshPlayerList()"
									class="btn btn-secondary btn-modern btn-sm">
									<i class="fas fa-sync-alt me-1"></i> Atualizar
								</button>
							</div>
							<%
							}
							} else {
							%>
							<div class="col-12 text-center py-4">
								<p class="text-muted">Nenhum jogador online no momento.</p>
								<button onclick="refreshPlayerList()"
									class="btn btn-secondary btn-modern btn-sm">
									<i class="fas fa-sync-alt me-1"></i> Atualizar
								</button>
							</div>
							<%
							}
							%>
						</div>
						<!-- No search results message -->
						<div class="col-12 text-center py-4" id="noSearchResults" style="display: none;">
							<p class="text-muted">Nenhum jogador encontrado.</p>
						</div>
					</div>
				</div>
			</div>
		</div>
	</div>
</main>

<script>
	// Garante que o username não é nulo para evitar erros JavaScript
	window.currentUsername = '<%=(String) session.getAttribute("username") != null ? session.getAttribute("username") : ""%>';
	console.log('Username atual para JS:', window.currentUsername);
</script>

<script src="js/lobby.js"></script>

<%@ include file="includes/footer.jsp"%>