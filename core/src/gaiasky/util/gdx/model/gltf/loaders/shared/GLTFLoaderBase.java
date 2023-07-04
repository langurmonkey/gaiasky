/*
 * This file is part of the gdx-gltf (https://github.com/mgsx-dev/gdx-gltf) library,
 * under the APACHE 2.0 license. We have possibly modified parts of this file so that
 * 32-bit integer indices are supported, instead of only 16-bit short ones.
 */

package gaiasky.util.gdx.model.gltf.loaders.shared;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.environment.BaseLight;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.ObjectSet;
import gaiasky.util.gdx.mesh.IntMesh;
import gaiasky.util.gdx.model.IntMeshPart;
import gaiasky.util.gdx.model.IntModel;
import gaiasky.util.gdx.model.IntNode;
import gaiasky.util.gdx.model.IntNodePart;
import gaiasky.util.gdx.model.gltf.data.GLTF;
import gaiasky.util.gdx.model.gltf.data.camera.GLTFCamera;
import gaiasky.util.gdx.model.gltf.data.extensions.*;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFNode;
import gaiasky.util.gdx.model.gltf.data.scene.GLTFScene;
import gaiasky.util.gdx.model.gltf.loaders.exceptions.GLTFUnsupportedException;
import gaiasky.util.gdx.model.gltf.loaders.shared.animation.AnimationLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataFileResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.data.DataResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.geometry.MeshLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.material.MaterialLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.material.PBRMaterialLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.scene.NodeResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.scene.SkinLoader;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.ImageResolver;
import gaiasky.util.gdx.model.gltf.loaders.shared.texture.TextureResolver;
import gaiasky.util.gdx.model.gltf.scene3d.model.NodePlus;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneAsset;
import gaiasky.util.gdx.model.gltf.scene3d.scene.SceneModel;
import gaiasky.util.gdx.shader.Material;

public class GLTFLoaderBase implements Disposable {

	public static final String TAG = "GLTF";
	
	public final static ObjectSet<String> supportedExtensions = new ObjectSet<>();

	static{
		supportedExtensions.addAll(
			KHRMaterialsPBRSpecularGlossiness.EXT,
			KHRTextureTransform.EXT,
			KHRLightsPunctual.EXT,
			KHRMaterialsUnlit.EXT,
			KHRMaterialsTransmission.EXT,
			KHRMaterialsVolume.EXT,
			KHRMaterialsIOR.EXT,
			KHRMaterialsSpecular.EXT,
			KHRMaterialsIridescence.EXT,
			KHRMaterialsEmissiveStrength.EXT
		);
	}
	
	private static final ObjectSet<Material> materialSet = new ObjectSet<>();
	private static final ObjectSet<IntMeshPart> meshPartSet = new ObjectSet<>();
	private static final ObjectSet<IntMesh> meshSet = new ObjectSet<>();
	private final ObjectSet<IntMesh> loadedMeshes = new ObjectSet<>();
	
	private final Array<Camera> cameras = new Array<>();
	private final Array<BaseLight> lights = new Array<>();
	
	/** node name to light index */
	private final ObjectMap<String, Integer> lightMap = new ObjectMap<>();
	
	
	/** node name to camera index */
	private final ObjectMap<String, Integer> cameraMap = new ObjectMap<>();

	private final Array<SceneModel> scenes = new Array<>();
	
	protected GLTF glModel;
	
	protected DataFileResolver dataFileResolver;
	protected MaterialLoader materialLoader;
	protected TextureResolver textureResolver;
	protected AnimationLoader animationLoader;
	protected DataResolver dataResolver;
	protected SkinLoader skinLoader;
	protected NodeResolver nodeResolver;
	protected MeshLoader meshLoader;
	protected ImageResolver imageResolver;
	
	public GLTFLoaderBase() 
	{
		this(null);
	}
	public GLTFLoaderBase(TextureResolver textureResolver)
	{
		this.textureResolver = textureResolver;
		animationLoader = new AnimationLoader();
		nodeResolver = new NodeResolver();
		meshLoader = new MeshLoader();
		skinLoader = new SkinLoader();
	}
	
	public SceneAsset load(DataFileResolver dataFileResolver, boolean withData){
		try{
			this.dataFileResolver = dataFileResolver;
			
			glModel = dataFileResolver.getRoot();
			
			// Pre-requisites (mandatory)
			if(glModel.extensionsRequired != null){
				for(String extension : glModel.extensionsRequired){
					if(!supportedExtensions.contains(extension)){
						throw new GLTFUnsupportedException("Extension " + extension + " required but not supported");
					}
				}
			}
			// Pre-requisites (optional)
			if(glModel.extensionsUsed != null){
				for(String extension : glModel.extensionsUsed){
					if(!supportedExtensions.contains(extension)){
						Gdx.app.error(TAG, "Extension " + extension + " used but not supported");
					}
				}
			}
			
			// Load dependencies from lower to higher
			
			// Images (pixmaps)
			dataResolver = new DataResolver(glModel, dataFileResolver);
			
			if(textureResolver == null){
				imageResolver = new ImageResolver(dataFileResolver); // TODO no longer necessary
				imageResolver.load(glModel.images);
				textureResolver = new TextureResolver();
				textureResolver.loadTextures(glModel.textures, glModel.samplers, imageResolver);
			}
			
			materialLoader = createMaterialLoader(textureResolver);
			materialLoader.loadMaterials(glModel.materials);
			
			loadCameras();
			loadLights();
			loadScenes();
			
			animationLoader.load(glModel.animations, nodeResolver, dataResolver);
			skinLoader.load(glModel.skins, glModel.nodes, nodeResolver, dataResolver);
			
			// create scene asset
			SceneAsset model = new SceneAsset();
			if(withData) model.data = glModel;
			model.scenes = scenes;
			model.scene = scenes.get(glModel.scene);
			model.maxBones = skinLoader.getMaxBones();
			model.textures = textureResolver.getTextures(new Array<>());
			if(imageResolver != null){
				model.pixmaps = imageResolver.getPixmaps(new Array<>());
				imageResolver.clear();
			}
			model.animations = animationLoader.animations;
			// XXX don't know where the animation are ...
			for(SceneModel scene : model.scenes){
				scene.model.animations.addAll(animationLoader.animations);
			}
			
			copy(loadedMeshes, model.meshes = new Array<>());
			loadedMeshes.clear();
			
			return model;
		}catch(RuntimeException e){
			dispose();
			throw e;
		}
	}
	
