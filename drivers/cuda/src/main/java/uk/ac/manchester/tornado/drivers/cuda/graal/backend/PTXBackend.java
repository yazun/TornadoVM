package uk.ac.manchester.tornado.drivers.cuda.graal.backend;

import jdk.vm.ci.code.CompilationRequest;
import jdk.vm.ci.code.CompiledCode;
import jdk.vm.ci.code.Register;
import jdk.vm.ci.code.RegisterConfig;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import org.graalvm.compiler.asm.Assembler;
import org.graalvm.compiler.code.CompilationResult;
import org.graalvm.compiler.core.common.CompilationIdentifier;
import org.graalvm.compiler.core.common.alloc.RegisterAllocationConfig;
import org.graalvm.compiler.lir.LIR;
import org.graalvm.compiler.lir.asm.CompilationResultBuilder;
import org.graalvm.compiler.lir.asm.CompilationResultBuilderFactory;
import org.graalvm.compiler.lir.framemap.FrameMap;
import org.graalvm.compiler.lir.framemap.FrameMapBuilder;
import org.graalvm.compiler.lir.framemap.ReferenceMapBuilder;
import org.graalvm.compiler.lir.gen.LIRGenerationResult;
import org.graalvm.compiler.lir.gen.LIRGeneratorTool;
import org.graalvm.compiler.nodes.StructuredGraph;
import org.graalvm.compiler.nodes.spi.NodeLIRBuilderTool;
import org.graalvm.compiler.phases.tiers.SuitesProvider;
import org.graalvm.util.EconomicSet;
import uk.ac.manchester.tornado.drivers.cuda.CUDADeviceContext;
import uk.ac.manchester.tornado.drivers.cuda.graal.PTXProviders;
import uk.ac.manchester.tornado.runtime.graal.backend.TornadoBackend;
import uk.ac.manchester.tornado.runtime.graal.compiler.TornadoSuitesProvider;

public class PTXBackend extends TornadoBackend<PTXProviders> implements FrameMap.ReferenceMapBuilderFactory {

    final CUDADeviceContext deviceContext;

    public PTXBackend(PTXProviders providers, CUDADeviceContext deviceContext) {
        super(providers);

        this.deviceContext = deviceContext;
        init();
    }

    @Override
    public String decodeDeopt(long value) {
        return null;
    }

    @Override
    public SuitesProvider getSuites() {
        return null;
    }

    @Override
    public FrameMapBuilder newFrameMapBuilder(RegisterConfig registerConfig) {
        return null;
    }

    @Override
    public RegisterAllocationConfig newRegisterAllocationConfig(RegisterConfig registerConfig, String[] allocationRestrictedTo) {
        return null;
    }

    @Override
    public FrameMap newFrameMap(RegisterConfig registerConfig) {
        return null;
    }

    @Override
    public LIRGeneratorTool newLIRGenerator(LIRGenerationResult lirGenRes) {
        return null;
    }

    @Override
    public LIRGenerationResult newLIRGenerationResult(CompilationIdentifier compilationId, LIR lir, FrameMapBuilder frameMapBuilder, StructuredGraph graph, Object stub) {
        return null;
    }

    @Override
    public NodeLIRBuilderTool newNodeLIRBuilder(StructuredGraph graph, LIRGeneratorTool lirGen) {
        return null;
    }

    @Override
    protected Assembler createAssembler(FrameMap frameMap) {
        return null;
    }

    @Override
    public CompilationResultBuilder newCompilationResultBuilder(LIRGenerationResult lirGenResult, FrameMap frameMap, CompilationResult compilationResult,
            CompilationResultBuilderFactory factory) {
        return null;
    }

    @Override
    protected CompiledCode createCompiledCode(ResolvedJavaMethod method, CompilationRequest compilationRequest, CompilationResult compilationResult) {
        return null;
    }

    @Override
    public void emitCode(CompilationResultBuilder crb, LIR lir, ResolvedJavaMethod installedCodeOwner) {

    }

    @Override
    public EconomicSet<Register> translateToCallerRegisters(EconomicSet<Register> calleeRegisters) {
        return null;
    }

    @Override
    public ReferenceMapBuilder newReferenceMapBuilder(int totalFrameSize) {
        return null;
    }

    public TornadoSuitesProvider getTornadoSuites() {

        return ((PTXProviders) getProviders()).getSuitesProvider();
    }

    public boolean isInitialised() {
        return false;
    }

    public void init() {
        allocateHeapMemoryOnDevice();
    }

    /**
     * It allocates the smallest of the requested heap size or the max global memory
     * size.
     */
    public void allocateHeapMemoryOnDevice() {
        //long memorySize = Math.min(DEFAULT_HEAP_ALLOCATION, deviceContext.getDevice().getDeviceMaxAllocationSize());
        //if (memorySize < DEFAULT_HEAP_ALLOCATION) {
            //Tornado.info("Unable to allocate %s of heap space - resized to %s", humanReadableByteCount(DEFAULT_HEAP_ALLOCATION, false), humanReadableByteCount(memorySize, false));
        //}
        //Tornado.info("%s: allocating %s of heap space", deviceContext.getDevice().getDeviceName(), humanReadableByteCount(memorySize, false));
        deviceContext.getMemoryManager().allocateRegion(DEFAULT_HEAP_ALLOCATION);
    }

    public CUDADeviceContext getDeviceContext() {
        return deviceContext;
    }
}
