package ldm2468.swipe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;

public class SwipeManager implements InputProcessor {
    public float x1 = 0, y1 = 0, x2 = 0, y2 = 0;
    boolean dragging = false, justDragged = false;

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        x1 = x2 = screenX;
        y1 = y2 = Gdx.graphics.getHeight() - screenY;
        dragging = true;
        return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!dragging) return false;
        x2 = screenX;
        y2 = Gdx.graphics.getHeight() - screenY;
        justDragged = true;
        dragging = false;
        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        x2 = screenX;
        y2 = Gdx.graphics.getHeight() - screenY;
        return true;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(int amount) {
        return false;
    }

    public boolean isDragging() {
        return dragging;
    }

    public boolean justDragged() {
        boolean b = justDragged;
        justDragged = false;
        return b;
    }

    public void unDrag() {
        dragging = false;
        justDragged = false;
    }
}
