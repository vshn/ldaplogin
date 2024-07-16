function getCsrfToken() {
    let cookies = document.cookie.split(";");
    for (let i = 0; i < cookies.length; i++) {
        const [key, ...v] = cookies[i].trim().split('=');
        if (key == "csrfToken") {
            return decodeURIComponent(v.join('='));
        }
    }
    return "";
}

document.addEventListener("DOMContentLoaded", function(event) {
    var services = document.getElementsByTagName("a");
    for (let i = 0; i < services.length; i++) {
        services[i].onclick = function(event) {
            formData = new FormData();
            formData.append("csrfToken", getCsrfToken());
            formData.append("serviceId", event.target.id);
            let options = {
                method: 'POST',
                body: new URLSearchParams(formData)
            };
            fetch ('/pwgen', options)
            .then(response => response.json())
            .then(body => {
                navigator.clipboard.writeText(body.pw);
                window.location.href=event.target.href;
            }).catch(error => {
                console.log(error);
            });
            return false;
        }
    }
});
