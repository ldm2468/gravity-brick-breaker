package ldm2468.swipe;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.Contact;
import com.badlogic.gdx.physics.box2d.ContactImpulse;
import com.badlogic.gdx.physics.box2d.ContactListener;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.Manifold;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.physics.box2d.World;
import com.badlogic.gdx.utils.Array;

public class BrickGrid {
    final float WORLD_WIDTH = 80, WORLD_HEIGHT = 80, BALL_RADIUS = 1.5f, BONUS_RADIUS = 1.7f, G = -260;
    final int C_FILTER = 0x001, B_FILTER = 0x010, W_FILTER = 0x100;
    public Array<int[]> bricks = new Array<int[]>();
    public boolean isRunning = false, fastForward = false;
    public World world;
    int width, maxHeight, max = 0, exp = 0;
    Array<Body> circles = new Array<Body>(false, 50);
    Array<Body> toDestroy = new Array<Body>(false, 10);
    int ballNum = 1;
    int ballsLeft = 1;
    float circleStart = 40, oldCircleStart = 40;
    float swipeX = 0, swipeY = 0;
    BitmapFont font;
    Preferences highScore = Gdx.app.getPreferences("highScore"), current = Gdx.app.getPreferences("current");
    int highestMax = 0, highestExp = 0;
    int simulationHit = 2;
    ContactListener defaultContactListener = new ContactListener() {
        @Override
        public void beginContact(Contact contact) {
            if (toDestroy.contains(contact.getFixtureA().getBody(), true) ||
                        toDestroy.contains(contact.getFixtureB().getBody(), true))
                return;
            Fixture correct = contact.getFixtureA(), wrong = contact.getFixtureB();
            if (correct.getUserData() == null) {
                wrong = correct;
                correct = contact.getFixtureB();
            }
            if (correct.getUserData() == null)
                return;
            if (correct.getUserData() instanceof Integer) {
                if (circleStart < 0)
                    circleStart = wrong.getBody().getWorldCenter().x;
                circles.removeValue(wrong.getBody(), true);
                toDestroy.add(wrong.getBody());
                return;
            }
            Coords c = (Coords) correct.getUserData();
            if (bricks.get(c.i)[c.j] < 0) {
                bricks.get(c.i)[c.j] = 0;
                toDestroy.add(correct.getBody());
                ballNum++;
                return;
            }
            bricks.get(c.i)[c.j]--;
            exp++;
            if (exp > highestExp) {
                highestExp = exp;
                highScore.putInteger("exp", exp);
                highScore.flush();
            }
            if (bricks.get(c.i)[c.j] == 0)
                toDestroy.add(correct.getBody());
        }

        public void endContact(Contact contact) {
        }

        public void preSolve(Contact contact, Manifold oldManifold) {
        }

        public void postSolve(Contact contact, ContactImpulse impulse) {
        }
    };
    ContactListener simulationContactListener = new ContactListener() {
        @Override
        public void beginContact(Contact contact) {
            Fixture correct = contact.getFixtureA(), wrong = contact.getFixtureB();
            if (correct.getUserData() == null) {
                wrong = correct;
                correct = contact.getFixtureB();
            }
            if (correct.getUserData() == null)
                return;
            if (correct.getUserData() instanceof Integer) {
                simulationHit = 0;
            }
        }

        @Override
        public void endContact(Contact contact) {

        }

        @Override
        public void preSolve(Contact contact, Manifold oldManifold) {

        }

        @Override
        public void postSolve(Contact contact, ContactImpulse impulse) {

        }
    };

