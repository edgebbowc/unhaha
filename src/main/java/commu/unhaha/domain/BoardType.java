package commu.unhaha.domain;

public enum BoardType {
    NEW("new", "전체게시글"),
    BEST("best", "인기글"),
    BODYBUILDING("bodybuilding", "보디빌딩"),
    POWERLIFTING("powerlifting", "파워리프팅"),
    CROSSFIT("crossfit", "크로스핏");

    private final String type;
    private final String title;

    BoardType(String type, String title) {
        this.type = type;
        this.title = title;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }

    public static BoardType fromString(String type) {
        for (BoardType boardType : BoardType.values()) {
            if (boardType.type.equals(type)) {
                return boardType;
            }
        }
        throw new IllegalArgumentException("Unknown board type: " + type);
    }
}
