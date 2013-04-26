/*******************************************************************************
 * Copyright Cyril M. Hansen 2013
 * Creative Commons Attribution-NonCommercial-ShareAlike 3.0 Unported License
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.input.GestureDetector;
import com.badlogic.gdx.input.GestureDetector.GestureListener;
import com.badlogic.gdx.math.Vector2;

public class DIYGslSurface implements ApplicationListener,
		AndroidWallpaperListener, GestureListener {

	private String shaderProgram;

	private ShaderProgram shader;

	// Screen surface is drawn as a GDX mesh (mandatory)
	Mesh mesh;

	// View and projection matrix managed with GDX ortho camera
	OrthographicCamera cam;

	private float m_fboScaler = 8.0f;
	private boolean m_fboEnabled = true;
	private boolean timeDithering = false;
	private int timeDitheringFactor = 2;
	private int nbRender = 0;

	private boolean touchEnabled;

	private FrameBuffer m_fbo = null;
	private TextureRegion m_fboRegion = null;

	private SpriteBatch batch;
	private int renderSurfaceWidth;
	private int renderSurfaceHeight;

	private int effectiveSurfaceWidth;
	private int effectiveSurfaceHeight;

	private float mouseCursorX;
	private float mouseCursorY;

	private float time;

	private boolean doscreenShotRequest;
	private ScreenshotProcessor screenshotProc;

	private String errorMsg;

	// GL ES 2.0 is required
	public boolean needsGL20() {
		return true;
	}

	/** detailed constructor */
	public DIYGslSurface(String shaderGLSL, boolean reductionFactorEnabled,
			int reductionFactor, boolean touchEnabled, boolean timeDither,
			int timeDitherFactor) {
		this.shaderProgram = shaderGLSL;
		this.m_fboEnabled = reductionFactorEnabled;
		this.m_fboScaler = reductionFactor;

		this.timeDithering = timeDither;
		this.timeDitheringFactor = timeDitherFactor;

		this.touchEnabled = touchEnabled;

	}

	/**
	 * Constructor with default params, custom shader
	 * 
	 * @param forcedShaderProgram
	 */
	public DIYGslSurface(String forcedShaderProgram) {
		this.shaderProgram = forcedShaderProgram;

	}

	/**
	 * Constructor with default params and shader (demo mode)
	 */
	public DIYGslSurface() {
	}

	public void create() {

		// GL 20 Required
		if (!Gdx.graphics.isGL20Available()) {
			Gdx.app.log("DIYGslSurface", "isGL20Available returns false");
			Gdx.app.exit();
		}

		Gdx.input.setInputProcessor(new GestureDetector(this));

		setupShader();

		effectiveSurfaceWidth = Gdx.graphics.getWidth();
		effectiveSurfaceHeight = Gdx.graphics.getHeight();

		renderSurfaceWidth = effectiveSurfaceWidth;
		renderSurfaceHeight = effectiveSurfaceHeight;

		if (m_fboEnabled) {
			renderSurfaceWidth /= m_fboScaler;
			renderSurfaceHeight /= m_fboScaler;
		}

		cam = new OrthographicCamera(1f, 1f);
		mesh = genUnitRectangle();
		mesh.getVertexAttribute(Usage.Position).alias = "a_position";

		batch = new SpriteBatch();

		// set the cursor somewhere else than @ 0,0
		mouseCursorX = 0.5f;
		mouseCursorY = 0.5f;
	}

	private void setupShader() {
		// setup verbose shader compile error messages
		ShaderProgram.pedantic = false;

		errorMsg = null;
		if (shaderProgram != null) {
			try {
				shader = new CustomShader(shaderProgram);
			} catch (Exception ex) {
				// fall back to default shader
				// notify user
				errorMsg = ex.getMessage();
			}
		}

		if (shader == null) {
			// default shader, should have no problem
			try {
				shader = new HerokuSampleShader();
			} catch (Exception ex) {
				errorMsg = ex.getMessage();
			}
		}
	}

	public void render() {
		// render is not done for dummy frames in time dithering mode
		boolean doReallyRender = true;

		if (handleErrorAndSlowHardware()) {
			return;
		}

		time += Gdx.graphics.getRawDeltaTime();

		if (m_fboEnabled) // enable or disable the supersampling
		{
			initRenderFramebufferIfNeeded();

			if (timeDithering) {
				if (nbRender % timeDitheringFactor == 0) {
					m_fbo.begin();
				} else {
					doReallyRender = false;
				}
			}
		}

		if (doReallyRender) {
			// this is the main render function
			renderShaderonMesh();
		}

		if (m_fbo != null) {
			m_fbo.end();

			batch.begin();
			batch.draw(m_fboRegion, 0, 0, effectiveSurfaceWidth,
					effectiveSurfaceHeight);
			batch.end();
		}

		// Process snapshot requests if any
		// Proper gl context for this is only available in render() aka here
		if (doscreenShotRequest && screenshotProc != null) {
			screenshotProc.doProcessScreenShot();
			doscreenShotRequest = false;
		}

	}

	private boolean handleErrorAndSlowHardware() {
		// If rendering is too slow, display an error
		if (Gdx.graphics.getDeltaTime() > 5
				&& Gdx.graphics.getFramesPerSecond() < 2) {
			shader = null;
			errorMsg = "Hardware too slow for shader";
		}

		// If missing shader, display error
		if (shader == null) {
			if (errorMsg == null) {
				// FIXME I18N
				errorMsg = "Error loading shader";
			}

			// FIXME : Not tested ! May crash
			BitmapFont font = new BitmapFont();
			font.draw(batch, errorMsg, 50, 50);

			return true;
		}

		return false;
	}

	private void initRenderFramebufferIfNeeded() {
		if (m_fbo == null) {
			forceNewRenderBuffer();
		}
	}

	private void forceNewRenderBuffer() {
		// Use RGBA8888 to allow the use of a bitmap beneath and transparency
		// later
		// For now, waste of precious gpu ressources... ???
		m_fbo = new FrameBuffer(Format.RGBA8888,
				(int) (effectiveSurfaceWidth / m_fboScaler),
				(int) (effectiveSurfaceHeight / m_fboScaler), false);
		m_fboRegion = new TextureRegion(m_fbo.getColorBufferTexture());

		// don't ask question about this
		m_fboRegion.flip(false, true);
	}

	/**
	 * handler for GDX resize event
	 */
	public void resize(int width, int height) {
		this.effectiveSurfaceWidth = width;
		this.effectiveSurfaceHeight = height;

		if (m_fbo != null) {
			m_fbo.dispose();
			forceNewRenderBuffer();
		}
	}

	/**
	 * render shader on mesh, update shader vars for frame
	 */
	private void renderShaderonMesh() {

		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);
		shader.begin();

		// TODO set constants outside render ?
		shader.setUniformf("resolution", renderSurfaceWidth,
				renderSurfaceHeight);
		shader.setUniformMatrix("u_mvpMatrix", cam.combined);

		shader.setUniformf("time", time);

		// TODO : mouse cursor position should be virtualized
		shader.setUniformf("mouse", mouseCursorX, mouseCursorY);

		// real thing
		mesh.render(shader, GL20.GL_TRIANGLES);

		shader.end();
	}

	/**
	 * @param maxX
	 * @param maxY
	 * @return mesh setup for rectangular view
	 */
	public static Mesh genUnitRectangle() {

		float x1 = -0.5f;
		float x2 = 0.5f;
		float y1 = -0.5f;
		float y2 = 0.5f;

		Mesh mesh = new Mesh(true, 4, 6, new VertexAttribute(Usage.Position, 3,
				"attr_Position"));

		mesh.setVertices(new float[] { x1, y1, 0, x2, y1, -0, x2, y2, -0, x1,
				y2, 0 });
		mesh.setIndices(new short[] { 0, 1, 2, 2, 3, 0 });

		return mesh;
	}

	static class HerokuSampleShader extends CustomShader {
		public HerokuSampleShader() {
			super(Gdx.files.internal("data/shaders/herokuwiggle1.frag")
					.readString());
		}
	}

	private static class CustomShader extends ShaderProgram {
		public CustomShader(String customFragShader) {

			super(Gdx.files.internal("data/shaders/herokubase.vert")
					.readString(), customFragShader);

			if (!isCompiled()) {
				throw new RuntimeException("Shader compilation failed:\n"
						+ getLog());
			}
		}

	}

	/**
	 * handler for android dispose event
	 */
	public void dispose() {

	}

	/**
	 * handler for android pause event
	 */
	public void pause() {
		freeRessourcesIfAny();
	}

	private void freeRessourcesIfAny() {
		// Free ressources if any used
		if (m_fbo != null) {
			m_fbo.dispose();
			m_fbo = null;
		}

		m_fboRegion = null;

		batch.dispose();
	}

	/**
	 * handler for android resume event
	 */
	public void resume() {
		reserveRessources();
	}

	private void reserveRessources() {
		batch = new SpriteBatch();

		if (m_fboEnabled && m_fbo == null) {
			initRenderFramebufferIfNeeded();
		}
	}

	// handler for ???
	public void offsetChange(float xOffset, float yOffset, float xOffsetStep,
			float yOffsetStep, int xPixelOffset, int yPixelOffset) {
	}

	// handler for ???
	public void previewStateChange(boolean isPreview) {
	}

	/**
	 * handler for touchDown Update cursor position if useful
	 */
	public boolean touchDown(float x, float y, int pointer, int button) {
		if (touchEnabled) {
			// update mouse cursor pos
			mouseCursorX = x * 1.0f / renderSurfaceWidth;
			mouseCursorY = y * 1.0f / renderSurfaceHeight;
			return true;
		} else {
			// ignore event
			return false;
		}
	}

	/**
	 * tap event handler (ignored)
	 */
	public boolean tap(float x, float y, int count, int button) {
		// ignore event
		return false;
	}

	/**
	 * longPress event handler (ignored)
	 */
	public boolean longPress(float x, float y) {
		// ignore event
		return false;
	}

	/**
	 * fling event handler (ignored)
	 */
	public boolean fling(float velocityX, float velocityY, int button) {
		// ignore event
		return false;
	}

	/**
	 * pan event handler
	 */
	public boolean pan(float x, float y, float deltaX, float deltaY) {
		if (touchEnabled) {
			cam.translate(deltaX, deltaY);
			return true;
		} else {
			// ignore event
			return false;
		}
	}

	/**
	 * zoom event handler
	 */
	public boolean zoom(float initialDistance, float distance) {
		if (touchEnabled) {
			float scaleInv = initialDistance / distance;
			// projectionUser.scale(scale, scale, 1.0f);
			cam.zoom *= scaleInv;
			return true;
		} else {
			// ignore event
			return false;
		}
	}

	/**
	 * pinch event handler
	 */
	public boolean pinch(Vector2 initialPointer1, Vector2 initialPointer2,
			Vector2 pointer1, Vector2 pointer2) {
		// ignore event
		return false;
	}

	// Screen shot management
	// Screen shot processing is differed in render method due to gl details
	// data is then provided to screenshotProc callback

	public boolean isDoscreenShotRequest() {
		return doscreenShotRequest;
	}

	public void setDoscreenShotRequest(boolean doscreenShotRequest) {
		this.doscreenShotRequest = doscreenShotRequest;
	}

	public ScreenshotProcessor getScreenshotProc() {
		return screenshotProc;
	}

	public void setScreenshotProc(ScreenshotProcessor screenshotProc) {
		this.screenshotProc = screenshotProc;
	}

}