    public BrickGrid(int width, int maxHeight) {
        this.width = width;
        this.maxHeight = maxHeight;
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("DroidSansMono.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = Gdx.graphics.getWidth() / 20;
        parameter.color = Color.WHITE;
        font = generator.generateFont(parameter);
        generator.dispose();
        world = new World(new Vector2(0, G), true);
        highestMax = highScore.getInteger("score");
        highestExp = highScore.getInteger("exp");
    }

    public void loadSave() {
        if (!current.getBoolean("saved"))
            push();
        else {
            max = current.getInteger("max");
            ballsLeft = ballNum = current.getInteger("ballNum");
            for (int i = 0; i < Math.min(max, maxHeight); i++) {
                bricks.add(new int[width]);
                for (int j = 0; j < width; j++) {
                    bricks.get(i)[j] = current.getInteger("b" + i + j);
                }
            }
            makeWorld();
        }
    }

    public void update() {
        if (isRunning) {
            for (int i = 0; i < (fastForward ? 2 : 1); i++) {
                if (ballsLeft > 0) {
                    launchBall();
                    ballsLeft--;
                }
                world.step(1 / 120f, 1, 1);
                for (Body b : toDestroy)
                    world.destroyBody(b);
                toDestroy.clear();
            }
            if (circles.size == 0) {
                isRunning = false;
                oldCircleStart = circleStart;
                push();
            }
        }
    }

    public void launchBall() {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(oldCircleStart, BALL_RADIUS * 1.1f);
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        Body body = world.createBody(bodyDef);
        circles.add(body);

        CircleShape circle = new CircleShape();
        circle.setRadius(BALL_RADIUS);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0;
        fixtureDef.restitution = 1;
        fixtureDef.friction = 0;
        fixtureDef.filter.categoryBits = C_FILTER;
        fixtureDef.filter.maskBits = B_FILTER | W_FILTER;
        body.createFixture(fixtureDef);
        body.applyLinearImpulse(swipeX, swipeY, body.getWorldCenter().x, body.getWorldCenter().y, true);
        circle.dispose();
    }

    public void swipe(float x, float y) {
        if (y < 40) return;
        isRunning = true;
        swipeX = x;
        swipeY = y;
        ballsLeft = ballNum;
        oldCircleStart = circleStart;
        Gdx.graphics.setContinuousRendering(true);
        circleStart = -1;
    }

    void addRect(World world, float x, float y, float w, float h, Object data) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(x + w / 2, y + h / 2);

        Body body = world.createBody(bodyDef);

        PolygonShape box = new PolygonShape();
        box.setAsBox(w / 2, h / 2);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = box;
        fixtureDef.filter.maskBits = C_FILTER;
        fixtureDef.filter.categoryBits = W_FILTER;
        body.createFixture(fixtureDef).setUserData(data);
        box.dispose();
    }

    private void shuffleArray(int[] array) {
        int index;
        for (int i = array.length - 1; i > 0; i--) {
            index = MathUtils.random(i);
            if (index != i) {
                int tmp = array[i];
                array[i] = array[index];
                array[index] = tmp;
            }
        }
    }

    public void makeWorld() {
        circles.clear();
        world.dispose();
        world = new World(new Vector2(0, G), true);
        world.setContactListener(defaultContactListener);
        addRect(world, -WORLD_WIDTH, -WORLD_HEIGHT, WORLD_WIDTH, 3 * WORLD_HEIGHT, null);
        addRect(world, WORLD_WIDTH, -WORLD_HEIGHT, WORLD_WIDTH, 3 * WORLD_HEIGHT, null);
        addRect(world, 0, -WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT, 999999);
        addRect(world, 0, WORLD_HEIGHT, WORLD_WIDTH, WORLD_HEIGHT, null);

        float blockWidth = WORLD_WIDTH / width, blockHeight = WORLD_HEIGHT / (maxHeight + 1);
        float currentHeight = blockHeight * (maxHeight - bricks.size);
        for (int i = 0; i < bricks.size; i++) {
            int[] line = bricks.get(i);
            for (int j = 0; j < width; j++) {
                if (line[j] > 0) {
                    BodyDef bodyDef = new BodyDef();
                    bodyDef.position.set(j * blockWidth + blockWidth / 2, currentHeight + blockHeight / 2);

                    Body body = world.createBody(bodyDef);

                    PolygonShape box = new PolygonShape();
                    box.setAsBox(blockWidth / 2, blockHeight / 2);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = box;
                    fixtureDef.filter.maskBits = C_FILTER;
                    fixtureDef.filter.categoryBits = B_FILTER;
                    body.createFixture(fixtureDef).setUserData(new Coords(i, j));
                    box.dispose();
                } else if (line[j] < 0) {
                    BodyDef bodyDef = new BodyDef();
                    bodyDef.position.set(j * blockWidth + blockWidth / 2, currentHeight + blockHeight / 2);

                    Body body = world.createBody(bodyDef);

                    CircleShape circleShape = new CircleShape();
                    circleShape.setRadius(BONUS_RADIUS);

                    FixtureDef fixtureDef = new FixtureDef();
                    fixtureDef.shape = circleShape;
                    fixtureDef.filter.maskBits = C_FILTER;
                    fixtureDef.filter.categoryBits = B_FILTER;
                    fixtureDef.isSensor = true;
                    body.createFixture(fixtureDef).setUserData(new Coords(i, j));
                    circleShape.dispose();
                }
            }
            currentHeight += blockHeight;
        }
    }

