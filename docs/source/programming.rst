.. _programming:

Core Programming
===================

TornadoVM exposes to the programmer task-level, data-level and pipeline-level parallelism via a light Application Programming Interface (API). 
In addition, TornadoVM uses single-source property, in which the code to be accelerated and the host code live in the same Java program.


Programming in TornadoVM involves the development of three parts:

1. **Expressing parallelism within Java methods:** TornadoVM offers two APIs: one for loop parallelization using Java annotations; and a second one for low-level programming using a Kernel API. 
   Developers can choose which one to use. The loop API is recommended for non-expert GPU/FPGA programmers. 
   The kernel API is recommended for experts GPU programmers than want more control (access to GPU's local memory, barriers, etc.).
2. **Selecting the methods to be accelerated using a Task-Graph API:** once Java methods have been identified for acceleration (either using the loop parallel API or kernel API), Java methods can be grouped together in a graph. 
   TornadoVM offers an API to define the data as well as the Java methods to be accelerated. 
3. Building an **Execution Plan**: From the task-graphs, developers can accelerate all methods that are indicate in that graph on an accelerator. Additionally, through an execution plan in TornadoVM, developers can change the way TornadoVM offloads and runs the code (e.g., by selecting a specific GPU, enabling the profiler, etc.).


1. Expressing Parallelism within Java Methods
------------------------------------------------


TornadoVM offloads Java methods to heterogeneous hardware such as GPUs and FPGAs for parallel execution. 
Those Java methods usually represents the sequential (single thread) implementation of the work to perform on the accelerator. 
However, TornadoVM does not auto-parallelize Java methods.

Thus, TornadoVM needs a hint about how to parallelize the code. 
TornadoVM has two APIs to achieve this goal: one for loop parallelization using Java annotations; and a second one for low-level programming using a Kernel API. 
Developers can choose which one to use. The loop API is recommended for non-expert GPU/FPGA programmers. 


Loop Parallel API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Compute kernels are written in a sequential form (tasks programmed for a single thread execution). 
To express parallelism, TornadoVM exposes two annotations that can be used in loops and parameters: 
a) ``@Parallel`` for annotating parallel loops; and b) ``@Reduce`` for annotating parameters used in reductions.

The following code snippet shows a full example to accelerate Matrix-Multiplication using TornadoVM and the loop-parallel API:
The two outermost loops can be parallelizable because there are no data dependencies across different iterations.
Therefore, we can annotate these two loops. 
Note that, since TornadoVM maps parallel loops to Parllel ND-Range for OpenCL, CUDA and SPIR-V, developers can benefit 
from 1D (annotating one parallel loop), 2D (annotating two consecutive parallel loops) and 3D (annotating 3 consecutive parallel loops) in their Java methods. 


.. include:: codesamples/Compute.java 
   :code: java 


The code snipet shows a complete example, using the Loop Parallel annotations, the Task Graphs and the execution plan. 
This document explains each part. 


.. _kernel-context-api:

Kernel API
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Another way to express compute-kernels in TornadoVM is via the kernel API. 
To do so, TornadoVM exposes a ``KernelContext`` with which the application can directly access 
the thread-id, allocate memory in local memory (shared memory on NVIDIA devices), and insert barriers. 
This model is similar to programming compute-kernels in OpenCL and CUDA. 
Therefore, this API is more suitable for GPU/FPGA expert programmers that want more control or want to port existing CUDA/OpenCL compute kernels into TornadoVM.

The following code-snippet shows the Matrix Multiplication example using the kernel-parallel API:


.. include:: codesamples/ComputeKernel.java 
   :code: java 


Kernel Context
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

``KernelContext`` is a Java object exposed by the TornadoVM API to the developers in order to leverage Kernel Parallel Programming using the
existing ``TaskGraph`` API. 
An instance of the ``KernelContext`` object is passed to each task that uses the kernel-parallel API.

Additionally, for all tasks using the ``KernelContext`` object, the user must provide a Grid of execution threads to run on the parallel device.
This grid of threads is similar to the number of threads to be launched using CUDA or OpenCL (Number of threads per block and number of blocks).
Examples can be found in the ``Grid``
`unit-tests <https://github.com/beehive-lab/TornadoVM/tree/master/tornado-unittests/src/main/java/uk/ac/manchester/tornado/unittests/grid>`__.

