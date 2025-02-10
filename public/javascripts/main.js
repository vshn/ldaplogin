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

document.addEventListener("DOMContentLoaded", function(event) {
    var serviceLinks = document.getElementsByTagName("a");
    for (let i = 0; i < serviceLinks.length; i++) {
        serviceLinks[i].onclick = function(event) {
            formData = new FormData();
            formData.append("csrfToken", getCsrfToken());
            formData.append("serviceId", event.target.parentElement.parentElement.id);
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
    }

    var staticPassButtons = document.getElementsByClassName("static-password");
    for (let i = 0; i < staticPassButtons.length; i++) {
        staticPassButtons[i].onclick = function(event) {
            event.target.style.display="none";
            var serviceId = event.target.parentElement.id;
            var currentStaticPassword = event.target.parentElement.getElementsByClassName("current-static-password")[0];
            var generateStaticPassword = event.target.parentElement.getElementsByClassName("generate-static-password")[0]
            if (localStorage.getItem("static-password-" + serviceId)) {
                currentStaticPassword.innerHTML = localStorage.getItem("static-password-" + serviceId);
            }
            currentStaticPassword.style.display="block";
            generateStaticPassword.style.display="block";
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
                var serviceId = event.target.parentElement.parentElement.id;
                var elements = event.target.parentElement.parentElement.getElementsByClassName("generate-static-password-confirm");
                elements[0].style.display="none";
                elements[1].style.display="none";
                event.target.parentElement.parentElement.getElementsByClassName("current-static-password")[0].innerHTML = body.pw;
                localStorage.setItem("static-password-" + serviceId, body.pw);
                return false;
            }).catch(error => {
                console.log(error);
            });
            return false;
        }
    }
});
