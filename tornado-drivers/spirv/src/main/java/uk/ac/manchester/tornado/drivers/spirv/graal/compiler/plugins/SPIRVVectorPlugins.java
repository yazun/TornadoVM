/*
 * This file is part of Tornado: A heterogeneous programming framework:
 * https://github.com/beehive-lab/tornadovm
 *
 * Copyright (c) 2021-2022, APT Group, Department of Computer Science,
 * School of Engineering, The University of Manchester. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */
package uk.ac.manchester.tornado.drivers.spirv.graal.compiler.plugins;

import static uk.ac.manchester.tornado.api.exceptions.TornadoInternalError.guarantee;

import org.graalvm.compiler.core.common.type.ObjectStamp;
import org.graalvm.compiler.core.common.type.StampPair;
import org.graalvm.compiler.nodes.ParameterNode;
import org.graalvm.compiler.nodes.PiNode;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderConfiguration.Plugins;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderTool;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins.Registration;
import org.graalvm.compiler.nodes.graphbuilderconf.NodePlugin;
import org.graalvm.compiler.nodes.java.StoreIndexedNode;
import org.graalvm.compiler.nodes.memory.address.AddressNode;
import org.graalvm.compiler.nodes.memory.address.OffsetAddressNode;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;
import jdk.vm.ci.meta.ResolvedJavaType;
import uk.ac.manchester.tornado.api.type.annotations.Vector;
import uk.ac.manchester.tornado.drivers.spirv.graal.SPIRVStampFactory;
import uk.ac.manchester.tornado.drivers.spirv.graal.lir.SPIRVKind;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.GetArrayNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.LoadIndexedVectorNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.SPIRVVectorValueNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorAddNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorDivNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorLoadElementNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorMultNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorStoreElementProxyNode;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorStoreGlobalMemory;
import uk.ac.manchester.tornado.drivers.spirv.graal.nodes.vector.VectorSubNode;
import uk.ac.manchester.tornado.runtime.common.Tornado;

public class SPIRVVectorPlugins {

    public static void registerPlugins(final Plugins ps, final InvocationPlugins plugins) {

        if (Tornado.ENABLE_VECTORS) {
            ps.appendNodePlugin(new NodePlugin() {
                @Override
                public boolean handleInvoke(GraphBuilderContext b, ResolvedJavaMethod method, ValueNode[] args) {
                    SPIRVKind vectorKind = SPIRVKind.fromResolvedJavaTypeToVectorKind(method.getDeclaringClass());
                    if (vectorKind == SPIRVKind.ILLEGAL) {
                        return false;
                    }
                    if (method.getName().equals("<init>")) {
                        final SPIRVVectorValueNode vectorValueNode = resolveReceiver(args[0]);
                        if (args.length > 1) {
                            int offset = (vectorValueNode == args[0]) ? 1 : 0;
                            for (int i = offset; i < args.length; i++) {
                                vectorValueNode.setElement(i - offset, args[i]);
                            }
                        } else {
                            if (vectorKind.getVectorLength() < 8) {
                                vectorValueNode.initialiseToDefaultValues(vectorValueNode.graph());
                            }
                        }
                        return true;
                    }
                    return false;
                }
            });

            // Byte
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR3_INT_8, byte[].class, byte.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR4_INT_8, byte[].class, byte.class);

            // Floats
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR2_FLOAT_32, float[].class, float.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR3_FLOAT_32, float[].class, float.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR4_FLOAT_32, float[].class, float.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR8_FLOAT_32, float[].class, float.class);

