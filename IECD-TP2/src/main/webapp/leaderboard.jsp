<%@ page import="java.util.List"%>
<%@ page import="core.Player"%>
<%@ page import="java.time.Duration"%>
<%@ page import="java.time.format.DateTimeFormatter"%>
<%@ page import="java.util.Comparator"%>
<%
    pageContext.setAttribute("pageTitle", "Leaderboard");
    // Assume que 'allPlayers' é uma lista de objetos Player obtida do backend
    // e definida como um atributo de requisição ou sessão.
    @SuppressWarnings("unchecked")
    List<Player> allPlayers = (List<Player>) request.getAttribute("allPlayers");

    // Opcional: Se quiser ordenar os jogadores por vitórias (descendente)
    if (allPlayers != null) {
        allPlayers.sort(Comparator.comparingInt(Player::getVictories).reversed()
                                 .thenComparingInt(Player::getDefeats) // Critério secundário: derrotas (ascendente)
                                 .thenComparing(Player::getUsername)); // Critério terciário: username alfabético
    }

    // Obter o jogador logado para destacar suas estatísticas
    Player loggedInPlayer = (Player) session.getAttribute("loggedInPlayer");
%>
<%@ include file="includes/header.jsp"%>

<main>
	<div class="container py-4">
		<h2 class="text-center mb-5 fade-in">Classificação dos Jogadores</h2>

		<% if (loggedInPlayer != null) { %>
		<div class="row justify-content-center mb-5 fade-in">
			<div class="col-lg-8 col-md-10">
				<div class="info-card p-4">
					<%-- Usando a classe de tema 'info-card' --%>
					<h4 class="text-white mb-3 text-center">As Minhas Estatísticas</h4>
					<div class="row align-items-center">
						<div class="col-md-auto text-center mb-3 mb-md-0">
							<% String userPhotoBase64 = loggedInPlayer.getPhotoBase64();
                                if (userPhotoBase64 != null && !userPhotoBase64.isEmpty()) { %>
							<img src="data:image/jpeg;base64,<%= userPhotoBase64 %>"
								alt="Foto de Perfil"
								class="rounded-circle border border-3 border-player-black"
								style="width: 80px; height: 80px; object-fit: cover; box-shadow: var(--glow-blue);">
							<% } else { %>
							<img src="images/default.jpg" alt="Foto de Perfil"
								class="rounded-circle border border-3 border-player-black"
								style="width: 80px; height: 80px; object-fit: cover; box-shadow: var(--glow-blue);">
							<% } %>
						</div>
						<div class="col-md">
							<h5 class="mb-1 text-white">
								<%= loggedInPlayer.getUsername() %>
								<% if (loggedInPlayer.getNationality() != null && !loggedInPlayer.getNationality().isEmpty()) { %>
									<img src="https://flagcdn.com/w20/<%= loggedInPlayer.getNationality().toLowerCase() %>.png" 
										 alt="<%= loggedInPlayer.getNationality() %>" 
										 style="width: 20px; height: 15px; margin-left: 8px; border-radius: 2px;">
								<% } %>
							</h5>
							<p class="text-light mb-1">
								Vitórias: <strong class="text-success"><%= loggedInPlayer.getVictories() %></strong>
								| Derrotas: <strong class="text-danger"><%= loggedInPlayer.getDefeats() %></strong>
							</p>
							<%
                                if (loggedInPlayer.getTotalTime() != null) {
                                    Duration totalTime = loggedInPlayer.getTotalTime();
                                    long hours = totalTime.toHours();
                                    long minutes = totalTime.toMinutesPart();
                                    long seconds = totalTime.toSecondsPart();
                                %>
							<p class="text-light mb-0">
								Tempo Total: <strong class="text-white"><%= String.format("%02d:%02d:%02d", hours, minutes, seconds) %></strong>
							</p>
							<% } %>
						</div>
						<div class="col-md-auto text-center text-md-end mt-3 mt-md-0">
							<a href="profile" class="btn btn-primary btn-modern btn-sm">
								<i class="fas fa-edit me-1"></i> Editar Perfil
							</a>
						</div>
					</div>
				</div>
			</div>
		</div>
		<% } %>

		<div class="row justify-content-center fade-in">
			<div class="col-lg-10">
				<div class="card shadow-lg">
					<div class="card-header modern-card-header text-center">
						<h5 class="mb-0 text-white">Top Jogadores</h5>
					</div>
					<div class="card-body p-0">
						<%-- Remover padding para tabela ocupar largura total do card-body --%>
						<% if (allPlayers != null && !allPlayers.isEmpty()) { %>
						<div class="table-responsive">
							<%-- Para scroll horizontal em telas pequenas --%>
							<table class="table table-dark-purple mb-0">
								<%-- mb-0 para remover margem extra --%>
								<thead>
									<tr>
										<th scope="col" class="text-white text-center">#</th>
										<th scope="col" class="text-white">Jogador</th>
										<th scope="col" class="text-white">País</th>
										<th scope="col" class="text-white text-center">Vitórias</th>
										<th scope="col" class="text-white text-center">Derrotas</th>
										<th scope="col" class="text-white text-center">Tempo Total</th>
									</tr>
								</thead>
								<tbody>
									<%
                                        int rank = 1;
                                        for (Player player : allPlayers) {
                                            String rowClass = "";
                                            if (loggedInPlayer != null && player.getUsername().equals(loggedInPlayer.getUsername())) {
                                                rowClass = "table-info text-primary-emphasis"; // Classe Bootstrap para destacar a linha do jogador logado
                                            }
                                            Duration totalTime = player.getTotalTime();
                                            long hours = (totalTime != null) ? totalTime.toHours() : 0;
                                            long minutes = (totalTime != null) ? totalTime.toMinutesPart() : 0;
                                            long seconds = (totalTime != null) ? totalTime.toSecondsPart() : 0;
                                        %>
									<tr class="<%= rowClass %>">
										<th scope="row" class="text-center text-white"><%= rank++ %></th>
										<td class="text-white align-items-center">
											<% String playerPhotoBase64 = player.getPhotoBase64();
                                                if (playerPhotoBase64 != null && !playerPhotoBase64.isEmpty()) { %>
											<img src="data:image/jpeg;base64,<%= playerPhotoBase64 %>"
											alt="Player Photo" class="rounded-circle me-2"
											style="width: 30px; height: 30px; object-fit: cover;">
											<% } else { %> <img src="images/default.jpg" alt="Player Photo"
											class="rounded-circle me-2"
											style="width: 30px; height: 30px; object-fit: cover;">
											<% } %> 
											<%= player.getUsername() %>
										</td>
										<td class="text-white">
											<% if (player.getNationality() != null && !player.getNationality().isEmpty()) { %>
												<img src="https://flagcdn.com/w20/<%= player.getNationality().toLowerCase() %>.png" 
													 alt="<%= player.getNationality() %>" 
													 style="width: 24px; height: 18px; border-radius: 2px;">
											<% } %>
										</td>
										<td class="text-white text-center"><%= player.getVictories() %></td>
										<td class="text-white text-center"><%= player.getDefeats() %></td>
										<td class="text-white text-center"><%= String.format("%02d:%02d:%02d", hours, minutes, seconds) %></td>
									</tr>
									<% } %>
								</tbody>
							</table>
						</div>
						<% } else { %>
						<div class="p-4 text-center">
							<p class="text-muted">Nenhum dado de jogador disponível.</p>
							<p class="text-light small">Certifique-se de que a lista de
								jogadores ('allPlayers') está a ser passada para este JSP.</p>
						</div>
						<% } %>
					</div>
				</div>
			</div>
		</div>
		<div class="row justify-content-center fade-in">
		<div class="card-body text-center my-3">
			<a href="lobby.jsp" class="btn btn-secondary btn-modern"> <%-- Usando btn-secondary e btn-modern --%>
				<i class="fas fa-sync-alt me-1"></i> Voltar ao Lobby
			</a>
			</div>
		</div>

	</div>
</main>

<%@ include file="includes/footer.jsp"%>