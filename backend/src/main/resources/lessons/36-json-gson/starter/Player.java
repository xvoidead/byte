public record Player(String name, int score) {

    /** Тот же игрок с добавленными очками: record не меняется, создаём новый. */
    public Player addScore(int points) {
        return new Player(name, score + points);
    }
}
