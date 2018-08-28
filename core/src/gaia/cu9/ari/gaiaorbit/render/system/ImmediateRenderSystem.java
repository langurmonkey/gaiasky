package gaia.cu9.ari.gaiaorbit.render.system;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;

import gaia.cu9.ari.gaiaorbit.scenegraph.SceneGraphNode.RenderGroup;
import gaia.cu9.ari.gaiaorbit.util.Logger;
import gaia.cu9.ari.gaiaorbit.util.Logger.Log;

public abstract class ImmediateRenderSystem extends AbstractRenderSystem {
        protected static final Log logger = Logger.getLogger(ImmediateRenderSystem.class);

        protected static final int DEFAULT_VERTICES_SIZE = 1500000;
	protected static final int shortLimit = (int) Math.pow(2, 2 * 8);

	protected int meshIdx;
	protected MeshData[] meshes;
	protected MeshData curr;
	// Auxiliary array that holds vertices temporarily
	protected float[] vertices;

	protected class MeshData {

		protected Mesh mesh;

		protected int colorOffset;

		protected int vertexIdx;
		// Size of each vertex in number of entries in array. Multiply by array type
		// size to get vertex size in bytes.
		protected int vertexSize;
		// Vertex array, this usually is just a reference to an external temp array
		protected float[] vertices;

		protected int indexIdx;
		protected short indexVert;
		protected short[] indices;
		protected int numVertices;

		public void clear() {
			vertexIdx = 0;
			indexIdx = 0;
			indexVert = 0;
			numVertices = 0;
		}
	}

	protected int maxVertices;

	protected ImmediateRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs) {
		this(rg, alphas, programs, -1);
	}

	protected ImmediateRenderSystem(RenderGroup rg, float[] alphas, ShaderProgram[] programs, int numVertices) {
		super(rg, alphas, programs);
		initShaderProgram();
		initVertices();
		meshIdx = 0;
		if (numVertices > 0)
			vertices = new float[numVertices];
	}

	protected abstract void initShaderProgram();

	protected abstract void initVertices();

	public void color(Color color) {
		curr.vertices[curr.vertexIdx + curr.colorOffset] = color.toFloatBits();
	}

	public void color(float r, float g, float b, float a) {
		curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits(r, g, b, a);
	}

	public void color(double r, double g, double b, double a) {
		curr.vertices[curr.vertexIdx + curr.colorOffset] = Color.toFloatBits((float) r, (float) g, (float) b,
				(float) a);
	}

	public void color(float colorBits) {
		curr.vertices[curr.vertexIdx + curr.colorOffset] = colorBits;
	}

	public void vertex(float x, float y, float z) {
		curr.vertices[curr.vertexIdx] = x;
		curr.vertices[curr.vertexIdx + 1] = y;
		curr.vertices[curr.vertexIdx + 2] = z;

		curr.vertexIdx += curr.vertexSize;
		curr.numVertices++;
	}

	protected void checkRequiredVerticesSize(int requiredSize) {
		if (vertices.length < requiredSize) {
			logger.info("Allocating new vertex array: " + vertices.length + " > " + requiredSize);
			vertices = new float[requiredSize];
		}
	}
}
