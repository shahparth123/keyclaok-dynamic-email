function togglePassword(event) {
    var img = event.src;
    if (img.indexOf('eye.svg') != -1) {
        event.setAttribute('src', img.replace('eye.svg', 'eye-off.svg'));
        event.nextElementSibling.setAttribute('type', 'text');
    } else {
        event.setAttribute('src', img.replace('eye-off.svg', 'eye.svg'));
        event.nextElementSibling.setAttribute('type', 'password');
    }
}

//var resetPasswordPageInterval = 0;
//var urlParams = new URLSearchParams(window.location.search);
//urlParams = Object.fromEntries(urlParams.entries());
//var token = urlParams['key'];
//function findResetPageAndSetRedirection() {
//   if (token) {
//    localStorage.setItem("mavq_action_token", token);
//   } else {
//    token = localStorage.getItem("mavq_action_token") || urlParams['key'];
//   }
//   var redirectTag = document.getElementById("remember_password_redirection");
//   if (redirectTag) {
//     var tokenData = parseJwt(token);
//    redirectTag.href = tokenData.reduri;
//    clearInterval(resetPasswordPageInterval);
//   }
//};
//
//function parseJwt (token) {
//    var base64Url = token.split('.')[1];
//    var base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
//    var jsonPayload = decodeURIComponent(atob(base64).split('').map(function(c) {
//        return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
//    }).join(''));
//
//    return JSON.parse(jsonPayload);
//};
//
//resetPasswordPageInterval = setInterval(function(){ findResetPageAndSetRedirection() }, 500);