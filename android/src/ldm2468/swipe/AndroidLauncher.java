package ldm2468.swipe;

import android.content.Intent;
import android.os.Bundle;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.GameHelper;

public class AndroidLauncher extends AndroidApplication implements GooglePlay, GameHelper.GameHelperListener {
    private GameHelper gameHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        config.numSamples = 2;
        initialize(new Main(), config);
        gameHelper = new GameHelper(this, GameHelper.CLIENT_GAMES);
        gameHelper.enableDebugLog(true);
        gameHelper.setup(this);
        Main.googlePlay = this;
    }

    @Override
    public void onStart() {
        super.onStart();
        gameHelper.onStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        gameHelper.onStop();
    }

    @Override
    public void onActivityResult(int request, int response, Intent data) {
        super.onActivityResult(request, response, data);
        gameHelper.onActivityResult(request, response, data);
    }

    @Override
    public void submitScore(String leaderboardId, int score) {
        if (gameHelper.isSignedIn())
            Games.Leaderboards.submitScore(gameHelper.getApiClient(), leaderboardId, score);
    }

    @Override
    public void unlockAchievement(String achievementId) {
        if (gameHelper.isSignedIn())
            Games.Achievements.unlock(gameHelper.getApiClient(), achievementId);
    }

    @Override
    public void showLeaderboards() {
        if (gameHelper.isSignedIn())
            startActivityForResult(Games.Leaderboards.getAllLeaderboardsIntent(gameHelper.getApiClient()), 123);
    }

    @Override
    public void showAchievements() {
        if (gameHelper.isSignedIn())
            startActivityForResult(Games.Achievements.getAchievementsIntent(gameHelper.getApiClient()), 123);
    }

    @Override
    public void onSignInFailed() {
    }

    @Override
    public void onSignInSucceeded() {
    }
}