    public void push() {
        if (++max > highestMax) {
            highestMax = max;
            highScore.putInteger("score", highestMax);
            highScore.flush();
        }

        Main.swipeManager.unDrag();

        int[] row = new int[width];
        row[0] = -1;
        int n;
        if (max < 100) {
            if (MathUtils.random() > 0.8)
                n = MathUtils.random(width / 2 - 2) + 2;
            else n = MathUtils.random(width - 2) + 2;
        } else if (max > 300)
            n = MathUtils.random(width - 3) + 3;
        else {
            if (MathUtils.random() > 0.6)
                n = MathUtils.random(width - 3) + 3;
            else n = MathUtils.random(width - 2) + 2;
        }
        for (int i = 1; i < n; i++)
            row[i] = max;
        shuffleArray(row);
        bricks.add(row);
        if (bricks.size > maxHeight)
            for (int i : bricks.removeIndex(0))
                if (i < 0) ballNum++;

        ballsLeft = ballNum;
        current.putBoolean("saved", true);
        current.putInteger("max", max);
        current.putInteger("ballNum", ballNum);
        for (int i = 0; i < bricks.size; i++) {
            for (int j = 0; j < width; j++) {
                current.putInteger("b" + i + j, bricks.get(i)[j]);
            }
        }
        current.flush();
        makeWorld();
    }

    public void draw(ShapeRenderer sr, float minX, float maxX, float minY, float maxY) {
        sr.setColor(Color.BLACK);
        sr.line(minX, minY, maxX, minY);
        sr.line(minX, minY, minX, maxY);
        sr.line(minX, maxY, maxX, maxY);
        sr.line(maxX, minY, maxX, maxY);
        float blockWidth = (maxX - minX) / width, blockHeight = (maxY - minY) / (maxHeight + 1);
        float currentHeight = blockHeight * (maxHeight - bricks.size) + minY;
        for (int i = 0; i < bricks.size; i++) {
            int[] row = bricks.get(i);
            for (int j = 0; j < width; j++) {
                if (row[j] > 0) {
                    sr.setColor(Color.PINK.cpy().lerp(Color.PURPLE, ((float) row[j]) / max));
                    if (bricks.size == maxHeight && i <= 2) {
                        sr.setColor(Color.ORANGE.cpy().lerp(Color.SCARLET, ((float) row[j]) / max));
                    }
                    sr.rect(j * blockWidth + minX, currentHeight, blockWidth, blockHeight);
                } else if (row[j] < 0) {
                    sr.setColor(Color.LIME);
                    sr.circle(j * blockWidth + blockWidth / 2 + minX, currentHeight + blockHeight / 2,
                            BONUS_RADIUS / WORLD_HEIGHT * (maxY - minY));
                }
            }
            currentHeight += blockHeight;
        }
        sr.setColor(63f / 255, 0, 1, 1);
        if (ballsLeft > 0) {
            sr.circle(oldCircleStart / WORLD_WIDTH * (maxX - minX) + minX, BALL_RADIUS / WORLD_HEIGHT * (maxY - minY) + minY, BALL_RADIUS / WORLD_HEIGHT * (maxY - minY));
        }
        if (circleStart > 0) {
            sr.circle(circleStart / WORLD_WIDTH * (maxX - minX) + minX, BALL_RADIUS / WORLD_HEIGHT * (maxY - minY) + minY, BALL_RADIUS / WORLD_HEIGHT * (maxY - minY));
        }
        for (Body b : circles) {
            Vector2 pos = b.getPosition();
            sr.circle(pos.x / WORLD_WIDTH * (maxX - minX) + minX, pos.y / WORLD_HEIGHT * (maxY - minY) + minY, BALL_RADIUS / WORLD_HEIGHT * (maxY - minY));
        }
    }