KernelContext Features
^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The following table presents the available features that TornadoVM exposes in Java along with the respective OpenCL and CUDA PTX terminology.

.. code:: java

   // Note: 
   kc = new KernelContext();

+-------------------+----------------+--------------------------------+
| TornadoVM         | OpenCL         | PTX                            |
| KernelContext     |                |                                |
+===================+================+================================+
| kc.globalIdx      | ge             | blockIdx \* blockDim.x +       |
|                   | t_global_id(0) | threadIdx                      |
+-------------------+----------------+--------------------------------+
| kc.globalIdy      | ge             | blockIdy \* blockDim.y +       |
|                   | t_global_id(1) | threadIdy                      |
+-------------------+----------------+--------------------------------+
| kc.globalIdz      | ge             | blockIdz \* blockDim.z +       |
|                   | t_global_id(2) | threadIdz                      |
+-------------------+----------------+--------------------------------+
| kc.ge             | ge             | blockDim                       |
| tLocalGroupSize() | t_local_size() |                                |
+-------------------+----------------+--------------------------------+
| kc.localBarrier() | barrier(CLK_LO | barrier.sync                   |
|                   | CAL_MEM_FENCE) |                                |
+-------------------+----------------+--------------------------------+
| k                 | b              | barrier.sync                   |
| c.globalBarrier() | arrier(CLK_GLO |                                |
|                   | BAL_MEM_FENCE) |                                |
+-------------------+----------------+--------------------------------+
| int[] array =     | \__local int   | .shared .s32 array[size]       |
| kc.allocateIn     | array[size]    |                                |
| tLocalArray(size) |                |                                |
+-------------------+----------------+--------------------------------+
| float[] array =   | \__local float | .shared .s32 array[size]       |
| kc.allocateFloa   | array[size]    |                                |
| tLocalArray(size) |                |                                |
+-------------------+----------------+--------------------------------+
| long[] array =    | \__local long  | .shared .s64 array[size]       |
| kc.allocateLon    | array[size]    |                                |
| gLocalArray(size) |                |                                |
+-------------------+----------------+--------------------------------+
| double[] array =  | \__local       | .shared .s64 array[size]       |
| kc.allocateDoubl  | double         |                                |
| eLocalArray(size) | array[size]    |                                |
+-------------------+----------------+--------------------------------+

Example
~~~~~~~~~~~~~~~~~~~~~~~~~~

The following
`example <https://github.com/beehive-lab/TornadoVM/tree/master/tornado-examples/src/main/java/uk/ac/manchester/tornado/examples/kernelcontext/compute/MatrixMultiplication2DV2.java>`__
is the Matrix Multiplication implementation using the ``KernelContext`` object for indexing threads and access to local memory. The following
example also makes use of loop tiling. There are three main steps to leverage the features of the ``KernelContext``:

1. The ``KernelContext`` object is passed as an argument in the method that will be accelerated. This implementation follows the OpenCL
   implementation description provided in https://github.com/cnugteren/myGEMM.

.. code:: java

   public static void matrixMultiplication(KernelContext context, 
                                          final float[] A, final float[] B, 
                                          final float[] C, final int size) {

       // Index thread in the first dimension ( get_global_id(0) )
       int row = context.localIdx;

       // Index thread in the seconbd dimension ( get_global_id(1) )
       int col = context.localIdy;

       int globalRow = TS * context.groupIdx + row;
       int globalCol = TS * context.groupIdy + col;

       // Create Local Memory via the context
       float[] aSub = context.allocateFloatLocalArray(TS * TS);
       float[] bSub = context.allocateFloatLocalArray(TS * TS);

       float sum = 0;

       // Loop over all tiles
       int numTiles = size/TS;
       for(int t = 0; t < numTiles; t++){

           // Load one tile of A and B into local memory
           int tiledRow = TS * t + row;
           int tiledCol = TS * t + col;
           aSub[col * TS + row] = A[tiledCol * size + globalRow];
           bSub[col * TS + row] = B[globalCol * size + tiledRow];

           // Synchronise to make sure the tile is loaded
           context.localBarrier();

           // Perform the computation for a single tile
           for(int k = 0; k < TS; k++) {
               sum += aSub[k* TS + row] * bSub[col * TS + k];
           }
           // Synchronise before loading the next tile
           context.globalBarrier();
       }

       // Store the final result in C
       C[(globalCol * size) + globalRow] = sum;
   }

