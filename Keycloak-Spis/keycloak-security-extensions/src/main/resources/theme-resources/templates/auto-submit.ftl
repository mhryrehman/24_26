<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Redirecting...</title>
</head>
<body>
    <form id="autoForm" method="post" action="${url.loginAction}">
        <input type="hidden" name="auto" value="true" />
    </form>

    <script>
        document.getElementById("autoForm").submit();
    </script>
</body>
</html>
