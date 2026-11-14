package com.huzeifa.cubomagico

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.MotionEvent
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class CubeGLView(context: Context) : GLSurfaceView(context) {
    private val cubeRenderer = CubeRenderer()
    private var previousX = 0f
    private var previousY = 0f
    init {
        setEGLContextClientVersion(2)
        setRenderer(cubeRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> { previousX = event.x; previousY = event.y; return true }
            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - previousX
                val dy = event.y - previousY
                previousX = event.x; previousY = event.y
                cubeRenderer.rotate(dx * 0.45f, dy * 0.45f)
                return true
            }
        }
        return true
    }
}

private class CubeRenderer : GLSurfaceView.Renderer {
    private var program = 0
    private var vertexBuffer: FloatBuffer? = null
    private var vertexCount = 0
    private var yaw = -28f
    private var pitch = 22f
    private val projection = FloatArray(16)
    private val view = FloatArray(16)
    private val model = FloatArray(16)
    private val mvp = FloatArray(16)
    fun rotate(dx: Float, dy: Float) { yaw += dx; pitch = (pitch + dy).coerceIn(-70f, 70f) }

    override fun onSurfaceCreated(gl: javax.microedition.khronos.opengles.GL10?, config: javax.microedition.khronos.egl.EGLConfig?) {
        GLES20.glClearColor(0.055f, 0.067f, 0.086f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        val vertex = """
            attribute vec3 aPosition;
            attribute vec3 aColor;
            uniform mat4 uMVP;
            varying vec3 vColor;
            void main() { gl_Position = uMVP * vec4(aPosition, 1.0); vColor = aColor; }
        """.trimIndent()
        val fragment = """
            precision mediump float;
            varying vec3 vColor;
            void main() { gl_FragColor = vec4(vColor, 1.0); }
        """.trimIndent()
        program = link(compile(GLES20.GL_VERTEX_SHADER, vertex), compile(GLES20.GL_FRAGMENT_SHADER, fragment))
        val data = buildVertices()
        vertexCount = data.size / 6
        vertexBuffer = ByteBuffer.allocateDirect(data.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(data); position(0) }
        Matrix.setLookAtM(view, 0, 0f, 0f, 6.2f, 0f, 0f, 0f, 0f, 1f, 0f)
    }

    override fun onSurfaceChanged(gl: javax.microedition.khronos.opengles.GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio = width.toFloat() / height.coerceAtLeast(1)
        if (ratio > 1f) Matrix.frustumM(projection, 0, -ratio, ratio, -1f, 1f, 3f, 10f)
        else Matrix.frustumM(projection, 0, -1f, 1f, -1f / ratio, 1f / ratio, 3f, 10f)
    }

    override fun onDrawFrame(gl: javax.microedition.khronos.opengles.GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        if (program == 0) return
        Matrix.setIdentityM(model, 0)
        Matrix.rotateM(model, 0, pitch, 1f, 0f, 0f)
        Matrix.rotateM(model, 0, yaw, 0f, 1f, 0f)
        Matrix.multiplyMM(mvp, 0, view, 0, model, 0)
        Matrix.multiplyMM(mvp, 0, projection, 0, mvp, 0)
        GLES20.glUseProgram(program)
        val pos = GLES20.glGetAttribLocation(program, "aPosition")
        val col = GLES20.glGetAttribLocation(program, "aColor")
        GLES20.glUniformMatrix4fv(GLES20.glGetUniformLocation(program, "uMVP"), 1, false, mvp, 0)
        val buffer = vertexBuffer ?: return
        buffer.position(0)
        GLES20.glEnableVertexAttribArray(pos)
        GLES20.glVertexAttribPointer(pos, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(3)
        GLES20.glEnableVertexAttribArray(col)
        GLES20.glVertexAttribPointer(col, 3, GLES20.GL_FLOAT, false, 24, buffer)
        buffer.position(0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount)
        GLES20.glDisableVertexAttribArray(pos)
        GLES20.glDisableVertexAttribArray(col)
    }

    private fun buildVertices(): FloatArray {
        val out = ArrayList<Float>()
        fun addTile(points: FloatArray, rgb: FloatArray) {
            intArrayOf(0, 1, 2, 0, 2, 3).forEach { p ->
                out.add(points[p * 3]); out.add(points[p * 3 + 1]); out.add(points[p * 3 + 2])
                out.add(rgb[0]); out.add(rgb[1]); out.add(rgb[2])
            }
        }
        fun quad(a: FloatArray, b: FloatArray, c: FloatArray, d: FloatArray, rgb: FloatArray) {
            addTile(floatArrayOf(
                a[0],a[1],a[2], b[0],b[1],b[2],
                c[0],c[1],c[2], d[0],d[1],d[2]
            ), rgb)
        }
        val green = floatArrayOf(0.10f, 0.72f, 0.43f)
        val red = floatArrayOf(0.90f, 0.18f, 0.25f)
        val white = floatArrayOf(0.95f, 0.95f, 0.95f)
        val gap = 0.035f
        for (row in 0..2) for (col in 0..2) {
            val x0 = -1f + col * (2f / 3f) + gap
            val x1 = -1f + (col + 1) * (2f / 3f) - gap
            val y1 = 1f - row * (2f / 3f) - gap
            val y0 = 1f - (row + 1) * (2f / 3f) + gap
            quad(floatArrayOf(x0,y1,1.02f), floatArrayOf(x1,y1,1.02f), floatArrayOf(x1,y0,1.02f), floatArrayOf(x0,y0,1.02f), green)
            val z1 = 1f - col * (2f / 3f) - gap
            val z0 = 1f - (col + 1) * (2f / 3f) + gap
            quad(floatArrayOf(1.02f,y1,z1), floatArrayOf(1.02f,y1,z0), floatArrayOf(1.02f,y0,z0), floatArrayOf(1.02f,y0,z1), red)
            val topZ0 = -1f + row * (2f / 3f) + gap
            val topZ1 = -1f + (row + 1) * (2f / 3f) - gap
            quad(floatArrayOf(x0,1.02f,topZ0), floatArrayOf(x1,1.02f,topZ0), floatArrayOf(x1,1.02f,topZ1), floatArrayOf(x0,1.02f,topZ1), white)
        }
        return out.toFloatArray()
    }
    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        return shader
    }
    private fun link(vertex: Int, fragment: Int): Int {
        val result = GLES20.glCreateProgram()
        GLES20.glAttachShader(result, vertex)
        GLES20.glAttachShader(result, fragment)
        GLES20.glLinkProgram(result)
        return result
    }
}