            // Adding ints
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR2_INT_32, int[].class, int.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR3_INT_32, int[].class, int.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR4_INT_32, int[].class, int.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR8_INT_32, int[].class, int.class);

            // Short
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR2_INT_16, short[].class, short.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR3_INT_16, short[].class, short.class);

            // Doubles
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR2_FLOAT_64, double[].class, double.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR3_FLOAT_64, double[].class, double.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR4_FLOAT_64, double[].class, double.class);
            registerVectorPlugins(plugins, SPIRVKind.OP_TYPE_VECTOR8_FLOAT_64, double[].class, double.class);
        }
    }

    private static void registerVectorPlugins(final InvocationPlugins plugins, final SPIRVKind spirvVectorKind, final Class<?> storageType, final Class<?> elementType) {

        final Class<?> declaringClass = spirvVectorKind.getJavaClass();
        final JavaKind javaElementKind = spirvVectorKind.getElementKind().asJavaKind();

        final Registration r = new Registration(plugins, declaringClass);

        r.register(new InvocationPlugin("get", Receiver.class, int.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode laneId) {
                final VectorLoadElementNode loadElement = new VectorLoadElementNode(spirvVectorKind.getElementKind(), receiver.get(), laneId);
                b.push(javaElementKind, b.append(loadElement));
                return true;
            }
        });

        r.register(new InvocationPlugin("set", Receiver.class, spirvVectorKind.getJavaClass()) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode value) {
                if (receiver.get() instanceof ParameterNode) {
                    final AddressNode address = new OffsetAddressNode(receiver.get(), null);
                    final VectorStoreGlobalMemory store = new VectorStoreGlobalMemory(spirvVectorKind, address, value);
                    b.add(b.append(store));
                    return true;
                }
                return false;
            }
        });

        r.register(new InvocationPlugin("set", Receiver.class, int.class, elementType) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode laneId, ValueNode value) {
                final VectorStoreElementProxyNode store = new VectorStoreElementProxyNode(spirvVectorKind.getElementKind(), receiver.get(), laneId, value);
                b.add(b.append(store));
                return true;
            }
        });

        r.register(new InvocationPlugin("add", declaringClass, declaringClass) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode input1, ValueNode input2) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                VectorAddNode addNode = new VectorAddNode(kind, input1, input2);
                b.push(JavaKind.Illegal, b.append(addNode));
                return true;
            }
        });

        r.register(new InvocationPlugin("sub", declaringClass, declaringClass) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode input1, ValueNode input2) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                VectorSubNode addNode = new VectorSubNode(kind, input1, input2);
                b.push(JavaKind.Illegal, b.append(addNode));
                return true;
            }
        });

        r.register(new InvocationPlugin("mul", declaringClass, declaringClass) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode input1, ValueNode input2) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                VectorMultNode addNode = new VectorMultNode(kind, input1, input2);
                b.push(JavaKind.Illegal, b.append(addNode));
                return true;
            }
        });

        r.register(new InvocationPlugin("div", declaringClass, declaringClass) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode input1, ValueNode input2) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                VectorDivNode addNode = new VectorDivNode(kind, input1, input2);
                b.push(JavaKind.Illegal, b.append(addNode));
                return true;
            }
        });

        r.register(new InvocationPlugin("loadFromArray", storageType, int.class) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode array, ValueNode index) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                JavaKind elementKind = kind.getElementKind().asJavaKind();
                LoadIndexedVectorNode indexedLoad = new LoadIndexedVectorNode(kind, array, index, elementKind);
                b.push(JavaKind.Object, b.append(indexedLoad));
                return true;
            }
        });

        r.register(new InvocationPlugin("storeToArray", Receiver.class, storageType, int.class) {
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode array, ValueNode index) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                ValueNode value = receiver.get();
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                JavaKind elementKind = kind.getElementKind().asJavaKind();
                StoreIndexedNode indexedStore = new StoreIndexedNode(array, index, null, null, elementKind, value);
                b.append(b.append(indexedStore));
                return true;
            }
        });

        r.register(new InvocationPlugin("getArray", Receiver.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver) {
                final ResolvedJavaType resolvedType = b.getMetaAccess().lookupJavaType(declaringClass);
                SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(resolvedType);
                JavaKind elementKind = kind.getElementKind().asJavaKind();
                ValueNode array = receiver.get();
                GetArrayNode getArrayNode = new GetArrayNode(kind, array, elementKind);
                b.push(JavaKind.Object, b.append(getArrayNode));
                return true;
            }
        });

    }

    private static SPIRVVectorValueNode resolveReceiver(ValueNode thisObject) {
        SPIRVVectorValueNode vector = null;
        if (thisObject instanceof PiNode) {
            thisObject = ((PiNode) thisObject).getOriginalNode();
        }
        if (thisObject instanceof SPIRVVectorValueNode) {
            vector = (SPIRVVectorValueNode) thisObject;
        }
        guarantee(vector != null, "[Vector Plugins] unable to resolve vector");
        return vector;
    }

    /**
     * If the parameter passed is a vector, we attach vector information (SPIRVKind)
     * to the parameter node.
     *
     * @param plugins
     *            {@link Plugins}
     */
    public static void registerParameterPlugins(Plugins plugins) {
        plugins.appendParameterPlugin((GraphBuilderTool tool, int index, StampPair stampPair) -> {
            if (stampPair.getTrustedStamp() instanceof ObjectStamp) {
                ObjectStamp objectStamp = (ObjectStamp) stampPair.getTrustedStamp();
                if (objectStamp.type().getAnnotation(Vector.class) != null) {
                    SPIRVKind kind = SPIRVKind.fromResolvedJavaTypeToVectorKind(objectStamp.type());
                    return new ParameterNode(index, StampPair.createSingle(SPIRVStampFactory.getStampFor(kind)));
                }
            }
            return null;
        });
    }
}
