package filament

import backend.*
import backend.metal.MetalPlatform
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.sizeOf
import dev.romainguy.kotlin.math.*

/**
 * Engine is filament's main entry-point.
 *
 * An Engine instance main function is to keep track of all resources created by the user and
 * manage the rendering thread as well as the hardware renderer.
 *
 * To use filament, an Engine instance must be created first:
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * #include <filament/Engine.h>
 * using namespace filament;
 *
 * Engine* engine = Eng.create();
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Engine essentially represents (or is associated to) a hardware context
 * (e.g. an OpenGL ES context).
 *
 * Rendering typically happens in an operating system's window (which can be full screen), such
 * window is managed by a filament.Renderer.
 *
 * A typical filament render loop looks like this:
 *
 *
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 * #include <filament/Engine.h>
 * #include <filament/Renderer.h>
 * #include <filament/Scene.h>
 * #include <filament/View.h>
 * using namespace filament;
 *
 * Engine* engine       = Engine.create();
 * SwapChain* swapChain = engine.createSwapChain(nativeWindow);
 * Renderer* renderer   = engine.createRenderer();
 * Scene* scene         = engine.createScene();
 * View* view           = engine.createView();
 *
 * view.setScene(scene);
 *
 * do {
 *     // typically we wait for VSYNC and user input events
 *     if (renderer.beginFrame(swapChain)) {
 *         renderer.render(view);
 *         renderer.endFrame();
 *     }
 * } while (!quit);
 *
 * engine.destroy(view);
 * engine.destroy(scene);
 * engine.destroy(renderer);
 * engine.destroy(swapChain);
 * Engine.destroy(&engine); // clears engine*
 * ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 *
 * Resource Tracking
 * =================
 *
 *  Each Engine instance keeps track of all objects created by the user, such as vertex and index
 *  buffers, lights, cameras, etc...
 *  The user is expected to free those resources, however, leaked resources are freed when the
 *  engine instance is destroyed and a warning is emitted in the console.
 *
 * Thread safety
 * =============
 *
 * An Engine instance is not thread-safe. The implementation makes no attempt to synchronize
 * calls to an Engine instance methods.
 * If multi-threading is needed, synchronization must be external.
 *
 * Multi-threading
 * ===============
 *
 * When created, the Engine instance starts a render thread as well as multiple worker threads,
 * these threads have an elevated priority appropriate for rendering, based on the platform's
 * best practices. The number of worker threads depends on the platform and is automatically
 * chosen for best performance.
 *
 * On platforms with asymmetric cores (e.g. ARM's Big.Little), Engine makes some educated guesses
 * as to which cores to use for the render thread and worker threads. For example, it'll try to
 * keep an OpenGL ES thread on a Big core.
 *
 * Swap Chains
 * ===========
 *
 * A swap chain represents an Operating System's *native* renderable surface. Typically it's a window
 * or a view. Because a SwapChain is initialized from a native object, it is given to filament
 * as a `void*`, which must be of the proper type for each platform filament is running on.
 *
 * @see SwapChain
 *
 *
 * @see Renderer
 */
