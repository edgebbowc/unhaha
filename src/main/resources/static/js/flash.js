const flash = document.querySelector("#flash");
if (flash) {
    const flashClose = flash.querySelector(".close");
    flashClose.addEventListener("click", () => {
        flash
            .classList
            .toggle("active");
    });
    setTimeout(() => {
        flash.style.display = "none";
    }, 5000);
}