2. A TornadoVM program that uses the ``KernelContext`` must use the context with a ``WorkerGrid`` (1D/2D/3D). This is necessary in order
   to obtain awareness about the dimensions and the sizes of the threads that will be deployed. 
   Therefore, ``KernelContext`` can only work with tasks that are linked with a ``GridScheduler``.

.. code:: java

   // Create 2D Grid of Threads with a 2D Worker
   WorkerGrid workerGrid = new WorkerGrid2D(size, size);

   // Create a GridScheduler that associates a task-ID with a worker grid
   GridScheduler gridScheduler = new GridScheduler("s0.t0", workerGrid);

   // Create the TornadoVM Context
   KernelContext context = new KernelContext();

   // [Optional] Set the local work size 
   workerGrid.setLocalWork(32, 32, 1);

3. Create a ``TornadoExecutionPlan`` and execute:

.. code:: java

   TaskGraph tg = new TaskGraph("s0")
         .transferToDevice(DataTransferMode.EVERY_EXECUTION, matrixA, matrixB)
         .task("t0",MxM::compute,context, matrixA, matrixB, matrixC, size)
         .transferToHost(DataTransferMode.EVERY_EXECUTION, matrixC);

   ImmutableTaskGraph immutableTaskGraph = tg.snapshot();
   TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph);

   executionPlan.withGridScheduler(gridScheduler)  // Pass the GridScheduler in the execute method
                .execute();



Running Multiple Tasks with the Kernel Context
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The TornadoVM Task-Graph can be composed of multiple tasks which can either exploit the ``KernelContext`` features or adhere to the original
TornadoVM annotations (``@Parallel``, ``@Reduce``). 

The following code snippet shows an example of how to combine tasks that require a ``KernelContext`` object with tasks that do not need a ``KernelContext``. 

.. code:: java

   WorkerGrid worker = new WorkerGrid1D(size);

   GridScheduler gridScheduler = new GridScheduler();
   gridScheduler.setWorkerGrid("s02.t0", worker);
   gridScheduler.setWorkerGrid("s02.t1", worker);
   
   // Create a Kernel Context object
   KernelContext context = new KernelContext();

   TaskGraph taskGraph = new TaskGraph("s02") //
      .transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b) //
      .task("t0", TestCombinedTaskGraph::vectorAddV2, context, a, b, cTornado) //
      .task("t1", TestCombinedTaskGraph::vectorMulV2, context, cTornado, b, cTornado) //
      .task("t2", TestCombinedTaskGraph::vectorSubV2, context, cTornado, b) //
      .transferToHost(DataTransferMode.EVERY_EXECUTION, cTornado);

   ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
   TornadoExecutionPlan executor = new TornadoExecutionPlan(immutableTaskGraph);

   executor.withGridScheduler(gridScheduler) //
           .execute();


In this test case, each of the first two tasks uses a separate ``WorkerGrid``. 
The third task does not use a ``WorkerGrid``, and it relies on the TornadoVM Runtime for the scheduling of the threads. 

You can see more examples on `GitHub <https://github.com/beehive-lab/TornadoVM/tree/master/tornado-examples/src/main/java/uk/ac/manchester/tornado/examples/kernelcontext>`_.



2. Selecting the methods to be accelerated using a Task-Graph API
-----------------------------------------------------------------

A ``TaskGraph`` is an TornadoVM object that defines and identify which Java methods to be accelerated and the data involved. 
Task-graph defines data to be copied in, and out of the accelerator as well as all tasks (Java methods) to be accelerated. 
Note that a ``TaskGraph`` object does not compute/move data, but rather annotates what to do when the computation in launched. 
As we will see in Step 3, a task-graph is only executed through an execution plan. 


The following code snippet shows how to instantiate a ``TaskGraph`` TornadoVM object. 

