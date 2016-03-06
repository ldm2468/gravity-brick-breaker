package ldm2468.swipe;

public interface GooglePlay {
    void submitScore(String leaderboardId, int score);

    void unlockAchievement(String achievementId);

    void showLeaderboards();

    void showAchievements();
}
