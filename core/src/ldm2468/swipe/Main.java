package ldm2468.swipe;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {
    public static SwipeManager swipeManager;
    ShapeRenderer sr;
    SpriteBatch batch;
    BrickGrid brickGrid;
    Viewport viewport;
    static final int WIDTH = 7, HEIGHT = 12;
    int i = 0;

    @Override
    public void create() {
        swipeManager = new SwipeManager();
        (brickGrid = new BrickGrid(WIDTH, HEIGHT)).loadSave();
        (viewport = new ScreenViewport()).apply();
        (sr = new ShapeRenderer()).setProjectionMatrix(viewport.getCamera().combined);
        (batch = new SpriteBatch()).setProjectionMatrix(viewport.getCamera().combined);
        Gdx.input.setInputProcessor(swipeManager);
        Gdx.graphics.setContinuousRendering(false);
        Gdx.graphics.requestRendering();
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(1, 1, 1, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        //skip (debug)
        if (Gdx.input.isKeyJustPressed(Input.Keys.S))
            brickGrid.push();
        //process swipe
        if (swipeManager.justDragged() && !brickGrid.isRunning)
            brickGrid.swipe((swipeManager.x2 - swipeManager.x1) / 2f, (swipeManager.y2 - swipeManager.y1) / 2f);

        //draw
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        sr.begin(ShapeRenderer.ShapeType.Filled);
        int screenSize = Math.min(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        brickGrid.draw(sr, Gdx.graphics.getWidth() / 2 - screenSize / 2, Gdx.graphics.getWidth() / 2 + screenSize / 2,
                Gdx.graphics.getHeight() / 2 - screenSize / 2, Gdx.graphics.getHeight() / 2 + screenSize / 2);

        //process drag
        brickGrid.fastForward = false;
        if (swipeManager.isDragging()) {
            if (brickGrid.isRunning) {
                brickGrid.fastForward = true;
            } else {
                sr.setColor(100f / 255, 0.5f, 1, 0.3f);
                if ((swipeManager.y2 - swipeManager.y1) / 2f < 40) {
                    sr.setColor(1, 0.5f, 100f / 255f, 0.3f);
                }
                sr.circle(swipeManager.x1, swipeManager.y1, 4);
                sr.rectLine(swipeManager.x1, swipeManager.y1, swipeManager.x2, swipeManager.y2, 4);
                brickGrid.drawSimulation(sr, Gdx.graphics.getWidth() / 2 - screenSize / 2, Gdx.graphics.getWidth() / 2 + screenSize / 2,
                        Gdx.graphics.getHeight() / 2 - screenSize / 2, Gdx.graphics.getHeight() / 2 + screenSize / 2,
                        (swipeManager.x2 - swipeManager.x1) / 2f, (swipeManager.y2 - swipeManager.y1) / 2f);
            }
        } else if (!brickGrid.isRunning) {
            Gdx.graphics.setContinuousRendering(false);
        }

        sr.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
        batch.begin();
        brickGrid.drawText(batch, Gdx.graphics.getWidth() / 2 - screenSize / 2, Gdx.graphics.getWidth() / 2 + screenSize / 2,
                Gdx.graphics.getHeight() / 2 - screenSize / 2, Gdx.graphics.getHeight() / 2 + screenSize / 2);
        batch.end();

        //update
        brickGrid.update();
        if (brickGrid.bricks.size == brickGrid.maxHeight) {
            for (int i : brickGrid.bricks.get(0)) {
                if (i > 0) {
                    //die
                    brickGrid = new BrickGrid(WIDTH, HEIGHT);
                    brickGrid.push();
                }
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        sr.setProjectionMatrix(viewport.getCamera().combined);
        batch.setProjectionMatrix(viewport.getCamera().combined);
    }
}