.. code:: java 

   TaskGraph taskGraph = new TaskGraph(""name");



A. Defining copies from the host (main CPU) to the device (accelerator).
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
 

The Task-Graph API also defines a method, named ``transferToDevice`` to set which arrays need to be copied to the target accelerator. 
This method receives two types of arguments: 

1. Data Transfer Mode:
   a. ``EVERY_EXECUTION``: Data is copied from host to device every time a task-graph is executed by an execution plan. 
   b. ``FIRST_EXECUTION``: Data is only copied the first time a task-graph is executed by an execution plan. 
2. All input arrays needed to be copied from the host to the device. 


The following code snippet sets two arrays (a, b) to be copied from the host to the device every time a task-graph is executed. 


.. code:: java

   taskGraph.transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b);


Note that this call is only used for the definition of the data flow across multiple tasks in a task-graph, and there are no data copies involved. 
The TornadoVM runtime stores which data are associated with each data transfer mode and the actual data transfers take place only during the execution by the execution plan. 


B. Code definition
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

To identify which Java methods, from all existing Java methods in a Java program, to accelerate.
This is performed using the ``task`` API call as follows:


.. code:: java

   taskGraph.task(“sample”, Class::method, param1, param2);


- The first paramter sets an ID to the task. This is useful if developers want to change device, or other runtime parameters, from the command line. 
- The second parameter is a reference (or a Java lambda expression), to an existing Java method.
- The rest of the parameters correspond to the function call parameters, as if the method were invoked. 


Developers can add as many tasks as needed. 
The maximum number of tasks depends on the amount of code that can be shipped to the accelerator. 
Usually, FPGAs are more limited than GPUs. 


C. Copy out from the device (accelerator) to the host (main CPU). 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

Similar to ``transferToDevice``, the ``TaskGraph`` API also offers a call to sync the data back from the device to the host. 
The API call is ``transferToHost`` with the following parameters:

1. Data Transfer Mode:
   a. ``EVERY_EXECUTION``: Data is copied from the device to the host every time a task-graph is executed by an execution plan. 
   b. ``USER_DEFINED:`` Data is only copied by an execution result under demand. This is an optimization if developers plan to execute the task-graph multiple times and do not want to copy the results every time the execution plan is launched.
2. All output arrays to be copied from the device to the host.


Example:

.. code:: java

   taskGraph.transferToHost(DataTransferMode.EVERY_EXECUTION, output1, output2);


3. Execution Plans 
------------------------------------------------


The last step is the creation of an execution plan. An execution plan receives a list of immutable task graphs ready to be executed, as follows:


.. code:: java 

   TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(itg); 


What can we do with an execution plan?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


We can execute an execution plan directly, and TornadoVM will apply a list of default optimisations (e.g., it will run on the default device, using the default thread scheduler).


.. code:: java 

   executionPlan.execute(); 



How can we optimize an execution plan?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


The execution plan offers a set of methods that developers can use to optimize different execution plans. 
Note that the execution plan operates over all immutable task graphs given in the constructor. Therefore, all immutable task graphs will be executed on the same device in order.

Example:

.. code:: java 

   executionPlan.withProfiler(ProfilerMode.SILENT) // Enable Profiling
       .withWarmUp() //  Perform a warmup (compile and code and install it in a code-cache).
       .withDevice(device); Select a specific device


And then:

.. code:: java 

   executionPlan.execute();



4. Obtain the result and the profiler
--------------------------------------------

Every time an execution plan is executed, a new object of type ``TornadoExecutionResult`` is created. 

.. code:: java 

   TornadoExecutionResult executionResult = executionPlan.execute(); 



From the execution result, developers can obtain the result of the TornadoVM profiler:


.. code:: java 

   executionResult.getProfilerResult();  


And query the values of the profiling report. 
Note that the TornadoVM profiler works only if enabled in the execution plan (via the ``withProfiler`` method). 



.. _reductions:
Parallel Reductions
------------------------------------

TornadoVM now supports basic reductions for ``int``, ``long``,\ ``float`` and ``double`` data types for the operators ``+`` and ``*``, ``max`` and ``min``. 
Examples can be found in the ``examples/src/main/java/uk/ac/manchester/tornado/unittests/reductions`` directory on GitHub.