    public void drawText(SpriteBatch sb, float minX, float maxX, float minY, float maxY) {
        float blockWidth = (maxX - minX) / width, blockHeight = (maxY - minY) / (maxHeight + 1);
        float currentHeight = blockHeight * (maxHeight - bricks.size) + minY;
        font.setColor(Color.WHITE);
        for (int[] row : bricks) {
            for (int i = 0; i < width; i++) {
                if (row[i] > 0) {
                    GlyphLayout layout = new GlyphLayout(font, Integer.toString(row[i]));
                    float x = i * blockWidth + minX + blockWidth / 2 - layout.width / 2;
                    float y = currentHeight + blockHeight / 2 + layout.height / 2;
                    font.draw(sb, layout, x, y);
                }
            }
            currentHeight += blockHeight;
        }
        font.setColor(Color.NAVY);
        if (ballsLeft > 0) {
            GlyphLayout layout = new GlyphLayout(font, "x" + ballsLeft);
            float x = oldCircleStart / WORLD_WIDTH * (maxX - minX) + minX - layout.width / 2;
            font.draw(sb, layout, x, minY - 10);
        }
        font.setColor(max == highestMax ? Color.LIME : Color.BLACK);
        GlyphLayout layout = new GlyphLayout(font, "Level: " + max + " (best: " + highestMax + ")");
        float x = (maxX + minX) / 2 + minX - layout.width / 2;
        font.draw(sb, layout, x, maxY + layout.height + 10);

        font.setColor(exp == highestExp ? Color.LIME : Color.BLACK);
        layout = new GlyphLayout(font, "Score: " + exp + " (best: " + highestExp + ")");
        x = (maxX + minX) / 2 + minX - layout.width / 2;
        font.draw(sb, layout, x, maxY + layout.height * 2 + 30);
    }

    public void drawSimulation(ShapeRenderer sr, float minX, float maxX, float minY, float maxY, float x, float y) {
        BodyDef bodyDef = new BodyDef();
        bodyDef.position.set(oldCircleStart, BALL_RADIUS * 1.1f);
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        Body body = world.createBody(bodyDef);

        CircleShape circle = new CircleShape();
        circle.setRadius(BALL_RADIUS);
        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.density = 0;
        fixtureDef.restitution = 1;
        fixtureDef.friction = 0;
        fixtureDef.filter.categoryBits = C_FILTER;
        fixtureDef.filter.maskBits = W_FILTER;
        body.createFixture(fixtureDef);
        body.applyLinearImpulse(x, y, body.getWorldCenter().x, body.getWorldCenter().y, true);
        circle.dispose();
        simulationHit = 2;
        world.setContactListener(simulationContactListener);
        Vector2 oldPos, newPos = body.getPosition();
        while (simulationHit > 0) {
            oldPos = newPos.cpy();
            world.step(1 / 120f, 1, 1);
            newPos = body.getPosition();
            sr.rectLine(oldPos.x / WORLD_WIDTH * (maxX - minX) + minX, oldPos.y / WORLD_HEIGHT * (maxY - minY) + minY,
                    newPos.x / WORLD_WIDTH * (maxX - minX) + minX, newPos.y / WORLD_HEIGHT * (maxY - minY) + minY, 4);
        }
        world.setContactListener(defaultContactListener);
        world.destroyBody(body);
    }

    class Coords {
        public int i, j;

        public Coords(int i, int j) {
            this.i = i;
            this.j = j;
        }
    }
}