	protected MaterialLoader createMaterialLoader(TextureResolver textureResolver) {
		return new PBRMaterialLoader(textureResolver);
	}
	
	private void loadLights() {
		if(glModel.extensions != null){
			KHRLightsPunctual.GLTFLights lightExt = glModel.extensions.get(KHRLightsPunctual.GLTFLights.class, KHRLightsPunctual.EXT);
			if(lightExt != null){
				for(KHRLightsPunctual.GLTFLight light : lightExt.lights){
					lights.add(KHRLightsPunctual.map(light));
				}
			}
		}
	}

	@Override
	public void dispose() {
		if(imageResolver != null){
			imageResolver.dispose();
		}
		if(textureResolver != null){
			textureResolver.dispose();
		}
		for(SceneModel scene : scenes){
			scene.dispose();
		}
		for(IntMesh mesh : loadedMeshes){
			mesh.dispose();
		}
		loadedMeshes.clear();
	}

	private void loadScenes() {
		for(int i=0 ; i<glModel.scenes.size ; i++)
		{
			scenes.add(loadScene(glModel.scenes.get(i)));
		}
	}

	private void loadCameras() {
		if(glModel.cameras != null){
			for(GLTFCamera glCamera : glModel.cameras){
				cameras.add(GLTFTypes.map(glCamera));
			}
		}
	}
	
	private SceneModel loadScene(GLTFScene gltfScene)
	{
		SceneModel sceneModel = new SceneModel();
		sceneModel.name = gltfScene.name;
		sceneModel.model = new IntModel();
		
		// add root nodes
		if(gltfScene.nodes != null){
			for(int id : gltfScene.nodes){
				sceneModel.model.nodes.add(getNode(id));
			}
		}
		// add scene cameras (filter from all scenes cameras)
		for(Entry<String, Integer> entry : cameraMap){
			IntNode node = sceneModel.model.getNode(entry.key, true);
			if(node != null) sceneModel.cameras.put(node, cameras.get(entry.value));
		}
		// add scene lights (filter from all scenes lights)
		for(Entry<String, Integer> entry : lightMap){
			IntNode node = sceneModel.model.getNode(entry.key, true);
			if(node != null) sceneModel.lights.put(node, lights.get(entry.value));
		}

		// collect data references to store in model
		collectData(sceneModel.model, sceneModel.model.nodes);
		
		loadedMeshes.addAll(meshSet);
		
		copy(meshSet, sceneModel.model.meshes);
		copy(meshPartSet, sceneModel.model.meshParts);
		copy(materialSet, sceneModel.model.materials);
		
		meshSet.clear();
		meshPartSet.clear();
		materialSet.clear();
		
		return sceneModel;
	}
	
	private void collectData(IntModel model, Iterable<IntNode> nodes){
		for(IntNode node : nodes){
			for(IntNodePart part : node.parts){
				meshSet.add(part.meshPart.mesh);
				meshPartSet.add(part.meshPart);
				materialSet.add(part.material);
			}
			collectData(model, node.getChildren());
		}
	}
	
	private static <T> void copy(ObjectSet<T> src, Array<T> dst){
		for(T e : src) dst.add(e);
	}

	private IntNode getNode(int id)
	{
		IntNode node = nodeResolver.get(id);
		if(node == null){
			node = new NodePlus();
			nodeResolver.put(id, node);
			
			GLTFNode glNode = glModel.nodes.get(id);
			
			if(glNode.matrix != null){
				Matrix4 matrix = new Matrix4(glNode.matrix);
				matrix.getTranslation(node.translation);
				matrix.getScale(node.scale);
				matrix.getRotation(node.rotation, true);
			}else{
				if(glNode.translation != null){
					GLTFTypes.map(node.translation, glNode.translation);
				}
				if(glNode.rotation != null){
					GLTFTypes.map(node.rotation, glNode.rotation);
				}
				if(glNode.scale != null){
					GLTFTypes.map(node.scale, glNode.scale);
				}
			}
			
			node.id = glNode.name == null ? "glNode " + id : glNode.name;
			
			if(glNode.children != null){
				for(int childId : glNode.children){
					node.addChild(getNode(childId));
				}
			}
			
			if(glNode.mesh != null){
				meshLoader.load(node, glModel.meshes.get(glNode.mesh), dataResolver, materialLoader);
			}
			
			if(glNode.camera != null){
				cameraMap.put(node.id, glNode.camera);
			}
			
			// node extensions
			if(glNode.extensions != null){
				KHRLightsPunctual.GLTFLightNode nodeLight = glNode.extensions.get(KHRLightsPunctual.GLTFLightNode.class, KHRLightsPunctual.EXT);
				if(nodeLight != null){
					lightMap.put(node.id, nodeLight.light);
				}
			}
			
		}
		return node;
	}
}
