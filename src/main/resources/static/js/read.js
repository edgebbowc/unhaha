const articleButtons = document.querySelector(".right .buttons");
const articlePopup = document.querySelector(".right .popUp");
if (articleButtons) {
    articleButtons.addEventListener("click", () => {
        articlePopup.classList.toggle("active");
    });
    document.addEventListener("click", (event) => {
        if (!articleButtons.contains(event.target)) {
            articlePopup.classList.remove("active");
        }
    });
}

const authorNickName = document.querySelector("article .nickName");
const authorPopup = document.querySelector("article > .item .userPopup");
if (authorNickName) {
    authorNickName.addEventListener("click", () => {
        authorPopup.classList.toggle("active");
    });
    document.addEventListener("click", (event) => {
        if (!authorNickName.contains(event.target)) {
            authorPopup?.classList.remove("active");
        }
    });
}
const deletePopup = document.querySelector("article .popUp .deletePopup");
const removeArticle = document.querySelector("#modal .removeArticle");
if (deletePopup) {
    deletePopup.addEventListener("click", () => {
        modal.create(removeArticle);
    });
}
const articleReportBtn = document.querySelector("article .popUp .report");
if (articleReportBtn) {
    articleReportBtn.addEventListener("click", () => {
        reportType.value = "article";
        reportId.value = articleId;
        modal.create(reportContainer);
    });
}
const shareBtn = document.querySelector("#share");
const sharePopup = document.querySelector("#modal .share");
if (shareBtn) {
    shareBtn.addEventListener("click", () => {
        modal.create(sharePopup);
    });
}
const blockUserBtn = document.querySelector("article .popUp .block");
if (blockUserBtn) {
    blockUserBtn.addEventListener("click", async (event) => {
        event.preventDefault();
        await blockUser(articleUserId);
    });
}
const unblockUserBtn = document.querySelector("article .popUp .unblock");
if (unblockUserBtn) {
    unblockUserBtn.addEventListener("click", async (event) => {
        event.preventDefault();
        await unblockUser(articleUserId);
    });
}
const banBtn = document.querySelector("article .popUp .ban");
if (banBtn) {
    banBtn.addEventListener("click", (event) => {
        event.preventDefault();
        banId.value = articleUserId;
        banType.value = "article";
        banContentId.value = articleId;
        modal.create(banContainer);
    });
}
const userBanBtn = document.querySelector("article .userPopup .ban");
if (userBanBtn) {
    userBanBtn.addEventListener("click", (event) => {
        event.preventDefault();
        banId.value = articleUserId;
        banType.value = "article";
        banContentId.value = articleId;
        modal.create(banContainer);
    });
}
const kakaoJavascriptKey = document.querySelector("#modal .share .kakaoTalk")?.id;
if (kakaoJavascriptKey) {
    const title = document.querySelector("input[name=title]").value;
    const description = document.querySelector("input[name=ogContent]").value;
    const imageUrl = document.querySelector("input[name=ogImage]").value;
    const url = document.querySelector("input[name=url]").value;
    Kakao.init(kakaoJavascriptKey);
    const kakaoLink = () => {
        Kakao.Share.createDefaultButton({
            container: "#modal .share .kakaoTalk",
            objectType: "feed",
            content: {title, description, imageUrl, link: {mobileWebUrl: url, webUrl: url,},},
        });
    };
    kakaoLink();
}
const copyToClipboard = (val) => {
    const t = document.createElement("textarea");
    document.body.appendChild(t);
    t.value = val;
    t.select();
    document.execCommand("copy");
    document.body.removeChild(t);
    alert("링크가 클립보드에 복사되었습니다");
    modal.remove(sharePopup);
};
const scrapButton = document.querySelector("#scrap");
if (scrapButton) {
    const listener = async () => {
        const result = await scrap(articleId);
        if (result.status) {
            if (scrapButton.className === "scrap") {
                scrapButton.innerHTML = `<span>스크랩 취소</span><span><i class="fa-solid fa-bookmark"></i></span>`;
                scrapButton.className = `scrap scraped`;
            } else {
                scrapButton.innerHTML = `<span>스크랩</span><span><i class="fa-regular fa-bookmark"></i></span>`;
                scrapButton.className = `scrap`;
            }
        } else {
            alert(result.message);
        }
    };
    scrapButton.addEventListener("click", listener);
}
const disabledLikeButton = document.querySelector("#disabledLike");
if (disabledLikeButton) {
    disabledLikeButton.addEventListener("click", async () => {
        alert("3일이 지난 게시물은 침하하 할 수 없습니다.");
    });
}
const scrap = async (articleId) => {
    try {
        showLoading();
        const {data} = await axios.post("/api/scrap", {articleId});
        hideLoading();
        return data;
    } catch (e) {
        console.error(e);
        throw e;
    }
};
const like = async (articleId) => {
    try {
        showLoading();
        const {data} = await axios.post("/api/like", {articleId});
        hideLoading();
        return data;
    } catch (e) {
        console.error(e);
        throw e;
    }
};
if (window.matchMedia("(display-mode: standalone)").matches) {
    const images = document.querySelectorAll(".content img");
    if (images.length) {
        images.forEach((image) => image.addEventListener("click", (event) => {
            window.open(event.currentTarget.src, "_blank");
        }),);
    }
}