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
package io.prestosql.orc.metadata;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.prestosql.spi.PrestoException;
import io.prestosql.spi.type.ArrayType;
import io.prestosql.spi.type.CharType;
import io.prestosql.spi.type.DecimalType;
import io.prestosql.spi.type.MapType;
import io.prestosql.spi.type.RowType;
import io.prestosql.spi.type.Type;
import io.prestosql.spi.type.TypeSignatureParameter;
import io.prestosql.spi.type.VarcharType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static io.prestosql.spi.StandardErrorCode.NOT_SUPPORTED;
import static io.prestosql.spi.type.BigintType.BIGINT;
import static io.prestosql.spi.type.BooleanType.BOOLEAN;
import static io.prestosql.spi.type.DateType.DATE;
import static io.prestosql.spi.type.DoubleType.DOUBLE;
import static io.prestosql.spi.type.IntegerType.INTEGER;
import static io.prestosql.spi.type.RealType.REAL;
import static io.prestosql.spi.type.SmallintType.SMALLINT;
import static io.prestosql.spi.type.TimestampType.TIMESTAMP;
import static io.prestosql.spi.type.TinyintType.TINYINT;
import static io.prestosql.spi.type.VarbinaryType.VARBINARY;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

public class OrcType
{
    public enum OrcTypeKind
    {
        BOOLEAN,

        BYTE,
        SHORT,
        INT,
        LONG,
        DECIMAL,

        FLOAT,
        DOUBLE,

        STRING,
        VARCHAR,
        CHAR,

        BINARY,

        DATE,
        TIMESTAMP,

        LIST,
        MAP,
        STRUCT,
        UNION,
    }

    private final OrcTypeKind orcTypeKind;
    private final List<OrcColumnId> fieldTypeIndexes;
    private final List<String> fieldNames;
    private final Optional<Integer> length;
    private final Optional<Integer> precision;
    private final Optional<Integer> scale;
    private final Map<String, String> attributes;

    private OrcType(OrcTypeKind orcTypeKind)
    {
        this(orcTypeKind, ImmutableList.of(), ImmutableList.of(), Optional.empty(), Optional.empty(), Optional.empty(), ImmutableMap.of());
    }

    private OrcType(OrcTypeKind orcTypeKind, int length)
    {
        this(orcTypeKind, ImmutableList.of(), ImmutableList.of(), Optional.of(length), Optional.empty(), Optional.empty(), ImmutableMap.of());
    }

    private OrcType(OrcTypeKind orcTypeKind, int precision, int scale)
    {
        this(orcTypeKind, ImmutableList.of(), ImmutableList.of(), Optional.empty(), Optional.of(precision), Optional.of(scale), ImmutableMap.of());
    }

    private OrcType(OrcTypeKind orcTypeKind, List<OrcColumnId> fieldTypeIndexes, List<String> fieldNames)
    {
        this(orcTypeKind, fieldTypeIndexes, fieldNames, Optional.empty(), Optional.empty(), Optional.empty(), ImmutableMap.of());
    }

    public OrcType(OrcTypeKind orcTypeKind, List<OrcColumnId> fieldTypeIndexes, List<String> fieldNames, Optional<Integer> length, Optional<Integer> precision, Optional<Integer> scale, Map<String, String> attributes)
    {
        this.orcTypeKind = requireNonNull(orcTypeKind, "typeKind is null");
        this.fieldTypeIndexes = ImmutableList.copyOf(requireNonNull(fieldTypeIndexes, "fieldTypeIndexes is null"));
        if (fieldNames == null || (fieldNames.isEmpty() && !fieldTypeIndexes.isEmpty())) {
            this.fieldNames = null;
        }
        else {
            this.fieldNames = ImmutableList.copyOf(requireNonNull(fieldNames, "fieldNames is null"));
            checkArgument(fieldNames.size() == fieldTypeIndexes.size(), "fieldNames and fieldTypeIndexes have different sizes");
        }
        this.length = requireNonNull(length, "length is null");
        this.precision = requireNonNull(precision, "precision is null");
        this.scale = requireNonNull(scale, "scale cannot be null");
        this.attributes = ImmutableMap.copyOf(requireNonNull(attributes, "attributes is null"));
    }

    public OrcTypeKind getOrcTypeKind()
    {
        return orcTypeKind;
    }

    public int getFieldCount()
    {
        return fieldTypeIndexes.size();
    }

    public OrcColumnId getFieldTypeIndex(int field)
    {
        return fieldTypeIndexes.get(field);
    }

    public List<OrcColumnId> getFieldTypeIndexes()
    {
        return fieldTypeIndexes;
    }

    public String getFieldName(int field)
    {
        return fieldNames.get(field);
    }

    public List<String> getFieldNames()
    {
        return fieldNames;
    }

    public Optional<Integer> getLength()
    {
        return length;
    }

    public Optional<Integer> getPrecision()
    {
        return precision;
    }

    public Optional<Integer> getScale()
    {
        return scale;
    }

    public Map<String, String> getAttributes()
    {
        return attributes;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("orcTypeKind", orcTypeKind)
                .add("fieldTypeIndexes", fieldTypeIndexes)
                .add("fieldNames", fieldNames)
                .toString();
    }

