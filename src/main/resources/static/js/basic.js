const toggleBtn = document.querySelector("header #toggleBtn");
const navContainer = document.querySelector("header #nav");
const navBackground = document.querySelector("header #nav .navBackground");
if (toggleBtn) {
    toggleBtn.addEventListener("click", () => {
        navContainer
            .classList
            .toggle("active");
        togglePtr();
    });
}
if (navBackground) {
    navBackground.addEventListener("click", () => {
        navContainer
            .classList
            .toggle("active");
        togglePtr();
    });
}