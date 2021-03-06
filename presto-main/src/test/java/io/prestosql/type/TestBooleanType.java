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
package io.prestosql.type;

import io.prestosql.spi.block.Block;
import io.prestosql.spi.block.BlockBuilder;

import static io.prestosql.spi.type.BooleanType.BOOLEAN;

public class TestBooleanType
        extends AbstractTestType
{
    public TestBooleanType()
    {
        super(BOOLEAN, Boolean.class, createTestBlock());
    }

    public static Block createTestBlock()
    {
        BlockBuilder blockBuilder = BOOLEAN.createBlockBuilder(null, 15);
        BOOLEAN.writeBoolean(blockBuilder, true);
        BOOLEAN.writeBoolean(blockBuilder, true);
        BOOLEAN.writeBoolean(blockBuilder, true);
        BOOLEAN.writeBoolean(blockBuilder, false);
        BOOLEAN.writeBoolean(blockBuilder, false);
        BOOLEAN.writeBoolean(blockBuilder, false);
        BOOLEAN.writeBoolean(blockBuilder, false);
        BOOLEAN.writeBoolean(blockBuilder, false);
        BOOLEAN.writeBoolean(blockBuilder, true);
        BOOLEAN.writeBoolean(blockBuilder, true);
        BOOLEAN.writeBoolean(blockBuilder, false);
        return blockBuilder.build();
    }

    @Override
    protected Object getGreaterValue(Object value)
    {
        return true;
    }
}