class Engine(
    val backend: Backend = Backend.DEFAULT,
    val platform: MetalPlatform = MetalPlatform,
    private val sharedGLContext: Any?
) {

    private val mVertexBuffers = ResourceList<VertexBuffer>("VertexBuffer")

    private lateinit var mFullScreenTriangleRph: HwRenderPrimitive
    private lateinit var mFullScreenTriangleVb: VertexBuffer
    private lateinit var mFullScreenTriangleIb: IndexBuffer
    private lateinit var mUvFromClipMatrix: Mat4
    private lateinit var mDefaultIblTexture: Texture;

    val driver: Driver = platform.createDriver(sharedGLContext)
    private val mCommandBufferQueue = CommandBufferQueue(CONFIG_MIN_COMMAND_BUFFERS_SIZE, CONFIG_COMMAND_BUFFERS_SIZE)
    val driverApi: DriverApi = DriverApi(driver, mCommandBufferQueue.circularBuffer)
    private var mResourceAllocator: ResourceAllocator = ResourceAllocator(driverApi)
    private val mOwnPlatform = true

    /**
     * Creates an instance of Engine
     *
     * @param backend           Which driver backend to use.
     *
     * @param platform          A pointer to an object that implements Platform. If this is
     *                          provided, then this object is used to create the hardware context
     *                          and expose platform features to it.
     *
     *                          If not provided (or nullptr is used), an appropriate Platform
     *                          is created automatically.
     *
     *                          All methods of this interface are called from filament's
     *                          render thread, which is different from the main thread.
     *
     *                          The lifetime of \p platform must exceed the lifetime of
     *                          the Engine object.
     *
     *  @param sharedGLContext  A platform-dependant OpenGL context used as a shared context
     *                          when creating filament's internal context.
     *                          Setting this parameter will force filament to use the OpenGL
     *                          implementation (instead of Vulkan for instance).
     *
     *
     * @return A pointer to the newly created Engine, or nullptr if the Engine couldn't be created.
     *
     * nullptr if the GPU driver couldn't be initialized, for instance if it doesn't
     * support the right version of OpenGL or OpenGL ES.
     *
     * @exception utils.PostConditionPanic can be thrown if there isn't enough memory to
     * allocate the command buffer. If exceptions are disabled, this condition if fatal and
     * this function will abort.
     *
     * \remark
     * This method is thread-safe.
     */
    suspend init {
        memScoped {

            mFullScreenTriangleVb = VertexBuffer.Builder().apply {
                vertexCount = 3
                bufferCount = 1
                attribute(VertexAttribute.POSITION, 0, ElementType.FLOAT4, 0)
            }.build(this@Engine) ?: error("fail to create mFullScreenTriangleVb")

            // these must be static because only a pointer is copied to the render stream
            // Note that these coordinates are specified in OpenGL clip space. Other backends can transform
            // these in the vertex shader as needed.
            val sFullScreenTriangleVertices = allocArrayOf(
                -1.0f, -1.0f, 1.0f, 1.0f,
                3.0f, -1.0f, 1.0f, 1.0f,
                -1.0f, 3.0f, 1.0f, 1.0f
            )
            val sFullScreenTriangleVerticesSize = sizeOf<FloatVar>() * 4 * 3
            mFullScreenTriangleVb.setBufferAt(
                this@Engine,
                0,
                BufferDescriptor(sFullScreenTriangleVertices, sFullScreenTriangleVerticesSize)
            )

            mFullScreenTriangleIb =
                IndexBuffer.Builder().apply {
                    indexCount = 3
                    bufferType = IndexBuffer.IndexType.USHORT
                }.build(this@Engine) ?: error("fail to create mFullScreenTriangleIb")


            mFullScreenTriangleIb.setBuffer(
                this@Engine,
                BufferDescriptor(sFullScreenTriangleVertices, sFullScreenTriangleVerticesSize)
            );

            mFullScreenTriangleRph = driver.createRenderPrimitive()
            driver.setRenderPrimitiveBuffer(
                mFullScreenTriangleRph,
                mFullScreenTriangleVb, mFullScreenTriangleIb
            )
            driver.setRenderPrimitiveRange(
                mFullScreenTriangleRph, PrimitiveType.TRIANGLES,
                0u, 0, 2, mFullScreenTriangleIb.indexCount
            )

            // Compute a clip-space [-1 to 1] to texture space [0 to 1] matrix, taking into account
            // backend differences.
            val textureSpaceYFlipped = backend == Backend.METAL || backend == Backend.VULKAN;
            if (textureSpaceYFlipped) {
                mUvFromClipMatrix = Mat4(
                    Float4(0.5f, 0.0f, 0.0f, 0.5f),
                    Float4(0.0f, -0.5f, 0.0f, 0.5f),
                    Float4(0.0f, 0.0f, 1.0f, 0.0f),
                    Float4(0.0f, 0.0f, 0.0f, 1.0f)
                )
            } else {
                mUvFromClipMatrix = Mat4(
                    Float4(0.5f, 0.0f, 0.0f, 0.5f),
                    Float4(0.0f, 0.5f, 0.0f, 0.5f),
                    Float4(0.0f, 0.0f, 1.0f, 0.0f),
                    Float4(0.0f, 0.0f, 0.0f, 1.0f)
                )
            }

            mDefaultIblTexture = Texture.Builder(
                width = 1,
                height = 1,
                level = 1,
                format = TextureFormat.RGBA8,
                sampler = SamplerType.SAMPLER_CUBEMAP
            ).build(this@Engine) ?: error("fail to create texture")


            // 3 bands = 9 float3
            val sh = Array<float>(9 * 3) { 0.0f }
            mDefaultIbl =  IndirectLight.Builder()
                    .irradiance(3, sh)
                    .build(this@Engine)

            mDefaultColorGrading = upcast(ColorGrading.Builder().build(this));

            // Always initialize the default material, most materials' depth shaders fallback on it.
            mDefaultMaterial =
                FMaterial.DefaultMaterialBuilder()
                    .package(MATERIALS_DEFAULTMATERIAL_DATA, MATERIALS_DEFAULTMATERIAL_SIZE)
                    .build(*const_cast < FEngine * >(this))


            // Create a dummy morph target buffer.
            mDummyMorphTargetBuffer = createMorphTargetBuffer(FMorphTargetBuffer.EmptyMorphTargetBuilder());

            float3 dummyPositions [1] = {};
            short4 dummyTangents [1] = {};
            mDummyMorphTargetBuffer.setPositionsAt(this, 0, dummyPositions, 1, 0);
            mDummyMorphTargetBuffer.setTangentsAt(this, 0, dummyTangents, 1, 0);

            // create dummy textures we need throughout the engine

            mDummyOneTexture = driverApi.createTexture(
                SamplerType.SAMPLER_2D, 1,
                TextureFormat.RGBA8, 1, 1, 1, 1, TextureUsage.DEFAULT
            );

            mDummyOneTextureArray = driverApi.createTexture(
                SamplerType.SAMPLER_2D_ARRAY, 1,
                TextureFormat.RGBA8, 1, 1, 1, 1, TextureUsage.DEFAULT
            );

            mDummyZeroTextureArray = driverApi.createTexture(
                SamplerType.SAMPLER_2D_ARRAY, 1,
                TextureFormat.RGBA8, 1, 1, 1, 1, TextureUsage.DEFAULT
            );

            mDummyOneIntegerTextureArray = driverApi.createTexture(
                SamplerType.SAMPLER_2D_ARRAY, 1,
                TextureFormat.RGBA8I, 1, 1, 1, 1, TextureUsage.DEFAULT
            );

            mDummyZeroTexture = driverApi.createTexture(
                SamplerType.SAMPLER_2D, 1,
                TextureFormat.RGBA8, 1, 1, 1, 1, TextureUsage.DEFAULT
            );


            // initialize the dummy textures so that their contents are not undefined

            using PixelBufferDescriptor = Texture . PixelBufferDescriptor;

            static const uint32_t zeroes = 0;
            static const uint32_t ones = 0xffffffff;
            static const uint32_t signedOnes = 0x7f7f7f7f;

            mDefaultIblTexture.setImage(
                *this, 0,
                PixelBufferDescriptor(& zeroes, 4, Texture.Format.RGBA, Texture.Type.UBYTE
            ), {});

            driverApi.update2DImage(
                mDummyOneTexture, 0, 0, 0, 1, 1,
                PixelBufferDescriptor(& ones, 4, Texture.Format.RGBA, Texture.Type.UBYTE
            ));

            driverApi.update3DImage(
                mDummyOneTextureArray, 0, 0, 0, 0, 1, 1, 1,
                PixelBufferDescriptor(& ones, 4, Texture.Format.RGBA, Texture.Type.UBYTE
            ));

            driverApi.update3DImage(
                mDummyOneIntegerTextureArray, 0, 0, 0, 0, 1, 1, 1,
                PixelBufferDescriptor(& signedOnes, 4, Texture.Format.RGBA_INTEGER, Texture.Type.BYTE
            ));

            driverApi.update3DImage(
                mDummyZeroTexture, 0, 0, 0, 0, 1, 1, 1,
                PixelBufferDescriptor(& zeroes, 4, Texture.Format.RGBA, Texture.Type.UBYTE
            ));

            driverApi.update3DImage(
                mDummyZeroTextureArray, 0, 0, 0, 0, 1, 1, 1,
                PixelBufferDescriptor(& zeroes, 4, Texture.Format.RGBA, Texture.Type.UBYTE
            ));

            mDefaultRenderTarget = driverApi.createDefaultRenderTarget();

            mPostProcessManager.init();
            mLightManager.init(*this);
            mDFG.init(*this);

        }
    }


    fun createVertexBuffer(builder: VertexBuffer.Builder): VertexBuffer? {
        TODO("Not yet implemented")
    }

    fun createIndexBuffer(builder: IndexBuffer.Builder): IndexBuffer {
        TODO("Not yet implemented")
    }

    fun createTexture(builder: Texture.Builder): Texture {
        TODO("Not yet implemented")
    }

    fun createIndirectLight(builder: IndirectLight.Builder): IndirectLight? {
        TODO("Not yet implemented")
    }
}