TornadoVM exposes the Java annotation ``@Reduce`` to represent parallel reductions. 
Simiarly to the ``@Parallel`` annotation, the ``@Reduce`` annotation is used to identify parallel sections in Java sequential code. 
The annotations is used for method parameter in which reductions must be applied. 
This is similar to OpenMP and OpenACC. 

Example:

.. code:: java

   public static void reductionAddFloats(float[] input, @Reduce float[] result) {
       for (@Parallel int i = 0; i < input.length; i++) {
           result[0] += input[i];
       }
    }

The code is very similar to a Java sequential reduction but with ``@Reduce`` and ``@Parallel`` annotations. 
The ``@Reduce`` annotation is associated with a variable, in this case, with the ``result`` float
array. 
Then, we annotate the loop with ``@Parallel``. The OpenCL/PTX JIT compilers generate OpenCL/PTX parallel version for this code that can
run on GPU and CPU.

Creating reduction tasks 
~~~~~~~~~~~~~~~~~~~~~~~~~

TornadoVM generates different OpenCL/SPIR-V code depending on the target device. 
Internally, if the target is a GPU, TornadoVM performs full and
parallel reductions using the threads within the same OpenCL work-group.
If the target is a CPU, TornadoVM performs full reductions within the
same thread-id. Besides, TornadoVM automatically resizes the output
variables according to the number of work-groups and threads selected.

For PTX code generation, TornadoVM will always perform full and parallel
reductions using the threads within the same CUDA block.

.. code:: java

   float[] input = new float[SIZE];
   float[] result = new float[1];

   Random r = new Random();
   IntStream.range(0, SIZE).sequential().forEach(i -> {
       input[i] = r.nextFloat();
   });

   TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, input) //
                .task("t0", TestReductionsFloats::reductionAddFloats, input, result) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, result);

   ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
   TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph);
   executionPlan.execute();


Map/Reduce
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This section shows an example of how to perform map/reduce operations with TornadoVM. 
Each of the operations corresponds to a task as follows
in the next example:

.. code:: java


   public class ReduceExample {
       public static void map01(int[] a, int[] b, int[] c) {
           for (@Parallel int i = 0; i < a.length; i++) {
               c[i] = a[i] + b[i];
           }
       }

       public static void reduce01(int[] c, @Reduce int[] result) {
           result[0] = 0;
           for (@Parallel int i = 0; i < c.length; i++) {
               result[0] += c[i];
           }
       }

       public void testMapReduce() {
            int[] a = new int[BIG_SIZE];
            int[] b = new int[BIG_SIZE];
            int[] c = new int[BIG_SIZE];
            int[] result = new int[] { 0 };

            IntStream.range(0, BIG_SIZE).parallel().forEach(i -> {
               a[i] = 10;
               b[i] = 2;
            });

            TaskGraph taskGraph = new TaskGraph("s0") //
                .transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b, c) //
                .task("t0", TestReductionsIntegers::map01, a, b, c) //
                .task("t1", TestReductionsIntegers::reduce01, c, result) //
                .transferToHost(DataTransferMode.EVERY_EXECUTION, result);
            ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
            TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(immutableTaskGraph);
            TornadoExecutionResult executionResult = executionPlan.execute();
        }
   }

Reduction with dependencies
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

TornadoVM also supports reductions using data dependencies. 
The next example illustrates this case with the ``PI`` computation.

.. code:: java

   public static void computePi(float[] input, @Reduce float[] result) {
       for (@Parallel int i = 1; i < input.length; i++) {
           float value = (float) (Math.pow(-1, i + 1) / (2 * i - 1));
           result[0] += value + input[i];
       }
   }


.. _dynamic_reconfiguration:

Dynamic Reconfiguration
------------------------------


The dynamic configuration in TornadoVM is the capability to migrate tasks at runtime from one device to another (e.g., from one GPU to another, or from one CPU to GPU, etc).
The dynamic reconfiguration is not enabled by default, but it can be easily activated through the execition plan as follows:


.. code:: java

   executionPlan.withDynamicReconfiguration(Policy.PERFORMANCE, DRMode.Parallel)
                .execute();



