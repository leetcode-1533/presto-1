/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.prestosql.operator.scalar;

import com.google.common.collect.ImmutableList;
import io.prestosql.metadata.FunctionBinding;
import io.prestosql.metadata.FunctionDependencies;
import io.prestosql.metadata.FunctionDependencyDeclaration;
import io.prestosql.spi.block.Block;
import io.prestosql.spi.type.RowType;
import io.prestosql.spi.type.Type;

import java.lang.invoke.MethodHandle;
import java.util.List;

import static io.prestosql.spi.function.InvocationConvention.InvocationArgumentConvention.NEVER_NULL;
import static io.prestosql.spi.function.InvocationConvention.InvocationReturnConvention.FAIL_ON_NULL;
import static io.prestosql.spi.function.OperatorType.GREATER_THAN;
import static io.prestosql.spi.function.OperatorType.GREATER_THAN_OR_EQUAL;
import static io.prestosql.util.Reflection.methodHandle;

public final class RowGreaterThanOrEqualOperator
        extends RowComparisonOperator
{
    public static final RowGreaterThanOrEqualOperator ROW_GREATER_THAN_OR_EQUAL = new RowGreaterThanOrEqualOperator();
    private static final MethodHandle METHOD_HANDLE = methodHandle(RowGreaterThanOrEqualOperator.class, "greaterOrEqual", RowType.class, List.class, Block.class, Block.class);

    private RowGreaterThanOrEqualOperator()
    {
        super(GREATER_THAN_OR_EQUAL);
    }

    @Override
    public FunctionDependencyDeclaration getFunctionDependencies(FunctionBinding functionBinding)
    {
        return getFunctionDependencies((RowType) functionBinding.getTypeVariable("T"), GREATER_THAN);
    }

    @Override
    public ScalarFunctionImplementation specialize(FunctionBinding functionBinding, FunctionDependencies functionDependencies)
    {
        Type type = functionBinding.getTypeVariable("T");
        return new ScalarFunctionImplementation(
                FAIL_ON_NULL,
                ImmutableList.of(NEVER_NULL, NEVER_NULL),
                METHOD_HANDLE.bindTo(type).bindTo(getMethodHandles((RowType) type, functionDependencies, GREATER_THAN)));
    }

    public static boolean greaterOrEqual(
            RowType rowType,
            List<MethodHandle> lessThanFunctions,
            Block leftRow, Block rightRow)
    {
        int compareResult = compare(rowType, lessThanFunctions, leftRow, rightRow);
        return compareResult >= 0;
    }
}
