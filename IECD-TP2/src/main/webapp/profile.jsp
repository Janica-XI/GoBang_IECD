<%@ page import="core.Player"%>
<%
pageContext.setAttribute("pageTitle", "Editar Perfil");
Player currentPlayer = (Player) session.getAttribute("loggedInPlayer");
%>
<%@ include file="includes/header.jsp"%>
<main>
	<div class="container py-4">
		<h2 class="text-center fade-in">Edição de Perfil</h2>
	</div>
	<div
		class="container-fluid d-flex align-items-center justify-content-center">
		<div class="row w-100 justify-content-center">
			<div class="col-md-10 col-lg-8">
				<div class="card shadow">
					<div class="card-body p-5">
						<div class="row">
							<!-- Current Photo Display -->
							<div class="col-md-12 mb-3 text-center">
								<%
								// Show current photo if available
								if (currentPlayer != null && currentPlayer.getPhotoBase64() != null && !currentPlayer.getPhotoBase64().isEmpty()) {
								%>
								<img
									src="data:image/jpeg;base64,<%=currentPlayer.getPhotoBase64()%>"
									alt="Foto de Perfil"
									class="rounded-circle border border-3 border-primary"
									style="width: 120px; height: 120px; object-fit: cover;"
									id="profilePhotoPreview">
								<%
								} else {
								%>
								<img src="images/default.jpg" alt="Foto de Perfil"
									class="rounded-circle border border-3 border-primary"
									style="width: 120px; height: 120px; object-fit: cover;"
									id="profilePhotoPreview">
								<%
								}
								%>
								<div class="mt-2">
									<small class="text-muted">Foto atual do perfil</small>
								</div>
							</div>
						</div>

						<!-- Edit Profile Form -->
						<form action="profile" method="post" enctype="multipart/form-data"
							id="editProfileForm">
							<div class="row">
								<!-- Photo Upload Field -->
								<div class="col-md-12 mb-2">
									<label for="profilePhoto" class="form-label">Nova Foto
										de Perfil:</label> <input type="file" class="form-control"
										id="profilePhoto" name="profilePhoto" accept="image/*"
										onchange="previewPhoto(this)"> <small
										class="text-muted">Formatos: JPG, PNG, GIF (máx. 5MB)</small>
								</div>
							</div>
							<div class="row">
								<!-- Username Field (Read-only) -->
								<div class="col-md-6 mb-2">
									<label for="username" class="form-label">Nome de
										utilizador:</label> <input type="text" class="form-control"
										id="username" name="username"
										value="<%=currentPlayer != null ? currentPlayer.getUsername() : ""%>"
										readonly style="background-color: #f8f9fa;"> <small
										class="text-muted">O nome de utilizador não pode ser
										alterado</small>
								</div>
								<!-- Date of Birth Field -->
								<div class="col-md-6 mb-3">
									<label for="dateOfBirth" class="form-label">Data de
										Nascimento:</label> <input type="date" class="form-control"
										id="dateOfBirth" name="dateOfBirth"
										value="<%=currentPlayer != null && currentPlayer.getDateOfBirth() != null
		? currentPlayer.getDateOfBirth().toString()
		: ""%>">
								</div>

							</div>
							<div class="row">
								<!-- New Password Field -->
								<div class="col-md-6 mb-3">
									<label for="newPassword" class="form-label">Nova
										Palavra-Passe:</label> <input type="password" class="form-control"
										id="newPassword" name="newPassword"
										placeholder="Deixe vazio para manter a atual">
									<div class="invalid-feedback" id="passwordError"
										style="display: none;">A palavra-passe deve ter entre 8
										e 16 caracteres.</div>
								</div>

								<!-- Confirm Password Field -->
								<div class="col-md-6 mb-3">
									<label for="confirmPassword" class="form-label">Confirmar
										Nova Palavra-Passe:</label> <input type="password"
										class="form-control" id="confirmPassword"
										name="confirmPassword"
										placeholder="Confirme a nova palavra-passe">
									<div class="invalid-feedback" id="confirmPasswordError"
										style="display: none;">As palavras-passe não coincidem.
									</div>
								</div>
							</div>

							<div class="row">
								<!-- Nationality Field -->
								<div class="col-md-3">
									<label for="nationality" class="form-label">Nacionalidade:</label>
									<div class="nationality-container">
										<div class="nationality-input">
											<input type="text" id="nationality" name="nationality"
												list="nationalities" autocomplete="off"
												value="<%=currentPlayer != null ? currentPlayer.getNationality() : ""%>">
											<datalist id="nationalities"></datalist>
										</div>
									</div>
								</div>

								<div class="col-md-3">
									<div class="form-label" style="visibility: hidden;">Hidden
										Label</div>
									<div id="selectedNationality" class="nationality-display">
									</div>
								</div>
								<!-- Theme Selector -->
								<div class="col-md-6 mb-3">
									<label class="form-label fw-bold">Tema de jogo:</label>

									<div class="theme-selector">
										<div class="theme-option">
											<input type="radio" id="theme-purple" name="theme"
												value="purple"
												<%=currentPlayer != null && "default".equals(currentPlayer.getTheme()) ? "checked" : ""%>>
											<div class="theme-square theme-purple"></div>
											<div class="theme-label">Padrão</div>
										</div>
										<div class="theme-option">
											<input type="radio" id="theme-default" name="theme"
												value="default"
												<%=currentPlayer != null && "light".equals(currentPlayer.getTheme()) ? "checked" : ""%>>
											<div class="theme-square theme-default"></div>
											<div class="theme-label">Claro</div>
										</div>

										<div class="theme-option">
											<input type="radio" id="theme-dark" name="theme" value="dark"
												<%=currentPlayer != null && "dark".equals(currentPlayer.getTheme()) ? "checked" : ""%>>
											<div class="theme-square theme-dark"></div>
											<div class="theme-label">Escuro</div>
										</div>

										<div class="theme-option">
											<input type="radio" id="theme-blue" name="theme" value="blue"
												<%=currentPlayer != null && "blue".equals(currentPlayer.getTheme()) ? "checked" : ""%>>
											<div class="theme-square theme-blue"></div>
											<div class="theme-label">Azul</div>
										</div>

										<div class="theme-option">
											<input type="radio" id="theme-green" name="theme"
												value="green"
												<%=currentPlayer != null && "green".equals(currentPlayer.getTheme()) ? "checked" : ""%>>
											<div class="theme-square theme-green"></div>
											<div class="theme-label">Verde</div>
										</div>

									</div>


								</div>

							</div>


							<div class="row">
								<div class="col-md-6 mb-3">
									<button type="submit" class="btn btn-primary w-100">
										<i class="fas fa-save"></i> Guardar Alterações
									</button>
								</div>
								<div class="col-md-6 mb-3">
									<a href="lobby" class="btn btn-secondary w-100"> <i
										class="fas fa-arrow-left"></i> Voltar ao Lobby
									</a>
								</div>
							</div>

						</form>
					</div>
				</div>
			</div>
		</div>
	</div>
</main>

<script src="js/nationalities.js"></script>
<script src="js/validation/profile.js"></script>
<script src="js/profile-custom.js"></script>
<script>
//Debug current player data
console.log('Current player data loaded');
console.log('Username:', '<%=currentPlayer != null ? currentPlayer.getUsername() : "null"%>');
console.log('Nationality:', '<%=currentPlayer != null ? currentPlayer.getNationality() : "null"%>');
console.log('Date of Birth:', '<%=currentPlayer != null && currentPlayer.getDateOfBirth() != null
		? currentPlayer.getDateOfBirth().toString()
		: "null"%>
	');
</script>

<%@ include file="includes/footer.jsp"%>