    private static List<OrcType> toOrcType(int nextFieldTypeIndex, Type type)
    {
        if (BOOLEAN.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.BOOLEAN));
        }
        if (TINYINT.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.BYTE));
        }
        if (SMALLINT.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.SHORT));
        }
        if (INTEGER.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.INT));
        }
        if (BIGINT.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.LONG));
        }
        if (DOUBLE.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.DOUBLE));
        }
        if (REAL.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.FLOAT));
        }
        if (type instanceof VarcharType) {
            VarcharType varcharType = (VarcharType) type;
            if (varcharType.isUnbounded()) {
                return ImmutableList.of(new OrcType(OrcTypeKind.STRING));
            }
            return ImmutableList.of(new OrcType(OrcTypeKind.VARCHAR, varcharType.getBoundedLength()));
        }
        if (type instanceof CharType) {
            return ImmutableList.of(new OrcType(OrcTypeKind.CHAR, ((CharType) type).getLength()));
        }
        if (VARBINARY.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.BINARY));
        }
        if (DATE.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.DATE));
        }
        if (TIMESTAMP.equals(type)) {
            return ImmutableList.of(new OrcType(OrcTypeKind.TIMESTAMP));
        }
        if (type instanceof DecimalType) {
            DecimalType decimalType = (DecimalType) type;
            return ImmutableList.of(new OrcType(OrcTypeKind.DECIMAL, decimalType.getPrecision(), decimalType.getScale()));
        }
        if (type instanceof ArrayType) {
            return createOrcArrayType(nextFieldTypeIndex, type.getTypeParameters().get(0));
        }
        if (type instanceof MapType) {
            return createOrcMapType(nextFieldTypeIndex, type.getTypeParameters().get(0), type.getTypeParameters().get(1));
        }
        if (type instanceof RowType) {
            List<String> fieldNames = new ArrayList<>();
            for (int i = 0; i < type.getTypeSignature().getParameters().size(); i++) {
                TypeSignatureParameter parameter = type.getTypeSignature().getParameters().get(i);
                fieldNames.add(parameter.getNamedTypeSignature().getName().orElse("field" + i));
            }
            List<Type> fieldTypes = type.getTypeParameters();

            return createOrcRowType(nextFieldTypeIndex, fieldNames, fieldTypes);
        }
        throw new PrestoException(NOT_SUPPORTED, format("Unsupported Hive type: %s", type));
    }

    private static List<OrcType> createOrcArrayType(int nextFieldTypeIndex, Type itemType)
    {
        nextFieldTypeIndex++;
        List<OrcType> itemTypes = toOrcType(nextFieldTypeIndex, itemType);

        List<OrcType> orcTypes = new ArrayList<>();
        orcTypes.add(new OrcType(OrcTypeKind.LIST, ImmutableList.of(new OrcColumnId(nextFieldTypeIndex)), ImmutableList.of("item")));
        orcTypes.addAll(itemTypes);
        return orcTypes;
    }

    private static List<OrcType> createOrcMapType(int nextFieldTypeIndex, Type keyType, Type valueType)
    {
        nextFieldTypeIndex++;
        List<OrcType> keyTypes = toOrcType(nextFieldTypeIndex, keyType);
        List<OrcType> valueTypes = toOrcType(nextFieldTypeIndex + keyTypes.size(), valueType);

        List<OrcType> orcTypes = new ArrayList<>();
        orcTypes.add(new OrcType(
                OrcTypeKind.MAP,
                ImmutableList.of(new OrcColumnId(nextFieldTypeIndex), new OrcColumnId(nextFieldTypeIndex + keyTypes.size())),
                ImmutableList.of("key", "value")));
        orcTypes.addAll(keyTypes);
        orcTypes.addAll(valueTypes);
        return orcTypes;
    }

    public static ColumnMetadata<OrcType> createRootOrcType(List<String> fieldNames, List<Type> fieldTypes)
    {
        return new ColumnMetadata<>(createOrcRowType(0, fieldNames, fieldTypes));
    }

    private static List<OrcType> createOrcRowType(int nextFieldTypeIndex, List<String> fieldNames, List<Type> fieldTypes)
    {
        nextFieldTypeIndex++;
        List<OrcColumnId> fieldTypeIndexes = new ArrayList<>();
        List<List<OrcType>> fieldTypesList = new ArrayList<>();
        for (Type fieldType : fieldTypes) {
            fieldTypeIndexes.add(new OrcColumnId(nextFieldTypeIndex));
            List<OrcType> fieldOrcTypes = toOrcType(nextFieldTypeIndex, fieldType);
            fieldTypesList.add(fieldOrcTypes);
            nextFieldTypeIndex += fieldOrcTypes.size();
        }

        ImmutableList.Builder<OrcType> orcTypes = ImmutableList.builder();
        orcTypes.add(new OrcType(
                OrcTypeKind.STRUCT,
                fieldTypeIndexes,
                fieldNames));
        fieldTypesList.forEach(orcTypes::addAll);

        return orcTypes.build();
    }
}
