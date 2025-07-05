package commu.unhaha.domain;

public enum BoardType {
    NEW("new", "전체게시글", "/new"),
    BEST("best", "인기글", "/best"),
    BODYBUILDING("bodybuilding", "보디빌딩", "/bodybuilding"),
    POWERLIFTING("powerlifting", "파워리프팅", "/powerlifting"),
    CROSSFIT("crossfit", "크로스핏", "/crossfit"),
    HUMOR("humor", "유머", "/humor");

    private final String type;
    private final String title;
    private final String path;

    BoardType(String type, String title, String path) {
        this.type = type;
        this.title = title;
        this.path = path;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getPath() {return path; }

    public static BoardType fromString(String type) {
        for (BoardType boardType : BoardType.values()) {
            if (boardType.type.equals(type)) {
                return boardType;
            }
        }
        throw new IllegalArgumentException("Unknown board type: " + type);
    }

    public static String titleToPath(String title) {
        if (title == null) {
            throw new IllegalArgumentException("Board type cannot be null");
        }
        for (BoardType boardType : BoardType.values()) {
            if (boardType.title.equals(title)) {
                return boardType.path;
            }
        }
        throw new IllegalArgumentException("Unknown board title: " + title);
    }
}