The `withDynamicReconfiguration` call receives two arguments:

1. Policy: dynamically changes devices based on one of the following policies: 

   -  `PERFORMANCE`: after a warmup of all devices (JIT compilation is excluded). The TornadoVM runtime evaluates the execution for all devices before making a decision.
  
   - `END_2_END`: best performing device including the warm-up phase (JIT compilation and buffer allocations). The TornadoVM runtime evaluates the execution for all devices before making a decision.

   - `LATENCY`: fastest device to return. The TornadoVM runtime does not evaluate the execution for all devices before making a decision, but rather it switches context with the first device that finishes the execution.
  

.. _batch-processing:

Batch Computing Processing
--------------------------

TornadoVM supports batch processing through the following API call ``withBatch`` of the ``TornadoExecutionPlan`` API. 
This was mainly designed to run big data applications in which host data is much larger than the device memory capacity. 
In these cases, developers can enable batch processing by simply calling ``withBatch`` in their execution plans. 

.. code:: java

   int size = 2110000000;
   float[] arrayA = new float[size]; // Array of 8440MB
   float[] arrayB = new float[size]; // Array of 8440MB

   TaskGraph taskGraph = new TaskGraph("s0") //
         .transferToDevice(DataTransferMode.FIRST_EXECUTION, arrayA) //
         .task("t0", TestBatches::compute, arrayA, arrayB) //
         .transferToHost(DataTransferMode.EVERY_EXECUTION, arrayB);

   ImmutableTaskGraph immutableTaskGraph = taskGraph.snapshot();
   TornadoExecutionPlan executor = new TornadoExecutionPlan(immutableTaskGraph);
   executor.withBatch("512MB") // Run in blocks of 512MB
           .execute();


The batch method-call receives a Java string representing the batch to be allocated, copied-in and copied-out to/from the heap of the target device.

Examples of allowed sizes are:

.. code:: java

   batch("XMB");   // Express in MB (X is an int number)
   batch("ZGB");   // Express in GB (Z is an int number)

Current Limitations of Batch Processing
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

There is a set of limitations with the current implementation of batch processing.

1. All arrays passed to the input methods to be compiled to the target device have to have the same data type and size.
2. We only support arrays of primitives that are passed as arguments. This means that scope arrays in batches are not currently supported.
3. All bytecodes make use of the same OpenCL command queue / CUDA stream.
4. Matrix or non-regular batch distributions. (E.g., MxM would need to be split by rows in matrix-A and columns in matrix-B).



Migration to TornadoVM v0.15
----------------------------------


TornadoVM 0.15 introduced new APIs for expressing, optimising, and running Java methods (tasks) on heterogeneous hardware. 
This document details the main changes of existing Java applications for TornadoVM using the APIs previous to the 0.15 release. 

Porting applications that have been implemented with previous TornadoVM releases (prior to ``v0.15``) to the new TornadoVM APIs is not a complicated process, 
but it requires a few modifications in how the task graphs (previous task schedules) are built and executed. 


Step 1. ``TaskSchedule`` renamed to ``TaskGraph``
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Task-Graphs are a new construct exposed by the TornadoVM API to developers. 
A Task-Graph is an object equivalent to the previous Task-Schedule (< TornadoVM 0.15). 
Thus, existing ``TaskSchedule`` objects can be renamed to the ``TaskGraph`` with the following changes regarding how data is copied in and out from the host to the device, and vice-versa. 


.. code:: java

   TaskGraph taskGraph = new TaskGraph(“name”);


A TaskGraph provides the definition of data and code to be executed by an execution plan (more details in step 3). 
Therefore, in the task-graph, we must define which data to copy in, and out and the tasks (Java methods) to be accelerated. 


A. Defining copies from the host (main CPU) to the device (accelerator).
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
 

The Task-Graph API defines a method, named ``transferToDevice`` to set which arrays need to be copied to the target accelerator. This method receives two types of arguments: 

1. Data Transfer Mode:
   a. ``EVERY_EXECUTION``: Data is copied from host to device every time a task-graph is executed by an execution plan. This corresponds to the ``streamIn`` method in the TornadoVM API < ``v0.15``.
   b. ``FIRST_EXECUTION``: Data is only copied the first time a task-graph is executed by an execution plan. 
