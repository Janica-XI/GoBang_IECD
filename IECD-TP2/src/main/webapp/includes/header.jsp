<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE html>
<html class="h-100">
<head>
<title>GO Bang - ${pageTitle}</title>
<link rel="icon" type="image/x-icon" href="images/favicon.ico">
<!-- Bootstrap CSS -->
<link
	href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.2/dist/css/bootstrap.min.css"
	rel="stylesheet">

<!-- Custom CSS -->
<link rel="stylesheet"
	href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
<!-- Google Fonts -->
<link
	href="https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700;800&display=swap"
	rel="stylesheet">
<link
	href="https://fonts.googleapis.com/css2?family=Slackey&display=swap"
	rel="stylesheet">

<link rel="stylesheet" href="css/main.css">
<link rel="stylesheet" href="css/responsive.css">

<%
    String currentPage = request.getRequestURI();
    String pageName = currentPage.substring(currentPage.lastIndexOf("/") + 1);
%>

<%
String pageStylesheet = (String) pageContext.getAttribute("pageStylesheet");
if (pageStylesheet != null) {
%>
<link rel="stylesheet" href="<%=pageStylesheet%>">
<%
}
%>

</head>
<body class="d-flex flex-column h-100">
	<% if ("login.jsp".equals(pageName) || "register.jsp".equals(pageName) || "lobby.jsp".equals(pageName) ) { %>
	<header class="header py-4">
		<div class="container">
			<div class="row justify-content-center">
				<div class="col-auto text-center">

					<h2 class="text-white fw-bold d-block">GO Bang Application</h2>
				</div>


			</div>
		</div>
	</header>
	<% } %>
	<!-- Error Display -->
	<%
	if (request.getAttribute("error") != null) {
	%>
	<div class="alert alert-danger" role="alert">
		<%=request.getAttribute("error")%>
	</div>
	<%
	}
	%>
	<!-- Grid Background -->
	<div class="grid-background"></div>

	<!-- Page Loader -->
	<div class="page-loader" id="pageLoader">
		<div class="loader">
			<div class="loader-rings">
				<div class="loader-ring"></div>
				<div class="loader-ring"></div>
				<div class="loader-ring"></div>
			</div>
			<div class="loader-text">Loading...</div>
		</div>
	</div>