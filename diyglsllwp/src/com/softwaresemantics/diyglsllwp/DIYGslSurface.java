/*******************************************************************************
 * Cyril M. Hansen 2013
 * 
 * Licences :
 * Creative Commons Attribution-ShareAlike 3.0
 * Creative Commons Attribution - Partage dans les Mêmes Conditions 3.0 France
 * 
 * http://creativecommons.org/licenses/by-sa/3.0
 * http://creativecommons.org/licenses/by-sa/3.0/fr/
 * 
 * Sources :
 * https://github.com/cyrilmhansen/DIYGlslLwp
 ******************************************************************************/
package com.softwaresemantics.diyglsllwp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.android.AndroidWallpaperListener;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
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

	private static final int BYTES_PER_FLOAT = 4;

	private static final String OPEN_GL_ES_2_0_REQUIRED = "OpenGL ES 2.0 required";

	private static final String ATTR_POSITION = "attr_Position";

	private static final String ERROR_LOADING_SHADER = "Error loading shader";

	private static final String HARDWARE_TOO_SLOW_FOR_SHADER = "Hardware too slow for shader";

	private static final String DO_PROCESS_SCREEN_SHOT = "doProcessScreenShot";

	private static final String NO_EXCEPTION_MSG = "no exception msg";

	private String shaderProgram;

	private ShaderProgram shader;

	// Screen surface is drawn as a GDX mesh
	Mesh mesh;

	// View and projection matrix managed with GDX ortho camera
	OrthographicCamera cam;

	private float m_fboScaler = 1.0f;
	// private boolean m_fboEnabled = true;
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

	private boolean showFPS = true;

	private BitmapFont font;

	private ClickHandler listener;

	private ReqFailCallback reqFailCallback;

	// private FrameBuffer frontBuf, backBuf;
	//
	private int surfacePosAttrIdx;

	private int positionIdx;
	//
	// private int buffers[];

	private FloatBuffer renderVerticesBuffer;

	private FloatBuffer verticesBuffer;

	private FloatBuffer unitSurfaceBuffer;

	private boolean lwpIsRunning;

	private float[] vertices;

	private IntBuffer handles;

	private float[] unitSurfaces;

	// private VertexBufferObject screenDimVBO;

	// GL ES 2.0 is required
	public boolean needsGL20() {
		return true;
	}

	/** detailed constructor */
	public DIYGslSurface(String shaderGLSL, boolean reductionFactorEnabled,
			int reductionFactor, boolean touchEnabled, boolean displayFPSLWP,
			boolean timeDither, int timeDitherFactor) {
		this.shaderProgram = shaderGLSL;
		// this.m_fboEnabled = reductionFactorEnabled;
		this.m_fboScaler = reductionFactor;

		this.timeDithering = timeDither;
		this.timeDitheringFactor = timeDitherFactor;

		this.touchEnabled = touchEnabled;
		this.showFPS = displayFPSLWP;

	}

	public void updatePrefs(boolean reductionFactorEnabled,
			int reductionFactor, boolean touchEnabled, boolean displayFPSLWP,
			boolean timeDither, int timeDitherFactor) {
		// this.m_fboEnabled = reductionFactorEnabled;
		this.m_fboScaler = reductionFactor;

		this.timeDithering = timeDither;
		this.timeDitheringFactor = timeDitherFactor;

		this.touchEnabled = touchEnabled;
		this.showFPS = displayFPSLWP;

		// Force re creation of framebuffer
		this.m_fbo = null;
	}

	public void updateShader(String shaderGLSL) {
		this.shaderProgram = shaderGLSL;

		setupShader();
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

		// GL 20 (GL2ES) Required
		if (!Gdx.graphics.isGL20Available()) {
			Gdx.app.log("DIYGslSurface", "isGL20Available returns false");
			if (reqFailCallback != null) {
				reqFailCallback.onRequirementFailure(null);
			}
			// Gdx.app.exit();
		}

		// If the Lwp is still running
		// we must manage manually the change of graphics of the GDX app

		// if (lwpIsRunning) {
		// Gdx.app.s
		// }

		Gdx.input.setInputProcessor(new GestureDetector(this));

		setupShader();

		effectiveSurfaceWidth = Gdx.graphics.getWidth();
		effectiveSurfaceHeight = Gdx.graphics.getHeight();

		renderSurfaceWidth = effectiveSurfaceWidth;
		renderSurfaceHeight = effectiveSurfaceHeight;

		// if (m_fboEnabled) {
		renderSurfaceWidth /= m_fboScaler;
		renderSurfaceHeight /= m_fboScaler;
		// }

		computeSurfaceCorners();

		// frontBuf = createTarget(renderSurfaceWidth, renderSurfaceHeight);
		// backBuf = createTarget(renderSurfaceWidth, renderSurfaceHeight);

		// Set up buffers

		cam = new OrthographicCamera(1f, 1f);

		mesh = genUnitRectangle();
		mesh.getVertexAttribute(Usage.Position).alias = "a_position";

		reserveRessources();

		// set the cursor somewhere else than @ 0,0
		mouseCursorX = 0.2f;
		mouseCursorY = 0.3f;
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

		if (shader != null) {
			surfacePosAttrIdx = shader.getAttributeLocation("surfacePosAttrib");
			// ?? shader.enableVertexAttribute(positionAttributeIdx);
			Gdx.gl20.glEnableVertexAttribArray(surfacePosAttrIdx);

			positionIdx = shader.getAttributeLocation("position");
			Gdx.gl20.glEnableVertexAttribArray(positionIdx);

			// Create vertex buffer (2 triangles)

			// http://www.learnopengles.com/android-lesson-seven-an-introduction-to-vertex-buffer-objects-vbos/

			vertices = new float[] { -1.0f, -1.0f, 1f, -1.0f, -1.0f, 1f, 1.0f,
					-1.0f, 1f, 1.0f, -1.0f, 1f };
			// vertices = new float[] { -10.0f, -10.0f, 10.0f, -10.0f, -10.0f,
			// 10.0f, 10.0f,
			// -10.0f, 10.0f, 10.0f, -10.0f, 10.0f };
			verticesBuffer = ByteBuffer
					.allocateDirect(vertices.length * BYTES_PER_FLOAT)

					// Floats can be in big-endian or little-endian order.
					// We want the same as the native platform.
					.order(ByteOrder.nativeOrder())

					// Give us a floating-point view on this byte buffer.
					.asFloatBuffer();

			// Copy data from the Java heap to the native heap.
			verticesBuffer.put(vertices)

			// Reset the buffer position to the beginning of the buffer.
					.position(0);

			unitSurfaces = new float[] { -10.0f, -10.0f, 0.5f, 10.0f, -10.0f,
					0.5f, -10.0f, 10.0f, 0.5f, 10.0f, -10.0f, 0.5f, 10.0f, 10.0f,
					0.5f, -10.0f, 10.0f, 0.5f };
			unitSurfaceBuffer = ByteBuffer
					.allocateDirect(unitSurfaces.length * BYTES_PER_FLOAT)
					.order(ByteOrder.nativeOrder()).asFloatBuffer();
			unitSurfaceBuffer.put(unitSurfaces).position(0);

			handles = ByteBuffer.allocateDirect(1 * 4)
					.order(ByteOrder.nativeOrder()).asIntBuffer();
			// boolean d = handles.isDirect();
			Gdx.gl20.glGenBuffers(1, handles);
			// final int buffers[] = new int[3];

			//
			// // Bind to the buffer. Future commands will affect this buffer
			// specifically.
			Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, handles.get(0));
			//
			// // Transfer data from client memory to the buffer.
			// // We can release the client memory after this call.
			Gdx.gl20.glBufferData(GL20.GL_ARRAY_BUFFER, unitSurfaces.length
					* BYTES_PER_FLOAT, unitSurfaceBuffer, GL20.GL_STATIC_DRAW);

			// TO BE RELOADED AFTER PAUSE

			// Pass in the position information
			// Gdx.gl20.glEnableVertexAttribArray(surfacePosAttrIdx);
			// Gdx.gl20.glVertexAttribPointer(surfacePosAttrIdx, 4,
			// GL20.GL_FLOAT, false, 0, verticesBuffer);

			// unitVBO.setVertices(vertices, 0, vertices.length);
			//
			// screenDimVBO = new VertexBufferObject(false, 6);

			// surface.positionAttribute = gl.getAttribLocation(currentProgram,
			// "surfacePosAttrib");
			// gl.enableVertexAttribArray(surface.positionAttribute);
			//
			// vertexPosition = gl.getAttribLocation(currentProgram,
			// "position");
			// gl.enableVertexAttribArray( vertexPosition );
		}
	}

	public void computeSurfaceCorners() {

		float surface_width = renderSurfaceHeight
				* (float) effectiveSurfaceWidth / effectiveSurfaceHeight;
		float halfWidth = surface_width * 0.5f, halfHeight = renderSurfaceHeight * 0.5f;

		float centerX = -surface_width / 2.0f;
		float centerY = renderSurfaceHeight / 2.0f;

		float[] renderVert = { centerX - halfWidth, centerY - halfHeight,
				centerX + halfWidth, centerY - halfHeight, centerX - halfWidth,
				centerY + halfHeight, centerX + halfWidth,
				centerY - halfHeight, centerX + halfWidth,
				centerY + halfHeight, centerX - halfWidth, centerY + halfHeight };

		if (renderVerticesBuffer == null) {
			renderVerticesBuffer = ByteBuffer
					.allocateDirect(renderVert.length * 4)

					// Floats can be in big-endian or little-endian order.
					// We want the same as the native platform.
					.order(ByteOrder.nativeOrder())

					// Give us a floating-point view on this byte buffer.
					.asFloatBuffer();
		}

		// Copy data from the Java heap to the native heap.
		renderVerticesBuffer.put(renderVert)

		// Reset the buffer position to the beginning of the buffer.
				.position(0);
	}

	public void render() {

		// GL 20 Required
		if (!Gdx.graphics.isGL20Available()) {
			Gdx.app.log("DIYGslSurface", "isGL20Available returns false");

			// Gdx.app.exit()
			displayErrorMsg();
			return;
		}

		if (handleErrorAndSlowHardware()) {
			return;
		}

		time += Gdx.graphics.getRawDeltaTime();

		initRenderFramebufferIfNeeded();

		// render is not done for dummy frames in time dithering mode
		// boolean doReallyRender = true;
		if (timeDithering) {
			if (nbRender % timeDitheringFactor != 0) {
				// doReallyRender = false;
				// don't clear view, don't render, don't process screenshots
				return;
			}
		}

		// if (doReallyRender) {
		// main REAL render function
		// if (m_fboEnabled) // enable or disable the supersampling
		// {

		try {
			// Clear surface / Black background
			// TODO Pref background color
			// necessaire ??

			// backBuf.begin();
			// batch.begin();
			m_fbo.begin();

			Gdx.gl20.glClearColor(0, 0, 0, 1);
			Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT);

			renderShaderonMesh();

			m_fbo.end();
			// batch.end();

			// backBuf.end();

		} catch (Exception ex) {
			String msg = ex.getMessage() != null ? ex.getMessage()
					: NO_EXCEPTION_MSG;
			Gdx.app.log("GDX render", msg, ex);
		}

		// Mise à l'échelle
		batch.begin();

		try {
			batch.disableBlending();
			batch.draw(m_fbo.getColorBufferTexture(), 0, 0,
					effectiveSurfaceWidth, effectiveSurfaceHeight);
			batch.enableBlending();

		} catch (Exception ex) {
			String msg = ex.getMessage() != null ? ex.getMessage()
					: NO_EXCEPTION_MSG;
			Gdx.app.log("m_fbo scale", msg);
		} finally {
			batch.end();
		}

		// FPS is drawn on real screen
		if (showFPS) {
			batch.begin();
			font.draw(batch, "FPS:" + Gdx.graphics.getFramesPerSecond(),
					Gdx.graphics.getWidth() - 60, 15);
			batch.end();
		}

		// Process snapshot requests if any
		// Proper gl context for this is only available in render() aka here

		if (doscreenShotRequest && screenshotProc != null) {
			try {
				screenshotProc.doProcessScreenShot();
			} catch (Exception ex) {
				Gdx.app.log(DO_PROCESS_SCREEN_SHOT, ex.getLocalizedMessage());
			}
			doscreenShotRequest = false;
		}

	}

	private void displayErrorMsg() {
		batch.begin();
		font.draw(batch, OPEN_GL_ES_2_0_REQUIRED, 50, 50);
		batch.end();
	}

	private boolean handleErrorAndSlowHardware() {
		// If rendering is too slow, display an error
		if (Gdx.graphics.getDeltaTime() > 5
				&& Gdx.graphics.getFramesPerSecond() < 2) {
			shader = null;
			errorMsg = HARDWARE_TOO_SLOW_FOR_SHADER;
		}

		// If missing shader, display error
		if (shader == null) {
			if (errorMsg == null) {
				// TODO I18N
				errorMsg = ERROR_LOADING_SHADER;
			}
			// TODO test again
			batch.begin();
			font.draw(batch, errorMsg, 50, 50);
			batch.end();

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
		// No alpha channel needed
		m_fbo = new FrameBuffer(Format.RGB888,
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

		computeSurfaceCorners();

		if (m_fbo != null) {
			m_fbo.dispose();
			forceNewRenderBuffer();
		}
	}

	// adapted from glsl.heroku.com
	private FrameBuffer createTarget(int width, int height) {

		FrameBuffer fbuf = new FrameBuffer(Pixmap.Format.RGB888, width, height,
				true);

		return fbuf;

		// int textureIdx = 0;
		// int renderBufIdx = 0;
		//
		// // set up framebuffer
		// Gdx.gl20.glBindTexture(GL20.GL_TEXTURE_2D, textureIdx);
		// Gdx.gl20.glTexImage2D( GL20.GL_TEXTURE_2D, 0, GL20.GL_RGBA, width,
		// height, 0, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, null );
		//
		// Gdx.gl20.glTexParameteri( GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S,
		// GL20.GL_CLAMP_TO_EDGE );
		// Gdx.gl20.glTexParameteri( GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T,
		// GL20.GL_CLAMP_TO_EDGE );
		//
		// Gdx.gl20.glTexParameteri( GL20.GL_TEXTURE_2D,
		// GL20.GL_TEXTURE_MAG_FILTER, GL20.GL_NEAREST );
		// Gdx.gl20.glTexParameteri( GL20.GL_TEXTURE_2D,
		// GL20.GL_TEXTURE_MIN_FILTER, GL20.GL_NEAREST );
		//
		// Gdx.gl20.glBindFramebuffer( GL20.GL_FRAMEBUFFER, 0 );
		// Gdx.gl20.glFramebufferTexture2D( GL20.GL_FRAMEBUFFER,
		// GL20.GL_COLOR_ATTACHMENT0, GL20.GL_TEXTURE_2D, textureIdx, 0 );
		//
		// // set up renderbuffer
		// Gdx.gl20.glBindRenderbuffer( GL20.GL_RENDERBUFFER, renderBufIdx );
		//
		// Gdx.gl20.glRenderbufferStorage( GL20.GL_RENDERBUFFER,
		// GL20.GL_DEPTH_COMPONENT16, width, height );
		// Gdx.gl20.glFramebufferRenderbuffer( GL20.GL_FRAMEBUFFER,
		// GL20.GL_DEPTH_ATTACHMENT, GL20.GL_RENDERBUFFER, renderBufIdx );
		//
		// // clean up
		// Gdx.gl20.glBindTexture( GL20.GL_TEXTURE_2D, -1 ); // was null
		// Gdx.gl20.glBindRenderbuffer( GL20.GL_RENDERBUFFER, -1 );// was null
		// Gdx.gl20.glBindFramebuffer( GL20.GL_FRAMEBUFFER, -1 );// was null
	}

	/**
	 * render shader on mesh, update shader vars for frame
	 */
	private void renderShaderonMesh() {

		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT); // useful ??
		shader.begin();

		// gl.uniform1i( currentProgram.uniformsCache[ 'backbuffer' ], 0 );
		// gl.uniform2f( currentProgram.uniformsCache[ 'surfaceSize' ],
		// surface.width, surface.height );
		//
		// gl.bindBuffer( gl.ARRAY_BUFFER, surface.buffer );
		// gl.vertexAttribPointer( surface.positionAttribute, 2, gl.FLOAT,
		// false, 0, 0 );
		//
		// gl.bindBuffer( gl.ARRAY_BUFFER, buffer );
		// gl.vertexAttribPointer( vertexPosition, 2, gl.FLOAT, false, 0, 0 );

		// TODO set constants outside render ?
		shader.setUniformf("resolution", effectiveSurfaceWidth,
				effectiveSurfaceHeight);

		shader.setUniformi("backbuffer", 0);
		shader.setUniformf("surfaceSize", renderSurfaceWidth,
				renderSurfaceHeight);

		// Temp

		Gdx.gl20.glEnableVertexAttribArray(positionIdx);
		Gdx.gl20.glVertexAttribPointer(positionIdx, 2, GL20.GL_FLOAT, false, 0,
				renderVerticesBuffer);

		Gdx.gl20.glEnableVertexAttribArray(surfacePosAttrIdx);
		Gdx.gl20.glVertexAttribPointer(surfacePosAttrIdx, 2, GL20.GL_FLOAT,
				false, 0, verticesBuffer);

		// shader.setUniformMatrix("u_mvpMatrix", cam.combined);

		// shader.setVertexAttribute(screenDimVBO, size, type, normalize,
		// stride, buffer)

		shader.setUniformf("time", time);

		// TODO : mouse cursor position should be virtualized / setup as user
		// function parameter
		shader.setUniformf("mouse", mouseCursorX, mouseCursorY);

		// real thing
		// mesh.render(shader, GL20.GL_TRIANGLES);

		Gdx.gl20.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

		// Gdx.gl20.glEnableVertexAttribArray(0);
		
		// le buffer n'a pas d'impact sur glDrawArrays ?
		// à moins que cela ne soit parce qu'il n'y en a pas d'autres..
		
		//Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
		 Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, handles.get(0));
		//
		// Gdx.gl20.glVertexAttribPointer(
		// 0, // attribute 0. No particular reason for 0, but must match the
		// layout in the shader.
		// 6, // size
		// GL20.GL_FLOAT, // type
		// false, // normalized?
		// 0, // stride
		// verticesBuffer // array buffer offset
		// );
		//

		Gdx.gl20.glDrawArrays(GL20.GL_TRIANGLES, 0, 6); // 2 triangles ou
														// unitSurfaces.length ?

		// Gdx.gl20.glDisableVertexAttribArray(0);

		// // IMPORTANT: Unbind from the buffer when we're done with it.
		// GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		Gdx.gl20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);

		shader.end();

	}

	/**
	 * @param maxX
	 * @param maxY
	 * @return mesh setup for rectangular view
	 */
	public static Mesh genUnitRectangle() {

		float x1 = -1.0f;
		float y1 = -1.0f;
		float x2 = 1.0f;
		float y2 = 1.0f;

		Mesh mesh = new Mesh(true, 4, 6, new VertexAttribute(Usage.Position, 3,
				ATTR_POSITION));

		mesh.setVertices(new float[] { x1, y1, 1.0f, x2, y1, 1.0f, x1, y2,
				1.0f, x2, y2, 1.f });
		mesh.setIndices(new short[] { 0, 1, 2, 1, 3, 2 });

		return mesh;
	}

	static class HerokuSampleShader extends CustomShader {
		private static final String DATA_SHADERS_HEROKUWIGGLE1_FRAG = "data/shaders/herokuwiggle1.frag";

		public HerokuSampleShader() {
			super(Gdx.files.internal(DATA_SHADERS_HEROKUWIGGLE1_FRAG)
					.readString());
		}
	}

	private static class CustomShader extends ShaderProgram {
		private static final String SHADER_COMPILATION_FAILED = "Shader compilation failed:\n";
		private static final String DATA_SHADERS_HEROKUBASE_VERT = "data/shaders/surfacePos.vert";

		public CustomShader(String customFragShader) {

			super(
					Gdx.files.internal(DATA_SHADERS_HEROKUBASE_VERT)
							.readString(), customFragShader);

			if (!isCompiled()) {
				throw new RuntimeException(SHADER_COMPILATION_FAILED + getLog());
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
		// try {
		// Thread.sleep(500);
		// } catch (Exception ex) {
		// Gdx.app.log("resume", "resume/delay", ex);
		// }
		// ou reporter au premier render ?

		// Force recreation of buffers
		m_fbo = null;

		reserveRessources();
	}

	private void reserveRessources() {
		batch = new SpriteBatch();
		font = new BitmapFont();

		// Callback pour forcer le rechargement du shader
		setupShader();

		// if (m_fboEnabled) {
		initRenderFramebufferIfNeeded();
		// }
	}

	// handler for ???
	public void previewStateChange(boolean isPreview) {
	}

	/**
	 * handler for touchDown Update cursor position if useful
	 */
	public boolean touchDown(float x, float y, int pointer, int button) {
		if (listener != null) {
			listener.onClick((int) x, (int) y);
		}
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

	public void addClickHandler(ClickHandler listener) {
		this.listener = listener;

	}

	@Override
	public void offsetChange(float xOffset, float yOffset, float xOffsetStep,
			float yOffsetStep, int xPixelOffset, int yPixelOffset) {

		// Translation management need fragment shader support
		// By default shader uses screen coordinates, which are fixed.

		// offset can be provided as a custom attribute, but the shader must use
		// it.

	}

	public void addReqFailCallback(ReqFailCallback callback) {
		this.reqFailCallback = callback;
	}
}
