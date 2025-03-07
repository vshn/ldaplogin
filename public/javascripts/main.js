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

async function writeClipboardText(text) {
    try {
        await navigator.clipboard.writeText(text);
    } catch (error) {
        console.error(error.message);
    }
}

function showPwNotAvailable(serviceElement) {
    serviceElement.getElementsByClassName("current-static-password-unavailable")[0].style.display = "block";
    serviceElement.getElementsByClassName("static-password")[0].style.display = "none";
    serviceElement.getElementsByClassName("generate-static-password")[0].style.display = "block";
}

function showPw(serviceElement, pw) {
    serviceElement.getElementsByClassName("generate-static-password")[0].style.display="none";
    serviceElement.getElementsByClassName("current-static-password-unavailable")[0].style.display="none";
    var elements = serviceElement.getElementsByClassName("generate-static-password-confirm");
    elements[0].style.display="none";
    elements[1].style.display="none";

    serviceElement.getElementsByClassName("current-static-password-manager")[0].style.display="block";
    serviceElement.getElementsByClassName("current-static-password")[0].innerHTML = pw;
    serviceElement.getElementsByClassName("current-static-password")[0].style.display="block";
}

document.addEventListener("DOMContentLoaded", function(event) {
    var serviceButtons = document.getElementsByClassName("serviceButton");
    for (let i = 0; i < serviceButtons.length; i++) {
        if (serviceButtons[i].classList.contains("dynamicPassword")) {
            serviceButtons[i].onclick = function(event) {
                var serviceId = event.target.parentElement.parentElement.id;
                formData = new FormData();
                formData.append("csrfToken", getCsrfToken());
                formData.append("serviceId", serviceId);
                let options = {
                    method: 'POST',
                    body: new URLSearchParams(formData)
                };
                fetch ('/dynamicPwGen', options)
                .then(response => response.json())
                .then(body => {
                    writeClipboardText(body.pw);
                    window.location.href=event.target.href;
                }).catch(error => {
                    console.log(error);
                });
                return false;
            }
        } else {
            serviceButtons[i].onclick = function(event) {
                var serviceId = event.target.parentElement.parentElement.id;
                var pw = localStorage.getItem("static-password-" + serviceId);
                formData = new FormData();
                formData.append("csrfToken", getCsrfToken());
                formData.append("serviceId", serviceId);
                formData.append("pw", pw);
                let options = {
                    method: 'POST',
                    body: new URLSearchParams(formData)
                };
                fetch ('/staticPwCheck', options)
                .then(response => response.json())
                .then(body => {
                    if (body.check) {
                        writeClipboardText(pw);
                        window.location.href=event.target.href;
                    } else {
                        showPwNotAvailable(event.target.parentElement.parentElement);
                    }
                }).catch(error => {
                    console.log(error);
                });
                return false;
            }
        }
    }

    var staticPassButtons = document.getElementsByClassName("static-password");
    for (let i = 0; i < staticPassButtons.length; i++) {
        staticPassButtons[i].onclick = function(event) {
            event.target.style.display="none";
            var serviceId = event.target.parentElement.id;
            var generateStaticPassword = event.target.parentElement.getElementsByClassName("generate-static-password")[0];
            var pw = localStorage.getItem("static-password-" + serviceId);
            if (pw) {
                // we need to verify if the static password is still valid. We don't want to show invalid passwords to avoid confusion.
                formData = new FormData();
                formData.append("csrfToken", getCsrfToken());
                formData.append("serviceId", serviceId);
                formData.append("pw", pw);
                let options = {
                    method: 'POST',
                    body: new URLSearchParams(formData)
                };
                fetch ('/staticPwCheck', options)
                .then(response => response.json())
                .then(body => {
                    if (body.check) {
                        showPw(event.target.parentElement, pw);
                    } else {
                        showPwNotAvailable(event.target.parentElement);
                    }
                    generateStaticPassword.style.display="block";
                }).catch(error => {
                    console.log(error);
                });
            } else {
                showPwNotAvailable(event.target.parentElement);
            }
        }
    }

    var generatePassButton = document.getElementsByClassName("generate-static-password-link");
    for (let i = 0; i < generatePassButton.length; i++) {
        generatePassButton[i].onclick = function(event) {
            var elements = event.target.parentElement.parentElement.getElementsByClassName("generate-static-password-confirm");
            elements[0].style.display="block";
            elements[1].style.display="block";
            return false;
        }
    }

    var noGeneratePassButton = document.getElementsByClassName("generate-static-password-confirm-no");
    for (let i = 0; i < noGeneratePassButton.length; i++) {
        noGeneratePassButton[i].onclick = function(event) {
            var elements = event.target.parentElement.parentElement.getElementsByClassName("generate-static-password-confirm");
            elements[0].style.display="none";
            elements[1].style.display="none";
            return false;
        }
    }

    var yesGeneratePassButton = document.getElementsByClassName("generate-static-password-confirm-yes");
    for (let i = 0; i < yesGeneratePassButton.length; i++) {
        yesGeneratePassButton[i].onclick = function(event) {
            formData = new FormData();
            formData.append("csrfToken", getCsrfToken());
            formData.append("serviceId", event.target.parentElement.parentElement.id);
            let options = {
                method: 'POST',
                body: new URLSearchParams(formData)
            };
            fetch ('/staticPwGen', options)
            .then(response => response.json())
            .then(body => {
                localStorage.setItem("static-password-" + event.target.parentElement.parentElement.id, body.pw);
                showPw(event.target.parentElement.parentElement, body.pw);
                return false;
            }).catch(error => {
                console.log(error);
            });
            return false;
        }
    }
});
