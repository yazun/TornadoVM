package tornado.drivers.opencl.graal.nodes.vector;

import com.oracle.graal.api.meta.Value;
import com.oracle.graal.graph.Node;
import com.oracle.graal.graph.NodeClass;
import com.oracle.graal.graph.spi.CanonicalizerTool;
import com.oracle.graal.lir.Variable;
import com.oracle.graal.nodeinfo.NodeInfo;
import com.oracle.graal.nodes.ValueNode;
import com.oracle.graal.nodes.calc.BinaryNode;
import com.oracle.graal.nodes.spi.LIRLowerable;
import com.oracle.graal.nodes.spi.NodeLIRBuilderTool;
import tornado.common.Tornado;
import tornado.drivers.opencl.graal.OCLStamp;
import tornado.drivers.opencl.graal.OCLStampFactory;
import tornado.drivers.opencl.graal.asm.OpenCLAssembler.OCLBinaryOp;
import tornado.drivers.opencl.graal.lir.OCLBinary;
import tornado.drivers.opencl.graal.lir.OCLKind;
import tornado.drivers.opencl.graal.lir.OCLLIRInstruction.AssignStmt;

@NodeInfo(shortName = "+")
public class VectorAddNode extends BinaryNode implements LIRLowerable, VectorOp {

    public static final NodeClass<VectorAddNode> TYPE = NodeClass.create(VectorAddNode.class);

    public VectorAddNode(OCLKind kind, ValueNode x, ValueNode y) {
        super(TYPE, OCLStampFactory.getStampFor(kind), x, y);
    }

    @Override
    public void generate(NodeLIRBuilderTool gen) {

        final Variable result = gen.getLIRGeneratorTool().newVariable(stamp.getLIRKind(null));
        
        final Value input1 = gen.operand(x);
        final Value input2 = gen.operand(y);

        Tornado.trace("emitVectorAdd: %s + %s", input1, input2);
        OCLStamp stamp = (OCLStamp) stamp();
        gen.getLIRGeneratorTool().append(new AssignStmt(result,new OCLBinary.Expr(OCLBinaryOp.ADD, stamp.getLIRKind(null), input1, input2)));
        gen.setResult(this,result  );
    }

    @Override
    public Node canonical(CanonicalizerTool ct, ValueNode t, ValueNode t1) {
        return this;
    }

    @Override
    public Node canonical(CanonicalizerTool ct) {
        return this;
    }

}
