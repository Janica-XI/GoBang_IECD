
<%
pageContext.setAttribute("pageTitle", "Register");
%>
<%@ include file="includes/header.jsp"%>
<main id="registration">
	<div
		class="container-fluid d-flex align-items-center justify-content-center">
		<div class="row w-100 justify-content-center">
			<div class="col-md-8 col-lg-6">
				<div class="card shadow">
					<div class="card-body p-5 pb-4">
						<!-- Header -->
						<div class="text-center mb-4">
							<h5 class="card-subtitle text-muted">Faça o Registo</h5>
						</div>

						<!-- Registration Form -->
						<form action="register" method="post">
							<div class="row">
								<!-- Username Field -->
								<div class="col-md-6 mb-3">
									<label for="username" class="form-label">Nome de
										utilizador:</label> <input type="text" class="form-control"
										id="username" name="username"
										placeholder="Insira o nome de utilizador" required>
									<div class="invalid-feedback" id="usernameError"
										style="display: none;">Campo obrigatório.</div>
								</div>

								<!-- Birthdate Field -->
								<div class="col-md-6 mb-3">
									<label for="dateOfBirth" class="form-label">Data de
										Nascimento:</label> <input type="date" class="form-control"
										id="dateOfBirth" name="dateOfBirth" required>
									<div class="invalid-feedback" id="dateOfBirthError"
										style="display: none;">Campo obrigatório.</div>
								</div>
							</div>

							<div class="row">
								<!-- Password Field -->
								<div class="col-md-6 mb-3">
									<label for="password" class="form-label">Palavra-Passe:</label>
									<input type="password" class="form-control" id="password"
										name="password" placeholder="Insira a palavra-passe" required>
									<div class="invalid-feedback" id="passwordError"
										style="display: none;">Campo obrigatório.</div>
								</div>

								<!-- Password Confirmation Field -->
								<div class="col-md-6 mb-3">
									<label for="passwordConfirmation" class="form-label">Confirme
										a Palavra-Passe:</label> <input type="password" class="form-control"
										id="passwordConfirmation" name="passwordConfirmation"
										placeholder="Confirme a palavra-passe" required>
									<div class="invalid-feedback" id="passwordConfirmationError"
										style="display: none;">As palavras-passe não
										correspondem.</div>
								</div>
							</div>

							<!-- Nationality Field -->
							<div class="row">
								<div class="col-md-6">
									<label for="nationality" class="form-label">Nacionalidade:</label>
									<div class="nationality-container">
										<div class="nationality-input">
											<input type="text" id="nationality" name="nationality"
												list="nationalities" autocomplete="off">
											<datalist id="nationalities"></datalist>
										</div>
									</div>
								</div>

								<div class="col-md-6">
									<div class="form-label" style="visibility: hidden;">Hidden
										Label</div>
									<div id="selectedNationality" class="nationality-display">
									</div>
								</div>
							</div>

							<!-- Submit Button -->
							<div class="px-5 d-grid mb-3">
								<button type="submit" class="btn btn-success btn-lg">
									Fazer Registo</button>
							</div>

							<!-- Back to Login Link -->
							<div class="text-center">
								<p class="mb-3">
									Já tem conta? <a href="login" class="text-decoration-none">
										Voltar ao Login </a>
								</p>
							</div>
						</form>
					</div>
				</div>
			</div>
		</div>
	</div>
</main>
<script src="js/nationalities.js"></script>
<script src="js/validation/registration.js"></script>
<%@ include file="includes/footer.jsp"%>