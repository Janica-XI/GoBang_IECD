
<%
pageContext.setAttribute("pageTitle", "Login");
%>

<%@ include file="includes/header.jsp"%>
<main id="login">
	<div
		class="container-fluid d-flex align-items-center justify-content-center">
		<div class="row w-100 justify-content-center">
			<div class="col-md-6 col-lg-4">
				<div class="card shadow">
					<div class="card-body p-5">
						<!-- Header -->
						<div class="text-center mb-4">
							<h5 class="card-subtitle text-muted">Faça o Início de Sessão</h5>
						</div>

						<!-- Login Form -->
						<form action="login" method="post">
							<!-- Username Field -->
							<div class="mb-3">
								<label for="username" class="form-label">Nome de
									utilizador:</label> <input type="text" class="form-control"
									id="username" name="username"
									placeholder="Insira o nome de utilizador" required>
								<div class="invalid-feedback" id="usernameError"
									style="display: none;">Tem de preencher com username.</div>
							</div>

							<!-- Password Field -->
							<div class="mb-4">
								<label for="password" class="form-label">Palavra-Passe:</label>
								<input type="password" class="form-control" id="password"
									name="password" placeholder="Insira a palavra-passe" required>
								<div class="invalid-feedback" id="passwordError"
									style="display: none;">Tem de preencher com
									palavra-passe.</div>
							</div>

							<!-- Submit Button -->
							<div class="d-grid">
								<button type="submit" class="btn btn-primary btn-lg">
									Iniciar Sessão</button>
							</div>

							<!-- Registration Link -->
							<div class="text-center mt-3">
								<p class="mb-0">
									Não tem conta? <a href="register" class="text-decoration-none">
										Registar </a>
								</p>
							</div>
						</form>

					</div>
				</div>
			</div>
		</div>
	</div>
</main>
<script src="js/validation/login.js">></script>
<%@ include file="includes/footer.jsp"%>