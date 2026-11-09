package io.jbnu.test;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private SpriteBatch uiBatch;
    private BitmapFont font;
    private OrthographicCamera camera;
    private TiledMap map;
    private OrthogonalTiledMapRenderer mapRenderer;
    private TiledMapTileLayer terrainLayer;
    private TiledMapTileLayer hazardsLayer;
    private Texture stageBgTexture;
    private TextureRegion stage1Bg, stage2Bg, stage3Bg;

    private TextureRegion collectibleTileRegion;
    private TextureRegion goalTileRegion;
    private TextureRegion hazardTileRegion;
    private Music itemMusic;
    private Music goalMusic;
    private Music gameClearMusic;

    private boolean isStartScreen = true;
    private float startScreenTimer = 0f;

    private boolean isPaused = false;
    private boolean isGameCleared = false;

    private Texture idleSheet, runSheet, jumpSheet, climbSheet;
    private Animation<TextureRegion> idleAnimation, runAnimation, jumpAnimation, climbAnimation;
    private float stateTime = 0f;
    private float jumpStateTime = 0f;

    private float x = 40f;
    private float y = 112f;
    private float velocityY = 0f;
    private boolean isGrounded = false;
    private boolean facingLeft = false;
    public static class Collectible {
        float x, y, width, height;
        boolean collected = false;

        public Collectible(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
    }

    public static class Goal {
        float x, y, width, height;
        public Goal(float x, float y, float w, float h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
    }

    public static class FallingHazard {
        float x, y;
        boolean falling = false;
        float velocityY = 0f;

        public FallingHazard(float x, float y) {
            this.x = x; this.y = y;
        }
        public Rectangle getBounds() { return new Rectangle(x, y, 16f, 16f); }
    }

    private final Array<Collectible> collectibles = new Array<>();
    private final Array<Goal> goals = new Array<>();
    private final Array<FallingHazard> fallingHazards = new Array<>();
    private int totalCollectibles = 0;
    private int collectedCount = 0;

    private final Rectangle playerRect = new Rectangle();
    private final Rectangle tempRect = new Rectangle();
    private boolean isWallClinging = false;
    private boolean wallOnLeft = false;
    private float wallClingTimer = 0f;
    private static final float WALL_CLING_MAX_TIME = 1.0f;
    private boolean hasUsedWallCling = false;

    private static final float SPEED = 180f;
    private static final float JUMP_VELOCITY = 380f;
    private static final float GRAVITY = -9.8f * 100f;

    private float jumpBufferTimer = 0f;
    private static final float JUMP_BUFFER_TIME = 0.15f;

    private static final String[] STAGES = {"stage1.tmx", "stage2.tmx", "stage3.tmx"};
    private int currentStageIndex = 0;

    private float mapWidth = 1280f;
    private float mapHeight = 640f;

    private static final int FRAME_WIDTH = 128;
    private static final int FRAME_HEIGHT = 144;
    private static final int JUMP_FRAME_WIDTH = 224;
    private static final int JUMP_FRAME_HEIGHT = 256;
    private static final float RENDER_WIDTH = FRAME_WIDTH * 0.25f;
    private static final float RENDER_HEIGHT = FRAME_HEIGHT * 0.25f;

    @Override
    public void create() {
        batch = new SpriteBatch();
        uiBatch = new SpriteBatch();
        font = new BitmapFont();
        font.setColor(Color.YELLOW);
        font.getData().setScale(1.4f);

        camera = new OrthographicCamera();
        camera.setToOrtho(false, 640, 480);

        loadAssets();
    }

    private void loadAssets() {
        idleSheet = new Texture("player_idle.png");
        runSheet = new Texture("player_run.png");
        jumpSheet = new Texture("player_jump.png");
        climbSheet = new Texture("player_climb.png");

        TextureRegion[][] idleTmp = TextureRegion.split(idleSheet, FRAME_WIDTH, FRAME_HEIGHT);
        idleAnimation = new Animation<TextureRegion>(0.15f, idleTmp[0]);
        idleAnimation.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[][] runTmp = TextureRegion.split(runSheet, FRAME_WIDTH, FRAME_HEIGHT);
        runAnimation = new Animation<TextureRegion>(0.1f, runTmp[0]);
        runAnimation.setPlayMode(Animation.PlayMode.LOOP);

        TextureRegion[][] jumpTmp = TextureRegion.split(jumpSheet, JUMP_FRAME_WIDTH, JUMP_FRAME_HEIGHT);
        jumpAnimation = new Animation<TextureRegion>(0.11f, jumpTmp[0]);
        jumpAnimation.setPlayMode(Animation.PlayMode.NORMAL);

        TextureRegion[][] climbTmp = TextureRegion.split(climbSheet, 650, 718);
        climbAnimation = new Animation<TextureRegion>(0.15f, climbTmp[0]);
        climbAnimation.setPlayMode(Animation.PlayMode.LOOP);

        stageBgTexture = new Texture("stage.png");
        int bgWidth = stageBgTexture.getWidth() / 3;
        int bgHeight = stageBgTexture.getHeight() / 3;
        stage1Bg = new TextureRegion(stageBgTexture, bgWidth, 0, bgWidth, bgHeight);
        stage2Bg = new TextureRegion(stageBgTexture, bgWidth, bgHeight, bgWidth, bgHeight);
        stage3Bg = new TextureRegion(stageBgTexture, bgWidth * 2, bgHeight * 2, bgWidth, bgHeight);

        itemMusic = Gdx.audio.newMusic(Gdx.files.internal("itemsound.mp3"));
        goalMusic = Gdx.audio.newMusic(Gdx.files.internal("goalsound.mp3"));
        gameClearMusic = Gdx.audio.newMusic(Gdx.files.internal("gameclearsound.mp3"));
    }

    private void loadStage(int stageIndex) {
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();

        currentStageIndex = stageIndex;
        String stageFile = STAGES[currentStageIndex];

        map = new TmxMapLoader().load(stageFile);
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        collectibleTileRegion = null;
        goalTileRegion = null;
        hazardTileRegion = null;

        for (TiledMapTileSet tileset : map.getTileSets()) {
            TiledMapTile t10 = tileset.getTile(10 + 1);
            if (t10 != null && collectibleTileRegion == null) collectibleTileRegion = t10.getTextureRegion();

            TiledMapTile t13 = tileset.getTile(13 + 1);
            if (t13 != null && goalTileRegion == null) goalTileRegion = t13.getTextureRegion();

            TiledMapTile t14 = tileset.getTile(14 + 1);
            if (t14 != null && hazardTileRegion == null) hazardTileRegion = t14.getTextureRegion();
        }

        MapLayer layer1 = map.getLayers().get("Terrain");
        if (layer1 == null && map.getLayers().getCount() > 0) {
            layer1 = map.getLayers().get(0);
        }
        terrainLayer = (layer1 instanceof TiledMapTileLayer) ? (TiledMapTileLayer) layer1 : null;

        MapLayer layer2 = map.getLayers().get("Hazards");
        hazardsLayer = (layer2 instanceof TiledMapTileLayer) ? (TiledMapTileLayer) layer2 : null;

        if (terrainLayer != null) {
            mapWidth = terrainLayer.getWidth() * terrainLayer.getTileWidth();
            mapHeight = terrainLayer.getHeight() * terrainLayer.getTileHeight();
        }

        collectibles.clear();
        goals.clear();
        fallingHazards.clear();
        collectedCount = 0;

        // [요구사항 반영] Decorations 오브젝트 레이어만 단독으로 검사하여 수집품(10), 목적지(13), 낙하 함정(14) 배치
        MapLayer decorLayer = map.getLayers().get("Decorations");
        if (decorLayer == null) {
            decorLayer = map.getLayers().get("decorations");
        }

        if (decorLayer != null && decorLayer.getObjects() != null) {
            for (MapObject obj : decorLayer.getObjects()) {
                Integer gid = obj.getProperties().get("gid", Integer.class);
                if (gid != null) {
                    int tileId = gid - 1; // 0-based tile id
                    float objX = 0f, objY = 0f, w = 16f, h = 16f;
                    if (obj instanceof RectangleMapObject) {
                        Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                        objX = rect.x;
                        objY = rect.y;
                        w = rect.width;
                        h = rect.height;
                    } else {
                        Float px = obj.getProperties().get("x", Float.class);
                        Float py = obj.getProperties().get("y", Float.class);
                        Float pw = obj.getProperties().get("width", Float.class);
                        Float ph = obj.getProperties().get("height", Float.class);
                        if (px != null) objX = px;
                        if (py != null) objY = py;
                        if (pw != null && pw > 0) w = pw;
                        if (ph != null && ph > 0) h = ph;
                    }

                    if (tileId == 10) {
                        collectibles.add(new Collectible(objX, objY, w, h));
                    } else if (tileId == 13) {
                        goals.add(new Goal(objX, objY, w, h));
                    } else if (tileId == 14) {
                        fallingHazards.add(new FallingHazard(objX, objY));
                    }
                }
            }
        }
        totalCollectibles = collectibles.size;

        MapLayer spawnLayer = map.getLayers().get("PlayerSpawn");
        if (spawnLayer == null) {
            spawnLayer = map.getLayers().get("Entities");
        }

        if (spawnLayer != null && spawnLayer.getObjects().getCount() > 0) {
            MapObject mapObj = spawnLayer.getObjects().get(0);
            Float objX = mapObj.getProperties().get("x", Float.class);
            Float objY = mapObj.getProperties().get("y", Float.class);
            if (objX != null && objY != null) {
                x = objX;
                y = objY;
            }
        }

        velocityY = 0f;
        isGrounded = false;
        isWallClinging = false;
        hasUsedWallCling = false;
        isPaused = false;
        isGameCleared = false;
        jumpStateTime = 0f;
        jumpBufferTimer = 0f;

        camera.setToOrtho(false, 640, 480);
        camera.position.set(x + RENDER_WIDTH / 2f, y + RENDER_HEIGHT / 2f, 0);
        camera.update();
    }

    private int checkTile(float px, float py) {
        if (terrainLayer == null) return 0;
        int tileX = (int) (px / terrainLayer.getTileWidth());
        int tileY = (int) (py / terrainLayer.getTileHeight());

        if (tileX >= 0 && tileX < terrainLayer.getWidth() && tileY >= 0 && tileY < terrainLayer.getHeight()) {
            TiledMapTileLayer.Cell cell = terrainLayer.getCell(tileX, tileY);
            if (cell != null && cell.getTile() != null) {
                int tileId = cell.getTile().getId()-1;
                if (tileId == 17) {
                    return 2;
                }
                return 1;
            }
        }
        return 0;
    }

    private boolean checkHazards() {
        if (hazardsLayer != null) {
            float margin = 4f;
            int startX = (int) ((x + margin) / hazardsLayer.getTileWidth());
            int endX = (int) ((x + RENDER_WIDTH - margin) / hazardsLayer.getTileWidth());
            int startY = (int) (y / hazardsLayer.getTileHeight());
            int endY = (int) ((y + RENDER_HEIGHT) / hazardsLayer.getTileHeight());

            for (int tx = startX; tx <= endX; tx++) {
                for (int ty = startY; ty <= endY; ty++) {
                    if (tx >= 0 && tx < hazardsLayer.getWidth() && ty >= 0 && ty < hazardsLayer.getHeight()) {
                        TiledMapTileLayer.Cell cell = hazardsLayer.getCell(tx, ty);
                        if (cell != null && cell.getTile() != null) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private void checkObjectCollisions(float delta) {
        playerRect.set(x, y, RENDER_WIDTH + 2f, RENDER_HEIGHT + 2f);

        for (Collectible item : collectibles) {
            if (!item.collected) {
                tempRect.set(item.x, item.y, item.width, item.height);
                if (tempRect.overlaps(playerRect)) {
                    item.collected = true;
                    collectedCount++;
                    if (itemMusic != null) {
                        itemMusic.stop();
                        itemMusic.setPosition(0f);
                        itemMusic.play();
                    }
                }
            }
        }

        for (Goal goal : goals) {
            tempRect.set(goal.x, goal.y, goal.width, goal.height);
            if (tempRect.overlaps(playerRect)) {
                if (collectedCount >= totalCollectibles) {
                    if (currentStageIndex < STAGES.length - 1) {
                        if (goalMusic != null) {
                            goalMusic.stop();
                            goalMusic.setPosition(0f);
                            goalMusic.play();
                        }
                        loadStage(currentStageIndex + 1);
                        return;
                    } else {
                        isGameCleared = true;
                        if (gameClearMusic != null) {
                            gameClearMusic.stop();
                            gameClearMusic.setPosition(0f);
                            gameClearMusic.play();
                        }
                    }
                }
            }
        }

        for (FallingHazard fh : fallingHazards) {
            if (!fh.falling) {
                float playerCenterX = x + RENDER_WIDTH / 2f;
                float playerCenterY = y + RENDER_HEIGHT / 2f;
                if (Math.abs(playerCenterX - fh.x - 8f) <= 48f && Math.abs(playerCenterY - fh.y) <= 48f) {
                    fh.falling = true;
                }
            } else {
                fh.velocityY += GRAVITY * delta;
                fh.y += fh.velocityY * delta;
                if (fh.getBounds().overlaps(playerRect)) {
                    loadStage(0);
                    return;
                }
            }
        }
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();

        if (isStartScreen) {
            startScreenTimer += delta * 70f;
            float bgW = stageBgTexture.getWidth() / 3f;
            float bgH = stageBgTexture.getHeight() / 3f;
            float texW = stageBgTexture.getWidth();

            camera.viewportWidth = bgW;
            camera.viewportHeight = bgH;
            camera.position.set(bgW / 2f + (startScreenTimer % texW), bgH / 2f, 0);
            camera.update();

            ScreenUtils.clear(0.08f, 0.12f, 0.18f, 1f);

            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            batch.draw(stageBgTexture, 0, 0);
            batch.draw(stageBgTexture, texW, 0);
            batch.end();

            uiBatch.begin();
            font.setColor(Color.WHITE);
            font.getData().setScale(1.3f);
            font.draw(uiBatch, "Press [ENTER] to Start Game", Gdx.graphics.getWidth() / 2f - 140f, Gdx.graphics.getHeight() / 2f - 20f);
            uiBatch.end();

            if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                isStartScreen = false;
                font.getData().setScale(1.4f);
                loadStage(0);
            }
            return;
        }

        if (isGameCleared && Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            isGameCleared = false;
            isStartScreen = true;
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            isPaused = !isPaused;
        }

        boolean isMoving = false;
        if (!isPaused && !isGameCleared) {
            stateTime += delta;

            if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
                loadStage(0);
                return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) {
                loadStage(1);
                return;
            } else if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) {
                loadStage(2);
                return;
            }

            float moveX = 0f;
            if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
                moveX -= SPEED * delta;
                facingLeft = true;
                isMoving = true;
            }
            if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
                moveX += SPEED * delta;
                facingLeft = false;
                isMoving = true;
            }

            if (moveX != 0) {
                float newX = x + moveX;
                float margin = 4f;

                boolean collision = false;
                if (moveX < 0) {
                    if (checkTile(newX + margin, y + 2f) > 0 ||
                        checkTile(newX + margin, y + RENDER_HEIGHT / 2f) > 0 ||
                        checkTile(newX + margin, y + RENDER_HEIGHT - 4f) > 0) {
                        collision = true;
                    }
                } else {
                    if (checkTile(newX + RENDER_WIDTH - margin, y + 2f) > 0 ||
                        checkTile(newX + RENDER_WIDTH - margin, y + RENDER_HEIGHT / 2f) > 0 ||
                        checkTile(newX + RENDER_WIDTH - margin, y + RENDER_HEIGHT - 4f) > 0) {
                        collision = true;
                    }
                }

                if (!collision) {
                    x = newX;
                }
            }

            boolean touchCaveWallLeft = (checkTile(x - 2f, y + 4f) == 2) || (checkTile(x - 2f, y + RENDER_HEIGHT / 2f) == 2) || (checkTile(x - 2f, y + RENDER_HEIGHT - 4f) == 2);
            boolean touchCaveWallRight = (checkTile(x + RENDER_WIDTH + 2f, y + 4f) == 2) || (checkTile(x + RENDER_WIDTH + 2f, y + RENDER_HEIGHT / 2f) == 2) || (checkTile(x + RENDER_WIDTH + 2f, y + RENDER_HEIGHT - 4f) == 2);

            if (touchCaveWallLeft || touchCaveWallRight) {
                if (!isWallClinging && !hasUsedWallCling) {
                    isWallClinging = true;
                    hasUsedWallCling = true;
                    wallClingTimer = WALL_CLING_MAX_TIME;
                    wallOnLeft = touchCaveWallLeft;
                    facingLeft = wallOnLeft;
                }
            } else {
                isWallClinging = false;
            }

            if (isWallClinging) {
                wallClingTimer -= delta;
                if (wallClingTimer > 0f) {
                    velocityY = 0f;
                } else {
                    isWallClinging = false;
                    jumpStateTime += delta;
                    velocityY += GRAVITY * delta;
                }
            } else if (!isGrounded) {
                jumpStateTime += delta;
                velocityY += GRAVITY * delta;
            }

            float moveY = velocityY * delta;
            float newY = y + moveY;
            float marginX = 4f;

            if (moveY <= 0) {
                if (checkTile(x + marginX, newY) > 0 ||
                    checkTile(x + RENDER_WIDTH - marginX, newY) > 0 ||
                    checkTile(x + RENDER_WIDTH / 2f, newY) > 0) {
                    int tileY = (int) (newY / terrainLayer.getTileHeight());
                    y = (tileY + 1) * terrainLayer.getTileHeight();
                    velocityY = 0f;
                    isGrounded = true;
                    isWallClinging = false;
                    hasUsedWallCling = false;
                } else {
                    y = newY;
                    isGrounded = false;
                }
            } else {
                if (checkTile(x + marginX, newY + RENDER_HEIGHT) > 0 ||
                    checkTile(x + RENDER_WIDTH - marginX, newY + RENDER_HEIGHT) > 0) {
                    int tileY = (int) ((newY + RENDER_HEIGHT) / terrainLayer.getTileHeight());
                    y = tileY * terrainLayer.getTileHeight() - RENDER_HEIGHT;
                    velocityY = 0f;
                } else {
                    y = newY;
                }
            }

            if (isGrounded) {
                if (checkTile(x + marginX, y - 1f) == 0 &&
                    checkTile(x + RENDER_WIDTH - marginX, y - 1f) == 0 &&
                    checkTile(x + RENDER_WIDTH / 2f, y - 1f) == 0) {
                    isGrounded = false;
                }
            }

            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                jumpBufferTimer = JUMP_BUFFER_TIME;
            } else {
                jumpBufferTimer -= delta;
            }

            boolean canJump = isGrounded || isWallClinging;
            if (canJump && jumpBufferTimer > 0f) {
                velocityY = JUMP_VELOCITY;
                if (isWallClinging) {
                    x += wallOnLeft ? 6f : -6f;
                    facingLeft = !wallOnLeft;
                }
                isGrounded = false;
                isWallClinging = false;
                hasUsedWallCling = false;
                jumpStateTime = 0f;
                jumpBufferTimer = 0f;
            }

            if (checkHazards()) {
                loadStage(0);
                return;
            }

            checkObjectCollisions(delta);
            if (isGameCleared) return;

            x = Math.max(0, Math.min(x, mapWidth - RENDER_WIDTH));
            if (y < -100f) {
                loadStage(currentStageIndex);
                return;
            }

            camera.position.x = Math.max(camera.viewportWidth / 2f, Math.min(x + RENDER_WIDTH / 2f, mapWidth - camera.viewportWidth / 2f));
            camera.position.y = Math.max(camera.viewportHeight / 2f, Math.min(y + RENDER_HEIGHT / 2f, mapHeight - camera.viewportHeight / 2f));
            camera.update();
        }

        ScreenUtils.clear(0.08f, 0.12f, 0.18f, 1f);

        // stage.png 배경 이미지 가로 3등분 분할하여 스테이지별 백그라운드 렌더링
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        TextureRegion currentBg = (currentStageIndex == 0) ? stage1Bg : (currentStageIndex == 1) ? stage2Bg : stage3Bg;
        batch.draw(currentBg, camera.position.x - camera.viewportWidth / 2f, camera.position.y - camera.viewportHeight / 2f, camera.viewportWidth, camera.viewportHeight);
        batch.end();

        mapRenderer.setView(camera);
        mapRenderer.render();

        // Decorations 오브젝트 레이어에 배치된 오브젝트(10: 수집품, 13: 목적지, 14: 함정) 렌더링
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        if (collectibleTileRegion != null) {
            for (Collectible item : collectibles) {
                if (!item.collected) {
                    batch.draw(collectibleTileRegion, item.x, item.y, item.width, item.height);
                }
            }
        }

        if (goalTileRegion != null) {
            for (Goal goal : goals) {
                batch.draw(goalTileRegion, goal.x, goal.y, goal.width, goal.height);
            }
        }

        if (hazardTileRegion != null) {
            for (FallingHazard fh : fallingHazards) {
                batch.draw(hazardTileRegion, fh.x, fh.y, 16f, 16f);
            }
        }

        Animation<TextureRegion> currentAnimation;
        if (!isMoving && !isWallClinging) {
            currentAnimation = idleAnimation;
        } else if (isWallClinging) {
            currentAnimation = climbAnimation;
        } else if (!isGrounded) {
            currentAnimation = jumpAnimation;
        } else {
            currentAnimation = runAnimation;
        }

        TextureRegion currentFrame = currentAnimation.getKeyFrame(isWallClinging ? wallClingTimer : (!isGrounded ? jumpStateTime : stateTime), true);
        if (facingLeft && !currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        } else if (!facingLeft && currentFrame.isFlipX()) {
            currentFrame.flip(true, false);
        }

        batch.draw(currentFrame, x, y, RENDER_WIDTH, RENDER_HEIGHT);
        batch.end();

        uiBatch.begin();
        String stageStr = "STAGE " + (currentStageIndex + 1);
        String crystalStr = "Crystals : " + collectedCount + " / " + totalCollectibles;

        font.draw(uiBatch, stageStr, 20, Gdx.graphics.getHeight() - 20);
        font.draw(uiBatch, crystalStr, 20, Gdx.graphics.getHeight() - 45);

        if (isPaused) {
            font.setColor(Color.ORANGE);
            font.draw(uiBatch, "=== PAUSED ===", Gdx.graphics.getWidth() / 2f - 75f, Gdx.graphics.getHeight() / 2f + 20f);
            font.draw(uiBatch, "Press [ESC] to Resume", Gdx.graphics.getWidth() / 2f - 165f, Gdx.graphics.getHeight() / 2f - 10f);
            font.setColor(Color.YELLOW);
        }

        if (isGameCleared) {
            font.setColor(Color.GREEN);
            font.draw(uiBatch, "=== CONGRATULATIONS! GAME CLEAR! ===", Gdx.graphics.getWidth() / 2f - 160f, Gdx.graphics.getHeight() / 2f + 30f);
            font.draw(uiBatch, "Press [ENTER] for Restart", Gdx.graphics.getWidth() / 2f - 145f, Gdx.graphics.getHeight() / 2f - 10f);
            font.setColor(Color.YELLOW);
        }

        uiBatch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        uiBatch.dispose();
        font.dispose();
        if (idleSheet != null) idleSheet.dispose();
        if (runSheet != null) runSheet.dispose();
        if (jumpSheet != null) jumpSheet.dispose();
        if (climbSheet != null) climbSheet.dispose();
        if (stageBgTexture != null) stageBgTexture.dispose();
        if (itemMusic != null) itemMusic.dispose();
        if (goalMusic != null) goalMusic.dispose();
        if (gameClearMusic != null) gameClearMusic.dispose();
        if (map != null) map.dispose();
        if (mapRenderer != null) mapRenderer.dispose();
    }
}