2. All input arrays needed to be copied from the host to the device. 


The following code snippet sets two arrays (a, b) to be copied from the host to the device every time a task-graph is executed. 



.. code:: java

   taskGraph.transferToDevice(DataTransferMode.EVERY_EXECUTION, a, b);


Note that this call is only used for the definition of the data flow across multiple tasks in a task-graph, and there are no data copies involved. 
The TornadoVM runtime stores which data are associated with each data transfer mode and the actual data transfers take place only during the execution by the execution plan. 


B. Code definition
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


The code definition has not changed from the previous APIs of TornadoVM. 


.. code:: java

   taskGraph.task(“sample”, Class::method, param1, param2);


Developers can add as many tasks as needed. 
The maximum number of tasks depends on the amount of code that can be shipped to the accelerator. 
Usually, FPGAs are more limited than GPUs. 


C. Copy out from the device (accelerator) to the host (main CPU). 
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


The ``streamOut`` method from the TornadoVM APIs previous to 0.15 has been renamed to ``transferToHost``. 
Similar to ``transferToDevice``, this new call has the following parameters:


1. Data Transfer Mode:
   a. ``EVERY_EXECUTION``: Data is copied from the device to the host every time a task-graph is executed by an execution plan. 
   b. ``USER_DEFINED:`` Data is only copied by an execution result under demand. This is an optimization if developers plan to execute the task-graph multiple times and do not want to copy the results every time the execution plan is launched.
2. All output arrays to be copied from the device to the host.


Example:

.. code:: java

   taskGraph.transferToHost(DataTransferMode.EVERY_EXECUTION, output1, output2);


Step 2: Create an Immutable Task Graph
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Once the task-graph is defined, we need to create a snapshot to obtain an object of type ``ImmutableTaskGraph``.
This is a very simple process:


.. code:: java 

   ImmutableTaskGraph itg = taskGraph.snapshot();

An immutable task graph cannot be modified. 
Thus, if developers need to update a task graph, they need to invoke the ``snapshot`` method again and create a new immutable task graph object. 


Step 3: Build, Optimise and Execute an Execution Plan
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


The last step is the creation of an execution plan. An execution plan receives a list of immutable task graphs ready to be executed, as follows:


.. code:: java 

   TornadoExecutionPlan executionPlan = new TornadoExecutionPlan(itg); 


What can we do with an execution plan?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


We can execute an execution plan directly, and TornadoVM will apply a list of default optimisations (e.g., it will run on the default device, using the default thread scheduler).


.. code:: java 

   executionPlan.execute(); 


How can we optimize an execution plan?
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^


The execution plan offers a set of methods that developers can use to optimize different execution plans. 
Note that the execution plan operates over all immutable task graphs given in the constructor. Therefore, all immutable task graphs will be executed on the same device in order.

Example:

.. code:: java 

   executionPlan.withProfiler(ProfilerMode.SILENT) // Enable Profiling
       .withWarmUp() //  Perform a warmup (compile and code and install it in a code-cache).
       .withDevice(device); Select a specific device


And then:

.. code:: java 

   executionPlan.execute();


Step 4. Obtain the result and the profiler
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Every time an execution plan is executed, a new object of type ``TornadoExecutionResult`` is created. 

.. code:: java 

   TornadoExecutionResult executionResult = executionPlan.execute(); 



From the execution result, developers can obtain the result of the TornadoVM profiler:


.. code:: java 

   executionResult.getProfilerResult();  


And query the values of the profiling report. 
Note that the TornadoVM profiler works only if enabled in the execution plan (via the ``withProfiler`` method). 



Further reading and examples
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The TornadoVM modules for the unit tests:


https://github.com/beehive-lab/TornadoVM/tree/master/tornado-unittests/src/main/java/uk/ac/manchester/tornado/unittests


and the examples 



https://github.com/beehive-lab/TornadoVM/tree/master/tornado-examples/src/main/java/uk/ac/manchester/tornado/examples


contain a list of diverse applications that can be used to learn the new TornadoVM API. 



